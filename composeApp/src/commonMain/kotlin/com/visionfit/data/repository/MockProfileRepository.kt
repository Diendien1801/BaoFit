package com.visionfit.data.repository

import com.visionfit.data.mock.InMemoryVisionFitStore
import com.visionfit.domain.model.BodyProfile
import com.visionfit.domain.repository.ProfileRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

internal class MockProfileRepository(
    private val store: InMemoryVisionFitStore,
    private val latencyMillis: Long = 400,
) : ProfileRepository {

    override val profile: StateFlow<BodyProfile> = store.profile.asStateFlow()

    override suspend fun saveProfile(profile: BodyProfile) {
        delay(latencyMillis)
        store.profile.value = profile
    }
}
