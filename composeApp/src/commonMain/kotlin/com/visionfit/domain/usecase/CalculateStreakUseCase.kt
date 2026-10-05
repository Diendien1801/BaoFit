package com.visionfit.domain.usecase

import com.visionfit.domain.model.DayLog
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus

/**
 * Number of consecutive completed days with at least one confirmed meal, counted backwards
 * from yesterday. Today is still in progress, so it never breaks (or extends) the streak.
 */
class CalculateStreakUseCase {

    operator fun invoke(days: List<DayLog>, today: LocalDate): Int {
        val loggedDates = days.filter { it.confirmedMeals.isNotEmpty() }.map { it.date }.toSet()
        var streak = 0
        var date = today.minus(1, DateTimeUnit.DAY)
        while (date in loggedDates) {
            streak++
            date = date.minus(1, DateTimeUnit.DAY)
        }
        return streak
    }
}
