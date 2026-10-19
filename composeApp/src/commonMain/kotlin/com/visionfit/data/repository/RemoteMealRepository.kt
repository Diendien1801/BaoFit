package com.visionfit.data.repository

import com.visionfit.data.mock.InMemoryVisionFitStore
import com.visionfit.data.remote.TokenStore
import com.visionfit.data.remote.dto.DayDto
import com.visionfit.data.remote.dto.MealDto
import com.visionfit.data.remote.dto.SaveMealRequest
import com.visionfit.data.remote.dto.StreakDto
import com.visionfit.data.remote.dto.UpdateMealRequest
import com.visionfit.data.remote.dto.eatenMacros
import com.visionfit.data.remote.dto.toDomain
import com.visionfit.data.remote.dto.toSaveItem
import com.visionfit.domain.model.ConfirmedMeal
import com.visionfit.domain.model.DayLog
import com.visionfit.domain.model.DiaryStatus
import com.visionfit.domain.model.FoodItem
import com.visionfit.domain.model.Macros
import com.visionfit.domain.model.MealEntry
import com.visionfit.domain.model.MealPhoto
import com.visionfit.domain.model.MealType
import com.visionfit.domain.model.PendingMeal
import com.visionfit.domain.model.UserSession
import com.visionfit.domain.repository.MealRepository
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.daysUntil
import kotlinx.datetime.plus
import kotlin.coroutines.cancellation.CancellationException

/**
 * [MealRepository] backed by meal-service (`/api/v1/meals`).
 *
 * The server does the arithmetic: each day comes with its totals, each meal with its own, and the
 * streak has its own endpoint. This repository caches what the server said, per day and for the
 * signed-in user only, and asks again for the days a save, an edit or a delete changed; it never
 * adds numbers up itself. Meals still being analyzed come from [store], where the mock analysis
 * pipeline keeps them until analysis-service exists; they count in no total.
 */
internal class RemoteMealRepository(
    private val client: HttpClient,
    private val tokenStore: TokenStore,
    private val session: StateFlow<UserSession?>,
    private val store: InMemoryVisionFitStore,
) : MealRepository {

    /** A day as meal-service returned it. */
    private data class ServerDay(val meals: List<ConfirmedMeal>, val eatenKcal: Int, val eatenMacros: Macros)

    /** What the server said, for [ownerId] only: another user's sign-in starts over. */
    private data class Cache(
        val ownerId: String? = null,
        val days: Map<LocalDate, ServerDay> = emptyMap(),
        /** Streak per "today" the app asked about. */
        val streaks: Map<LocalDate, Int> = emptyMap(),
        /** Every day and streak a screen asked for, so [refresh] knows what to download again. */
        val requestedDays: Set<LocalDate> = emptySet(),
        val requestedStreaks: Set<LocalDate> = emptySet(),
    )

    private val cache = MutableStateFlow(Cache())
    private val loadLock = Mutex()

    private val _status = MutableStateFlow(DiaryStatus.LOADING)
    override val status: StateFlow<DiaryStatus> = _status.asStateFlow()

    override fun observeDay(date: LocalDate): Flow<DayLog> = observeRange(date, date).map { it.single() }

    override fun observeRange(from: LocalDate, to: LocalDate): Flow<List<DayLog>> {
        val dates = generateSequence(from) { it.plus(1, DateTimeUnit.DAY) }.takeWhile { it <= to }.toList()
        return combine(ownCache(), store.meals) { cached, local ->
            // Nothing rather than empty days: an empty day would read as "0 kcal eaten".
            if (cached == null || !cached.days.keys.containsAll(dates)) {
                null
            } else {
                dates.map { date -> dayLog(date, cached.days.getValue(date), local) }
            }
        }
            .filterNotNull()
            .distinctUntilChanged()
            .onStart { load(days = dates, streaks = emptyList(), force = false) }
    }

    override fun observeStreak(today: LocalDate): Flow<Int> =
        ownCache()
            .map { it?.streaks?.get(today) }
            .filterNotNull()
            .distinctUntilChanged()
            .onStart { load(days = emptyList(), streaks = listOf(today), force = false) }

    override fun observeMeal(id: String): Flow<ConfirmedMeal?> =
        ownCache()
            .map { cached -> cached?.days?.values?.flatMap { it.meals }?.firstOrNull { it.id == id } }
            .distinctUntilChanged()

    override suspend fun saveMeal(
        id: String,
        jobId: String?,
        type: MealType,
        loggedAt: LocalDateTime,
        photo: MealPhoto?,
        items: List<FoodItem>,
        isEdited: Boolean,
    ): String {
        require(items.isNotEmpty()) { "A meal needs at least one dish" }
        val response = client.post(PATH) {
            bearerAuth(accessToken())
            contentType(ContentType.Application.Json)
            setBody(SaveMealRequest(id, jobId, type, loggedAt, photo?.uri, isEdited, items.map { it.toSaveItem() }))
        }
        // 200: this meal (same id, or same job) was saved before; the server returns that first meal.
        check(response.status == HttpStatusCode.Created || response.status == HttpStatusCode.OK) {
            "Saving the meal failed with ${response.status}"
        }
        val meal = response.body<MealDto>()
        store.meals.update { meals -> meals.filterNot { it is PendingMeal && it.jobId == jobId } }
        reloadAfterChange(meal.loggedAt.date)
        return meal.id
    }

    override suspend fun updateMeal(id: String, type: MealType, items: List<FoodItem>) {
        require(items.isNotEmpty()) { "A meal needs at least one dish" }
        val date = dateOf(id)
        val response = client.put("$PATH/$id") {
            bearerAuth(accessToken())
            contentType(ContentType.Application.Json)
            setBody(UpdateMealRequest(type, items.map { it.toSaveItem() }))
        }
        // 404: deleted meanwhile (on another device). Reloading its day drops it, the detail screen then says so.
        if (response.status == HttpStatusCode.NotFound) reloadAfterChange(date)
        check(response.status == HttpStatusCode.OK) { "Updating the meal failed with ${response.status}" }
        reloadAfterChange(date)
    }

    override suspend fun deleteMeal(id: String) {
        val date = dateOf(id)
        val response = client.delete("$PATH/$id") { bearerAuth(accessToken()) }
        // 404: already deleted, which is what the user asked for.
        check(response.status == HttpStatusCode.NoContent || response.status == HttpStatusCode.NotFound) {
            "Deleting the meal failed with ${response.status}"
        }
        reloadAfterChange(date)
    }

    override suspend fun refresh() {
        val cached = cache.value
        load(cached.requestedDays.sorted(), cached.requestedStreaks.toList(), force = true)
    }

    /** The cache when it belongs to the signed-in user, `null` otherwise. */
    private fun ownCache(): Flow<Cache?> =
        combine(cache, session) { cached, user -> cached.takeIf { user != null && it.ownerId == user.userId } }

    private fun dateOf(mealId: String): LocalDate? =
        cache.value.days.entries.firstOrNull { (_, day) -> day.meals.any { it.id == mealId } }?.key

    /**
     * A change moves the day's totals and maybe the streak, both the server's to compute: ask again
     * for that day, if a screen holds it, and for every streak shown.
     */
    private suspend fun reloadAfterChange(date: LocalDate?) {
        val cached = cache.value
        load(listOfNotNull(date).filter { it in cached.days }, cached.streaks.keys.toList(), force = true)
    }

    /**
     * Downloads the [days] and [streaks] not cached yet (all of them when [force]), in windows
     * meal-service accepts. Serialized, so two screens opening together do not fetch twice.
     */
    private suspend fun load(days: List<LocalDate>, streaks: List<LocalDate>, force: Boolean) = loadLock.withLock {
        val user = session.value ?: return@withLock
        if (cache.value.ownerId != user.userId) {
            cache.value = Cache(ownerId = user.userId)
            _status.value = DiaryStatus.LOADING
        }
        cache.update { it.copy(requestedDays = it.requestedDays + days, requestedStreaks = it.requestedStreaks + streaks) }
        val missingDays = if (force) days else days.filterNot { it in cache.value.days }
        val missingStreaks = if (force) streaks else streaks.filterNot { it in cache.value.streaks }
        if (missingDays.isEmpty() && missingStreaks.isEmpty()) return@withLock

        _status.value = DiaryStatus.LOADING
        _status.value = try {
            windows(missingDays).forEach { window ->
                val fetched = fetchDays(window.first(), window.last())
                if (session.value?.userId != user.userId) return@withLock // signed out meanwhile
                cache.update { it.copy(days = it.days + fetched) }
            }
            missingStreaks.forEach { today ->
                val days = fetchStreak(today)
                if (session.value?.userId != user.userId) return@withLock
                cache.update { it.copy(streaks = it.streaks + (today to days)) }
            }
            DiaryStatus.READY
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            // Offline, an expired token (401), or a body the app cannot read.
            DiaryStatus.FAILED
        }
    }

    private suspend fun fetchDays(from: LocalDate, to: LocalDate): Map<LocalDate, ServerDay> {
        val response = client.get(PATH) {
            bearerAuth(accessToken())
            parameter("from", from.toString())
            parameter("to", to.toString())
        }
        check(response.status == HttpStatusCode.OK) { "Loading meals failed with ${response.status}" }
        return response.body<List<DayDto>>().associate { day ->
            day.date to ServerDay(day.meals.map { it.toDomain() }, day.kcal, day.eatenMacros)
        }
    }

    private suspend fun fetchStreak(today: LocalDate): Int {
        val response = client.get("$PATH/streak") {
            bearerAuth(accessToken())
            parameter("today", today.toString())
        }
        check(response.status == HttpStatusCode.OK) { "Loading the streak failed with ${response.status}" }
        return response.body<StreakDto>().days
    }

    private fun accessToken(): String = checkNotNull(tokenStore.current()?.accessToken) { "Not signed in" }

    /** Groups sorted [dates] so each group spans fewer than [MAX_RANGE_DAYS] days: one GET each. */
    private fun windows(dates: List<LocalDate>): List<List<LocalDate>> {
        val windows = mutableListOf<MutableList<LocalDate>>()
        for (date in dates.sorted()) {
            val current = windows.lastOrNull()
            if (current != null && current.first().daysUntil(date) < MAX_RANGE_DAYS) current += date else windows += mutableListOf(date)
        }
        return windows
    }

    /** The server's day plus the meals still being analyzed on this device, newest first. */
    private fun dayLog(date: LocalDate, server: ServerDay, local: List<MealEntry>) = DayLog(
        date = date,
        meals = (server.meals + local.filter { it is PendingMeal && it.loggedAt.date == date }).sortedByDescending { it.loggedAt },
        eatenKcal = server.eatenKcal,
        eatenMacros = server.eatenMacros,
    )

    private companion object {
        const val PATH = "api/v1/meals"

        /** meal-service rejects ranges longer than 92 days. */
        const val MAX_RANGE_DAYS = 92
    }
}
