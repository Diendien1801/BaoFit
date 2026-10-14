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
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
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

    private fun signIn(userId: String = "user-1", accessToken: String = "access-1") {
        tokenStore.save(AuthTokens(accessToken = accessToken, refreshToken = "refresh-1"))
        session.value = UserSession(userId, "an@visionfit.vn", "An")
    }

    @Test
    fun aRangeIsDownloadedOnceWithTheTokenAndGroupedByDay() = runTest {
        val meals = repository { json(HttpStatusCode.OK, "[$LUNCH_JSON, $BREAKFAST_JSON]") }
        signIn()

        val week = meals.observeRange(OCT_12, OCT_14).first()

        assertEquals(listOf(OCT_12, OCT_13, OCT_14), week.map(DayLog::date))
        assertTrue(week[0].meals.isEmpty() && week[1].meals.isEmpty(), "days without meals are empty, not missing")
        val today = week[2].meals.map { it as ConfirmedMeal }
        assertEquals(listOf("meal-lunch", "meal-breakfast"), today.map { it.id }, "newest first")
        assertEquals("Cơm tấm & Sườn nướng", today[0].title, "the app builds the title")
        assertEquals(325 + 504, today[0].kcal)
        assertEquals(MealPhoto("https://cdn.example/lunch.jpg"), today[0].photo)
        assertEquals(null, today[0].items[1].confidence)
        assertEquals(DiaryStatus.READY, meals.status.value)

        val request = requests.single()
        assertEquals(HttpMethod.Get, request.method)
        assertEquals("http://meal.test/api/v1/meals?from=2026-10-12&to=2026-10-14", request.url.toString())
        assertEquals("Bearer access-1", request.headers[HttpHeaders.Authorization])

        assertEquals(today, meals.observeDay(OCT_14).first().meals, "a cached day needs no new call")
        assertEquals(1, requests.size)
    }

    @Test
    fun aFailedDownloadEmitsNothingUntilARefreshSucceeds() = runTest {
        var online = false
        val meals = repository { if (online) json(HttpStatusCode.OK, "[$LUNCH_JSON]") else error("offline") }
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
    fun savingPostsTheMealAddsItToTheDayAndReplacesItsPendingEntry() = runTest {
        val meals = repository { request ->
            if (request.method == HttpMethod.Post) json(HttpStatusCode.Created, LUNCH_JSON) else json(HttpStatusCode.OK, "[]")
        }
        signIn()
        store.meals.value = listOf(
            PendingMeal("pending-1", MealType.LUNCH, LUNCH_AT, MealPhoto("https://cdn.example/lunch.jpg"), "job-7f3a", PendingStatus.READY_FOR_REVIEW),
        )
        assertIs<PendingMeal>(meals.observeDay(OCT_14).first().meals.single())

        val id = meals.saveMeal(
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
        assertEquals(listOf("meal-lunch"), meals.observeDay(OCT_14).first().meals.map { it.id }, "saved, and the pending entry is gone")
        val post = requests.last()
        assertEquals(HttpMethod.Post, post.method)
        assertEquals("Bearer access-1", post.headers[HttpHeaders.Authorization])
        assertEquals(
            """{"jobId":"job-7f3a","type":"LUNCH","loggedAt":"2026-10-14T12:05","photoUrl":"https://cdn.example/lunch.jpg",""" +
                """"isEdited":true,"items":[{"name":"Cơm tấm","grams":250,"per100g":{"kcal":130.0,"proteinG":2.7,"carbsG":28.0,"fatG":0.3},""" +
                """"confidence":94},{"name":" Sườn nướng ","grams":120,"per100g":{"kcal":420.0,"proteinG":24.0,"carbsG":4.0,"fatG":20.0},""" +
                """"confidence":null}]}""",
            post.bodyText,
        )
    }

    @Test
    fun aRejectedSaveThrowsSoReviewCanTellTheUser() = runTest {
        val meals = repository { json(HttpStatusCode.BadRequest, """{"status":400}""") }
        signIn()

        assertFailsWith<IllegalStateException> {
            meals.saveMeal(
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
    fun updatingReplacesTheMealInItsDay() = runTest {
        val edited = LUNCH_JSON.replace("\"type\":\"LUNCH\"", "\"type\":\"DINNER\"")
        val meals = repository { request ->
            if (request.method == HttpMethod.Put) json(HttpStatusCode.OK, edited) else json(HttpStatusCode.OK, "[$LUNCH_JSON]")
        }
        signIn()
        meals.observeDay(OCT_14).first()

        meals.updateMeal("meal-lunch", MealType.DINNER, listOf(FoodItem("i1", "Cơm tấm", 200, NutrientsPer100g(130.0, 2.7, 28.0, 0.3), 94)))

        val put = requests.last()
        assertEquals("http://meal.test/api/v1/meals/meal-lunch", put.url.toString())
        assertEquals(
            """{"type":"DINNER","items":[{"name":"Cơm tấm","grams":200,"per100g":{"kcal":130.0,"proteinG":2.7,"carbsG":28.0,"fatG":0.3},"confidence":94}]}""",
            put.bodyText,
        )
        assertEquals(MealType.DINNER, meals.observeMeal("meal-lunch").first()?.type)
        assertEquals(listOf(MealType.DINNER), meals.observeDay(OCT_14).first().meals.map { it.type })
    }

    @Test
    fun updatingAMealDeletedElsewhereThrowsAndForgetsIt() = runTest {
        val meals = repository { request ->
            if (request.method == HttpMethod.Put) json(HttpStatusCode.NotFound, "{}") else json(HttpStatusCode.OK, "[$LUNCH_JSON]")
        }
        signIn()
        meals.observeDay(OCT_14).first()

        assertFailsWith<IllegalStateException> {
            meals.updateMeal("meal-lunch", MealType.DINNER, listOf(FoodItem("i1", "Cơm tấm", 200, NutrientsPer100g.Unknown, null)))
        }
        assertEquals(null, meals.observeMeal("meal-lunch").first())
    }

    @Test
    fun deletingRemovesTheMealAndAMissingMealCountsAsDeleted() = runTest {
        var deleteAnswer = HttpStatusCode.NoContent
        val meals = repository { request ->
            if (request.method == HttpMethod.Delete) respond("", deleteAnswer) else json(HttpStatusCode.OK, "[$LUNCH_JSON, $BREAKFAST_JSON]")
        }
        signIn()
        meals.observeDay(OCT_14).first()

        meals.deleteMeal("meal-lunch")
        assertEquals("http://meal.test/api/v1/meals/meal-lunch", requests.last().url.toString())
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
            json(HttpStatusCode.OK, if (forFirstUser) "[$LUNCH_JSON]" else "[]")
        }
        signIn("user-1", "access-1")
        assertEquals(1, meals.observeDay(OCT_14).first().meals.size)

        signIn("user-2", "access-2")

        assertTrue(meals.observeDay(OCT_14).first().meals.isEmpty())
        assertEquals("Bearer access-2", requests.last().headers[HttpHeaders.Authorization])
    }

    @Test
    fun longRangesAreSplitIntoWindowsTheServerAccepts() = runTest {
        val meals = repository { json(HttpStatusCode.OK, "[]") }
        signIn()

        meals.observeRange(LocalDate(2026, 1, 1), LocalDate(2026, 6, 30)).first()

        // 181 days: the server rejects ranges of more than 92 days.
        assertEquals(
            listOf("from=2026-01-01&to=2026-04-02", "from=2026-04-03&to=2026-06-30"),
            requests.map { it.url.encodedQuery },
        )
    }

    private companion object {
        val OCT_12 = LocalDate(2026, 10, 12)
        val OCT_13 = LocalDate(2026, 10, 13)
        val OCT_14 = LocalDate(2026, 10, 14)
        val LUNCH_AT = LocalDateTime(2026, 10, 14, 12, 5)

        // Exactly what meal-service returns (Jackson writes the seconds).
        const val LUNCH_JSON = """
            {"id":"meal-lunch","jobId":"job-7f3a","type":"LUNCH","loggedAt":"2026-10-14T12:05:00",
             "photoUrl":"https://cdn.example/lunch.jpg","isEdited":true,"items":[
               {"id":"i1","name":"Cơm tấm","grams":250,"per100g":{"kcal":130,"proteinG":2.7,"carbsG":28,"fatG":0.3},"confidence":94},
               {"id":"i2","name":"Sườn nướng","grams":120,"per100g":{"kcal":420,"proteinG":24,"carbsG":4,"fatG":20},"confidence":null}]}"""

        const val BREAKFAST_JSON = """
            {"id":"meal-breakfast","jobId":null,"type":"BREAKFAST","loggedAt":"2026-10-14T07:30:00",
             "photoUrl":null,"isEdited":false,"items":[
               {"id":"i3","name":"Phở bò","grams":400,"per100g":{"kcal":110,"proteinG":7,"carbsG":14,"fatG":3},"confidence":90}]}"""
    }
}
