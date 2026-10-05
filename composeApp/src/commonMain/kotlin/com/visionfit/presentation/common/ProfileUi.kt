package com.visionfit.presentation.common

import com.visionfit.core.format.VnFormat
import com.visionfit.domain.model.FitnessGoal
import com.visionfit.domain.model.Sex
import com.visionfit.presentation.designsystem.icons.IconSpec
import com.visionfit.presentation.designsystem.icons.VfIcons

val Sex.label: String
    get() = when (this) {
        Sex.MALE -> "Nam"
        Sex.FEMALE -> "Nữ"
    }

val FitnessGoal.title: String
    get() = when (this) {
        FitnessGoal.FAT_LOSS -> "Giảm mỡ"
        FitnessGoal.MUSCLE_GAIN -> "Tăng cơ"
    }

val FitnessGoal.description: String
    get() = "${VnFormat.signed(dailyAdjustmentKcal)} kcal / ngày"

val FitnessGoal.icon: IconSpec
    get() = when (this) {
        FitnessGoal.FAT_LOSS -> VfIcons.TrendDown
        FitnessGoal.MUSCLE_GAIN -> VfIcons.TrendUp
    }
