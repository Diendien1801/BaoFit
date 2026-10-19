package com.visionfit.data

import com.visionfit.core.time.FixedTimeProvider
import com.visionfit.data.mock.InMemoryVisionFitStore
import com.visionfit.data.remote.AuthTokens
import com.visionfit.data.remote.TokenStore
import com.visionfit.data.remote.createHttpClient
import com.visionfit.data.repository.RemoteMealRepository
import com.visionfit.domain.model.ConfirmedMeal
import com.visionfit.domain.model.DayLog
import com.visionfit.domain.model.DiaryStatus
import com.visionfit.domain.model.FoodItem
import com.visionfit.domain.model.Macros
import com.visionfit.domain.model.MealPhoto
import com.visionfit.domain.model.MealType
import com.visionfit.domain.model.NutrientsPer100g
import com.visionfit.domain.model.PendingMeal
import com.visionfit.domain.model.PendingStatus
import com.visionfit.domain.model.UserSession
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockEngineConfig
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.OutgoingContent
import io.ktor.http.headersOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.plus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertTrue

class RemoteMealRepositoryTest {

    private val tokenStore = TokenStore()
    private val session = MutableStateFlow<UserSession?>(null)
    private val store = InMemoryVisionFitStore(FixedTimeProvider(LocalDateTime(2026, 10, 14, 19, 0)), withDemoData = false)
    private val requests = mutableListOf<HttpRequestData>()

    /** The engine runs on the test scheduler, so `runCurrent()` also completes HTTP calls. */
    private fun TestScope.repository(
        handler: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData,
    ): RemoteMealRepository {
        val engine = MockEngine(
            MockEngineConfig().apply {
                dispatcher = StandardTestDispatcher(testScheduler)
                addHandler { request ->
                    requests += request
                    handler(request)
                }
            },
        )
        return RemoteMealRepository(createHttpClient("http://meal.test/", engine), tokenStore, session, store)
    }

    private fun MockRequestHandleScope.json(status: HttpStatusCode, body: String) =
        respond(body, status, headersOf(HttpHeaders.ContentType, "application/json"))

    private val HttpRequestData.bodyText: String
        get() = (body as OutgoingContent.ByteArrayContent).bytes().decodeToString()

    private val HttpRequestData.isGetDays: Boolean
        get() = method == HttpMethod.Get && url.encodedPath == "/api/v1/meals"

    private fun signIn(userId: String = "user-1", accessToken: String = "access-1") {
        tokenStore.save(AuthTokens(accessToken = accessToken, refreshToken = "refresh-1"))
        session.value = UserSession(userId, "an@visionfit.vn", "An")
    }

    @Test
    fun daysComeWithTheServersTotalsWhichTheAppShowsAsTheyAre() = runTest {
        val meals = repository {
            json(HttpStatusCode.OK, "[${day("2026-10-12")}, ${day("2026-10-13")}, ${day("2026-10-14", LUNCH_JSON, BREAKFAST_JSON)}]")
        }
        signIn()

        val week = meals.observeRange(OCT_12, OCT_14).first()

        assertEquals(listOf(OCT_12, OCT_13, OCT_14), week.map(DayLog::date))
        assertEquals(0, week[0].eatenKcal)
        val today = week[2]
        // The server's numbers, not sums the app made: the dishes alone would add up to 829 kcal.
        assertEquals(1270, today.eatenKcal)
        assertEquals(Macros(50.0, 130.0, 30.0), today.eatenMacros)
        val lunch = today.meals[0] as ConfirmedMeal
        assertEquals(listOf("meal-lunch", "meal-breakfast"), today.meals.map { it.id }, "newest first")
        assertEquals(830, lunch.kcal)
        assertEquals(Macros(35.5, 74.8, 24.8), lunch.macros)
        assertEquals("Cơm tấm & Sườn nướng", lunch.title, "the app builds the title")
        assertEquals(MealPhoto("https://cdn.example/lunch.jpg"), lunch.photo)
        assertEquals(DiaryStatus.READY, meals.status.value)

        val request = requests.single()
        assertEquals("http://meal.test/api/v1/meals?from=2026-10-12&to=2026-10-14", request.url.toString())
        assertEquals("Bearer access-1", request.headers[HttpHeaders.Authorization])

        assertEquals(today, meals.observeDay(OCT_14).first(), "a cached day needs no new call")
        assertEquals(1, requests.size)
    }

    @Test
    fun theStreakComesFromTheServer() = runTest {
        val meals = repository { json(HttpStatusCode.OK, """{"days":4}""") }
        signIn()

        assertEquals(4, meals.observeStreak(OCT_14).first())
        assertEquals("http://meal.test/api/v1/meals/streak?today=2026-10-14", requests.single().url.toString())
    }

    @Test
    fun aFailedDownloadEmitsNothingUntilARefreshSucceeds() = runTest {
        var online = false
        val meals = repository { if (online) json(HttpStatusCode.OK, "[${day("2026-10-14", LUNCH_JSON)}]") else error("offline") }
        signIn()
        val emitted = mutableListOf<DayLog>()
        meals.observeDay(OCT_14).onEach { emitted += it }.launchIn(backgroundScope)
        runCurrent()

        assertEquals(DiaryStatus.FAILED, meals.status.value)
        assertTrue(emitted.isEmpty(), "an unknown day must not look like a day with 0 kcal")

        online = true
        meals.refresh()
        runCurrent()

        assertEquals(DiaryStatus.READY, meals.status.value)
        assertEquals(listOf("meal-lunch"), emitted.single().meals.map { it.id })
    }

    @Test
    fun savingPostsTheMealThenAsksTheServerForTheDayAndTheStreakAgain() = runTest {
        var saved = false
        val meals = repository { request ->
            when {
                request.method == HttpMethod.Post -> json(HttpStatusCode.Created, LUNCH_JSON).also { saved = true }
                request.isGetDays -> json(HttpStatusCode.OK, if (saved) "[${day("2026-10-14", LUNCH_JSON)}]" else "[${day("2026-10-14")}]")
                else -> json(HttpStatusCode.OK, if (saved) """{"days":1}""" else """{"days":0}""")
            }
        }
        signIn()
        store.meals.value = listOf(
            PendingMeal("pending-1", MealType.LUNCH, LUNCH_AT, MealPhoto("https://cdn.example/lunch.jpg"), "job-7f3a", PendingStatus.READY_FOR_REVIEW),
        )
        val before = meals.observeDay(OCT_14).first()
        assertIs<PendingMeal>(before.meals.single())
        assertEquals(0, before.eatenKcal, "a meal being analyzed counts in no total")
        assertEquals(0, meals.observeStreak(OCT_14).first())

        val id = meals.saveMeal(
            id = "meal-lunch",
            jobId = "job-7f3a",
            type = MealType.LUNCH,
            loggedAt = LUNCH_AT,
            photo = MealPhoto("https://cdn.example/lunch.jpg"),
            items = listOf(
                FoodItem("ai-1", "Cơm tấm", 250, NutrientsPer100g(130.0, 2.7, 28.0, 0.3), confidence = 94),
                FoodItem("manual-1", " Sườn nướng ", 120, NutrientsPer100g(420.0, 24.0, 4.0, 20.0), confidence = null),
            ),
            isEdited = true,
        )

        assertEquals("meal-lunch", id)
        val after = meals.observeDay(OCT_14).first()
        assertEquals(listOf("meal-lunch"), after.meals.map { it.id }, "saved, and the pending entry is gone")
        assertEquals(1270, after.eatenKcal, "the day's total as the server recomputed it")
        assertEquals(1, meals.observeStreak(OCT_14).first())
        val post = requests.single { it.method == HttpMethod.Post }
        assertEquals("Bearer access-1", post.headers[HttpHeaders.Authorization])
        assertEquals(
            """{"id":"meal-lunch","jobId":"job-7f3a","type":"LUNCH","loggedAt":"2026-10-14T12:05","photoUrl":"https://cdn.example/lunch.jpg",""" +
                """"isEdited":true,"items":[{"name":"Cơm tấm","grams":250,"per100g":{"kcal":130.0,"proteinG":2.7,"carbsG":28.0,"fatG":0.3},""" +
                """"confidence":94},{"name":" Sườn nướng ","grams":120,"per100g":{"kcal":420.0,"proteinG":24.0,"carbsG":4.0,"fatG":20.0},""" +
                """"confidence":null}]}""",
            post.bodyText,
        )
        assertEquals(2, requests.count { it.isGetDays }, "the day was downloaded again after the save")
    }

    @Test
    fun aRejectedSaveThrowsSoReviewCanTellTheUser() = runTest {
        val meals = repository { json(HttpStatusCode.BadRequest, """{"status":400}""") }
        signIn()

        assertFailsWith<IllegalStateException> {
            meals.saveMeal(
                id = "meal-x",
                jobId = null,
                type = MealType.SNACK,
                loggedAt = LUNCH_AT,
                photo = null,
                items = listOf(FoodItem("x", "Táo", 9999, NutrientsPer100g(52.0, 0.3, 14.0, 0.2), confidence = null)),
                isEdited = false,
            )
        }
    }

    @Test
    fun updatingSendsTheChangesThenReloadsTheDay() = runTest {
        var updated = false
        val dinner = LUNCH_JSON.replace("\"type\":\"LUNCH\"", "\"type\":\"DINNER\"")
        val meals = repository { request ->
            when (request.method) {
                HttpMethod.Put -> json(HttpStatusCode.OK, dinner).also { updated = true }
                else -> json(HttpStatusCode.OK, "[${day("2026-10-14", if (updated) dinner else LUNCH_JSON)}]")
            }
        }
        signIn()
        meals.observeDay(OCT_14).first()

        meals.updateMeal("meal-lunch", MealType.DINNER, listOf(FoodItem("i1", "Cơm tấm", 200, NutrientsPer100g(130.0, 2.7, 28.0, 0.3), 94)))

        val put = requests.single { it.method == HttpMethod.Put }
        assertEquals("http://meal.test/api/v1/meals/meal-lunch", put.url.toString())
        assertEquals(
            """{"type":"DINNER","items":[{"name":"Cơm tấm","grams":200,"per100g":{"kcal":130.0,"proteinG":2.7,"carbsG":28.0,"fatG":0.3},"confidence":94}]}""",
            put.bodyText,
        )
        assertEquals(MealType.DINNER, meals.observeMeal("meal-lunch").first()?.type)
    }

    @Test
    fun updatingAMealDeletedElsewhereThrowsAndTheReloadDropsIt() = runTest {
        var deletedElsewhere = false
        val meals = repository { request ->
            when (request.method) {
                HttpMethod.Put -> json(HttpStatusCode.NotFound, "{}").also { deletedElsewhere = true }
                else -> json(HttpStatusCode.OK, "[${if (deletedElsewhere) day("2026-10-14") else day("2026-10-14", LUNCH_JSON)}]")
            }
        }
        signIn()
        meals.observeDay(OCT_14).first()

        assertFailsWith<IllegalStateException> {
            meals.updateMeal("meal-lunch", MealType.DINNER, listOf(FoodItem("i1", "Cơm tấm", 200, NutrientsPer100g.Unknown, null)))
        }
        assertEquals(null, meals.observeMeal("meal-lunch").first())
    }

    @Test
    fun deletingReloadsTheDayAndAMissingMealCountsAsDeleted() = runTest {
        var deleteAnswer = HttpStatusCode.NoContent
        val left = mutableListOf(LUNCH_JSON, BREAKFAST_JSON)
        val meals = repository { request ->
            if (request.method == HttpMethod.Delete) {
                left.removeAll { it.contains(request.url.encodedPath.substringAfterLast('/')) }
                respond("", deleteAnswer)
            } else {
                json(HttpStatusCode.OK, "[${day("2026-10-14", *left.toTypedArray())}]")
            }
        }
        signIn()
        meals.observeDay(OCT_14).first()

        meals.deleteMeal("meal-lunch")
        assertEquals("http://meal.test/api/v1/meals/meal-lunch", requests.last { it.method == HttpMethod.Delete }.url.toString())
        assertEquals(listOf("meal-breakfast"), meals.observeDay(OCT_14).first().meals.map { it.id })

        deleteAnswer = HttpStatusCode.NotFound
        meals.deleteMeal("meal-breakfast")
        assertTrue(meals.observeDay(OCT_14).first().meals.isEmpty())

        deleteAnswer = HttpStatusCode.InternalServerError
        assertFailsWith<IllegalStateException> { meals.deleteMeal("meal-x") }
    }

    @Test
    fun anotherUserNeverSeesThePreviousUsersDiary() = runTest {
        val meals = repository { request ->
            val forFirstUser = request.headers[HttpHeaders.Authorization] == "Bearer access-1"
            json(HttpStatusCode.OK, "[${if (forFirstUser) day("2026-10-14", LUNCH_JSON) else day("2026-10-14")}]")
        }
        signIn("user-1", "access-1")
        assertEquals(1, meals.observeDay(OCT_14).first().meals.size)

        signIn("user-2", "access-2")

        assertTrue(meals.observeDay(OCT_14).first().meals.isEmpty())
        assertEquals("Bearer access-2", requests.last().headers[HttpHeaders.Authorization])
    }

    @Test
    fun longRangesAreSplitIntoWindowsTheServerAccepts() = runTest {
        val meals = repository { request ->
            val from = LocalDate.parse(request.url.parameters["from"]!!)
            val to = LocalDate.parse(request.url.parameters["to"]!!)
            val days = generateSequence(from) { it.plus(1, DateTimeUnit.DAY) }.takeWhile { it <= to }
            json(HttpStatusCode.OK, days.joinToString(prefix = "[", postfix = "]") { day(it.toString()) })
        }
        signIn()

        meals.observeRange(LocalDate(2026, 1, 1), LocalDate(2026, 6, 30)).first()

        // 181 days: the server rejects ranges of more than 92 days.
        assertEquals(
            listOf("from=2026-01-01&to=2026-04-02", "from=2026-04-03&to=2026-06-30"),
            requests.map { it.url.encodedQuery },
        )
    }

    /** A day as meal-service returns it. Totals are made up on purpose: the app must not recompute them. */
    private fun day(date: String, vararg meals: String): String {
        val kcal = if (meals.isEmpty()) 0 else 1270
        val (protein, carbs, fat) = if (meals.isEmpty()) Triple(0, 0, 0) else Triple(50, 130, 30)
        return """{"date":"$date","kcal":$kcal,"proteinG":$protein,"carbsG":$carbs,"fatG":$fat,"mealCount":${meals.size},""" +
            """"meals":[${meals.joinToString(",")}]}"""
    }

    private companion object {
        val OCT_12 = LocalDate(2026, 10, 12)
        val OCT_13 = LocalDate(2026, 10, 13)
        val OCT_14 = LocalDate(2026, 10, 14)
        val LUNCH_AT = LocalDateTime(2026, 10, 14, 12, 5)

        // Shaped like meal-service's answer (Jackson writes the seconds).
        const val LUNCH_JSON = """
            {"id":"meal-lunch","jobId":"job-7f3a","type":"LUNCH","loggedAt":"2026-10-14T12:05:00",
             "photoUrl":"https://cdn.example/lunch.jpg","isEdited":true,
             "kcal":830,"proteinG":35.5,"carbsG":74.8,"fatG":24.8,"items":[
               {"id":"i1","name":"Cơm tấm","grams":250,"per100g":{"kcal":130,"proteinG":2.7,"carbsG":28,"fatG":0.3},"confidence":94},
               {"id":"i2","name":"Sườn nướng","grams":120,"per100g":{"kcal":420,"proteinG":24,"carbsG":4,"fatG":20},"confidence":null}]}"""

        const val BREAKFAST_JSON = """
            {"id":"meal-breakfast","jobId":null,"type":"BREAKFAST","loggedAt":"2026-10-14T07:30:00",
             "photoUrl":null,"isEdited":false,"kcal":440,"proteinG":28,"carbsG":56,"fatG":12,"items":[
               {"id":"i3","name":"Phở bò","grams":400,"per100g":{"kcal":110,"proteinG":7,"carbsG":14,"fatG":3},"confidence":90}]}"""
    }
}
