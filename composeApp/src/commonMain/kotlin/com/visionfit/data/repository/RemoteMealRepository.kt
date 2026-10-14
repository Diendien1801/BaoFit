package com.visionfit.data.repository

import com.visionfit.data.mock.InMemoryVisionFitStore
import com.visionfit.data.remote.TokenStore
import com.visionfit.data.remote.dto.MealDto
import com.visionfit.data.remote.dto.SaveMealRequest
import com.visionfit.data.remote.dto.UpdateMealRequest
import com.visionfit.data.remote.dto.toDomain
import com.visionfit.data.remote.dto.toSaveItem
import com.visionfit.domain.model.ConfirmedMeal
import com.visionfit.domain.model.DayLog
import com.visionfit.domain.model.DiaryStatus
import com.visionfit.domain.model.FoodItem
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
 * Confirmed meals come from the server and are cached per day for the signed-in user, so the
 * dashboard's 61 days also serve History and Review without new calls. A saved meal is added to
 * the cache directly. Meals still being analyzed come from [store], where the mock analysis
 * pipeline keeps them until analysis-service exists.
 */
internal class RemoteMealRepository(
    private val client: HttpClient,
    private val tokenStore: TokenStore,
    private val session: StateFlow<UserSession?>,
    private val store: InMemoryVisionFitStore,
) : MealRepository {

    /** The server's meals of [loaded] days, for [ownerId] only: another user's sign-in starts over. */
    private data class Cache(
        val ownerId: String? = null,
        val meals: Map<LocalDate, List<ConfirmedMeal>> = emptyMap(),
        val loaded: Set<LocalDate> = emptySet(),
        /** Every day a screen asked for, so [refresh] knows what to download again. */
        val requested: Set<LocalDate> = emptySet(),
    )

    private val cache = MutableStateFlow(Cache())
    private val loadLock = Mutex()

    private val _status = MutableStateFlow(DiaryStatus.LOADING)
    override val status: StateFlow<DiaryStatus> = _status.asStateFlow()

    override fun observeDay(date: LocalDate): Flow<DayLog> = observeRange(date, date).map { it.single() }

    override fun observeRange(from: LocalDate, to: LocalDate): Flow<List<DayLog>> {
        val dates = generateSequence(from) { it.plus(1, DateTimeUnit.DAY) }.takeWhile { it <= to }.toList()
        return combine(cache, store.meals, session) { cached, local, user ->
            // Nothing rather than empty days: an empty day would read as "0 kcal eaten".
            if (user == null || cached.ownerId != user.userId || !cached.loaded.containsAll(dates)) {
                null
            } else {
                dates.map { date -> dayLog(date, cached.meals[date].orEmpty(), local) }
            }
        }
            .filterNotNull()
            .distinctUntilChanged()
            .onStart { load(dates, force = false) }
    }

    override suspend fun saveMeal(
        jobId: String?,
        type: MealType,
        loggedAt: LocalDateTime,
        photo: MealPhoto?,
        items: List<FoodItem>,
        isEdited: Boolean,
    ): String {
        require(items.isNotEmpty()) { "A meal needs at least one dish" }
        val token = checkNotNull(tokenStore.current()?.accessToken) { "Not signed in" }
        val response = client.post(PATH) {
            bearerAuth(token)
            contentType(ContentType.Application.Json)
            setBody(SaveMealRequest(jobId, type, loggedAt, photo?.uri, isEdited, items.map { it.toSaveItem() }))
        }
        // 200: this job was saved before (a retry or a double tap), the server returns that meal.
        check(response.status == HttpStatusCode.Created || response.status == HttpStatusCode.OK) {
            "Saving the meal failed with ${response.status}"
        }
        val meal = response.body<MealDto>().toDomain()
        cache.update { it.withMeal(meal) }
        store.meals.update { meals -> meals.filterNot { it is PendingMeal && it.jobId == jobId } }
        return meal.id
    }

    override fun observeMeal(id: String): Flow<ConfirmedMeal?> =
        combine(cache, session) { cached, user ->
            if (user == null || cached.ownerId != user.userId) null else cached.meals.values.flatten().firstOrNull { it.id == id }
        }.distinctUntilChanged()

    override suspend fun updateMeal(id: String, type: MealType, items: List<FoodItem>) {
        require(items.isNotEmpty()) { "A meal needs at least one dish" }
        val token = checkNotNull(tokenStore.current()?.accessToken) { "Not signed in" }
        val response = client.put("$PATH/$id") {
            bearerAuth(token)
            contentType(ContentType.Application.Json)
            setBody(UpdateMealRequest(type, items.map { it.toSaveItem() }))
        }
        if (response.status == HttpStatusCode.NotFound) {
            // Deleted meanwhile (on another device): drop it here too, the detail screen then says so.
            cache.update { it.withoutMeal(id) }
        }
        check(response.status == HttpStatusCode.OK) { "Updating the meal failed with ${response.status}" }
        val meal = response.body<MealDto>().toDomain()
        cache.update { it.withMeal(meal) }
    }

    override suspend fun deleteMeal(id: String) {
        val token = checkNotNull(tokenStore.current()?.accessToken) { "Not signed in" }
        val response = client.delete("$PATH/$id") { bearerAuth(token) }
        // 404: already deleted, which is what the user asked for.
        check(response.status == HttpStatusCode.NoContent || response.status == HttpStatusCode.NotFound) {
            "Deleting the meal failed with ${response.status}"
        }
        cache.update { it.withoutMeal(id) }
    }

    /** Puts [meal] in its day, replacing its old version, if that day is loaded for the signed-in user. */
    private fun Cache.withMeal(meal: ConfirmedMeal): Cache {
        val date = meal.loggedAt.date
        if (ownerId != session.value?.userId || date !in loaded) return this
        val sameDay = meals[date].orEmpty().filterNot { it.id == meal.id } + meal
        return copy(meals = meals + (date to sameDay))
    }

    private fun Cache.withoutMeal(id: String): Cache =
        copy(meals = meals.mapValues { (_, dayMeals) -> dayMeals.filterNot { it.id == id } })

    override suspend fun refresh() {
        load(cache.value.requested.sorted(), force = true)
    }

    /**
     * Downloads the [dates] not cached yet (all of them when [force]), in windows meal-service
     * accepts. Serialized, so two screens opening together do not fetch the same days twice.
     */
    private suspend fun load(dates: List<LocalDate>, force: Boolean) = loadLock.withLock {
        val user = session.value ?: return@withLock
        if (cache.value.ownerId != user.userId) {
            cache.value = Cache(ownerId = user.userId)
            _status.value = DiaryStatus.LOADING
        }
        cache.update { it.copy(requested = it.requested + dates) }
        val missing = if (force) dates else dates.filterNot { it in cache.value.loaded }
        if (missing.isEmpty()) return@withLock

        _status.value = DiaryStatus.LOADING
        _status.value = try {
            windows(missing).forEach { window ->
                val meals = fetch(window.first(), window.last())
                if (session.value?.userId != user.userId) return@withLock // signed out meanwhile
                val byDate = meals.groupBy { it.loggedAt.date }
                cache.update { cached ->
                    cached.copy(
                        meals = cached.meals + window.associateWith { byDate[it].orEmpty() },
                        loaded = cached.loaded + window,
                    )
                }
            }
            DiaryStatus.READY
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            // Offline, an expired token (401), or a body the app cannot read.
            DiaryStatus.FAILED
        }
    }

    private suspend fun fetch(from: LocalDate, to: LocalDate): List<ConfirmedMeal> {
        val token = checkNotNull(tokenStore.current()?.accessToken) { "Signed in without an access token" }
        val response = client.get(PATH) {
            bearerAuth(token)
            parameter("from", from.toString())
            parameter("to", to.toString())
        }
        check(response.status == HttpStatusCode.OK) { "Loading meals failed with ${response.status}" }
        return response.body<List<MealDto>>().map { it.toDomain() }
    }

    /** Groups sorted [dates] so each group spans fewer than [MAX_RANGE_DAYS] days: one GET each. */
    private fun windows(dates: List<LocalDate>): List<List<LocalDate>> {
        val windows = mutableListOf<MutableList<LocalDate>>()
        for (date in dates.sorted()) {
            val current = windows.lastOrNull()
            if (current != null && current.first().daysUntil(date) < MAX_RANGE_DAYS) current += date else windows += mutableListOf(date)
        }
        return windows
    }

    /** Newest first, like the diary timeline. */
    private fun dayLog(date: LocalDate, confirmed: List<ConfirmedMeal>, local: List<MealEntry>) = DayLog(
        date = date,
        meals = (confirmed + local.filter { it is PendingMeal && it.loggedAt.date == date })
            .sortedByDescending { it.loggedAt },
    )

    private companion object {
        const val PATH = "api/v1/meals"

        /** meal-service rejects ranges longer than 92 days. */
        const val MAX_RANGE_DAYS = 92
    }
}
