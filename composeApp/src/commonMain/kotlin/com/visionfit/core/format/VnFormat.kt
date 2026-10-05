package com.visionfit.core.format

import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.number
import kotlin.math.abs
import kotlin.math.roundToLong
import kotlin.time.Duration

/**
 * Vietnamese number and date formatting, matching the design:
 * "." groups thousands, "," separates decimals and negatives use a real minus sign.
 */
object VnFormat {

    private const val MINUS = "−"

    /** 2034 → "2.034", -500 → "−500". */
    fun thousands(value: Int): String {
        val grouped = abs(value.toLong()).toString()
            .reversed()
            .chunked(3)
            .joinToString(".")
            .reversed()
        return if (value < 0) "$MINUS$grouped" else grouped
    }

    /** 300 → "+300", -500 → "−500", 0 → "0". */
    fun signed(value: Int): String = if (value > 0) "+${thousands(value)}" else thousands(value)

    /** 54.1 → "54,1", 46.0 → "46", 0.25 → "0,3". */
    fun oneDecimal(value: Double): String = decimal(value, fractionDigits = 1)

    /** 1.55 → "1,55", 1.2 → "1,2". */
    fun factor(value: Double): String = decimal(value, fractionDigits = 2)

    /** 1_200_000 → "1,2 MB". */
    fun megabytes(bytes: Long): String = "${oneDecimal(bytes / 1_000_000.0)} MB"

    /** 4.2 s → "4,2s". */
    fun seconds(duration: Duration): String = "${oneDecimal(duration.inWholeMilliseconds / 1000.0)}s"

    /** 18:40. */
    fun time(time: LocalTime): String = "${time.hour.pad2()}:${time.minute.pad2()}"

    /** 05/10. */
    fun dayMonth(date: LocalDate): String = "${date.day.pad2()}/${date.month.number.pad2()}"

    /** T2 … T7, CN. */
    fun weekdayShort(day: DayOfWeek): String = when (day) {
        DayOfWeek.MONDAY -> "T2"
        DayOfWeek.TUESDAY -> "T3"
        DayOfWeek.WEDNESDAY -> "T4"
        DayOfWeek.THURSDAY -> "T5"
        DayOfWeek.FRIDAY -> "T6"
        DayOfWeek.SATURDAY -> "T7"
        DayOfWeek.SUNDAY -> "CN"
    }

    /** Thứ Hai … Chủ Nhật. */
    fun weekdayLong(day: DayOfWeek): String = when (day) {
        DayOfWeek.MONDAY -> "Thứ Hai"
        DayOfWeek.TUESDAY -> "Thứ Ba"
        DayOfWeek.WEDNESDAY -> "Thứ Tư"
        DayOfWeek.THURSDAY -> "Thứ Năm"
        DayOfWeek.FRIDAY -> "Thứ Sáu"
        DayOfWeek.SATURDAY -> "Thứ Bảy"
        DayOfWeek.SUNDAY -> "Chủ Nhật"
    }

    /** "Thứ Hai, 05/10". */
    fun longDate(date: LocalDate): String = "${weekdayLong(date.dayOfWeek)}, ${dayMonth(date)}"

    /** "CN 04/10". */
    fun shortDate(date: LocalDate): String = "${weekdayShort(date.dayOfWeek)} ${dayMonth(date)}"

    private fun decimal(value: Double, fractionDigits: Int): String {
        var scale = 1L
        repeat(fractionDigits) { scale *= 10 }
        val scaled = (abs(value) * scale).roundToLong()
        val whole = scaled / scale
        val fraction = (scaled % scale).toString().padStart(fractionDigits, '0').trimEnd('0')
        val sign = if (value < 0 && scaled != 0L) MINUS else ""
        return if (fraction.isEmpty()) "$sign$whole" else "$sign$whole,$fraction"
    }

    private fun Int.pad2(): String = toString().padStart(2, '0')
}
