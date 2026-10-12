package com.visionfit.data.repository

import com.visionfit.data.mock.InMemoryVisionFitStore
import com.visionfit.domain.model.BodyProfile
import com.visionfit.domain.model.ProfileStatus
import com.visionfit.domain.repository.ProfileRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** The store always holds a profile, so it is ready from the start. */
internal class MockProfileRepository(
    private val store: InMemoryVisionFitStore,
    private val latencyMillis: Long = 400,
) : ProfileRepository {

    override val profile: StateFlow<BodyProfile> = store.profile.asStateFlow()

    override val status: StateFlow<ProfileStatus> = MutableStateFlow(ProfileStatus.READY)

    override suspend fun saveProfile(profile: BodyProfile): Boolean {
        delay(latencyMillis)
        store.profile.value = profile
        return true
    }

    override suspend fun refresh() = Unit
}
