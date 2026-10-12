package com.visionfit.data.remote.dto

import com.visionfit.domain.model.ActivityLevel
import com.visionfit.domain.model.BodyProfile
import com.visionfit.domain.model.FitnessGoal
import com.visionfit.domain.model.Sex
import kotlinx.serialization.Serializable

/**
 * Body of profile-service's `/api/v1/profile/me`, in both directions. The domain enums travel by
 * name ("FAT_LOSS"), which is exactly what the server expects, so they need no mapping.
 */
@Serializable
internal data class ProfileDto(
    val sex: Sex,
    val ageYears: Int,
    val heightCm: Int,
    val weightKg: Int,
    val activityLevel: ActivityLevel,
    val goal: FitnessGoal,
)

internal fun ProfileDto.toDomain() = BodyProfile(sex, ageYears, heightCm, weightKg, activityLevel, goal)

internal fun BodyProfile.toDto() = ProfileDto(sex, ageYears, heightCm, weightKg, activityLevel, goal)
