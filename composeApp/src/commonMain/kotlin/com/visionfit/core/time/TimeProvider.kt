package com.visionfit.core.time

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/** Injectable clock so that "today", greetings and meal types are testable. */
interface TimeProvider {
    fun now(): LocalDateTime

    fun today(): LocalDate = now().date
}

class SystemTimeProvider(
    private val timeZone: TimeZone = TimeZone.currentSystemDefault(),
) : TimeProvider {
    @OptIn(ExperimentalTime::class)
    override fun now(): LocalDateTime = Clock.System.now().toLocalDateTime(timeZone)
}

class FixedTimeProvider(private val fixed: LocalDateTime) : TimeProvider {
    override fun now(): LocalDateTime = fixed
}
