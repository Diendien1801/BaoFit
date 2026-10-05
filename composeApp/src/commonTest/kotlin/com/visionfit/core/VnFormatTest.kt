package com.visionfit.core

import com.visionfit.core.format.VnFormat
import com.visionfit.domain.usecase.CredentialError
import com.visionfit.domain.usecase.ValidateCredentialsUseCase
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds

class VnFormatTest {

    @Test
    fun numbersUseVietnameseSeparators() {
        assertEquals("2.034", VnFormat.thousands(2034))
        assertEquals("371", VnFormat.thousands(371))
        assertEquals("1.234.567", VnFormat.thousands(1_234_567))
        assertEquals("−500", VnFormat.thousands(-500))
        assertEquals("+300", VnFormat.signed(300))
        assertEquals("54,1", VnFormat.oneDecimal(54.1))
        assertEquals("46,8", VnFormat.oneDecimal(46.75))
        assertEquals("46", VnFormat.oneDecimal(46.0))
        assertEquals("1,55", VnFormat.factor(1.55))
        assertEquals("1,2", VnFormat.factor(1.2))
        assertEquals("1,2 MB", VnFormat.megabytes(1_200_000))
        assertEquals("4,2s", VnFormat.seconds(4_200.milliseconds))
    }

    @Test
    fun datesUseVietnameseWeekdays() {
        val monday = LocalDate(2026, 10, 5)
        assertEquals("Thứ Hai, 05/10", VnFormat.longDate(monday))
        assertEquals("CN 04/10", VnFormat.shortDate(LocalDate(2026, 10, 4)))
        assertEquals("07:30", VnFormat.time(LocalTime(7, 30)))
    }

    @Test
    fun credentialsValidation() {
        val validate = ValidateCredentialsUseCase()
        assertTrue(validate("an@visionfit.vn", "visionfit123").isValid)
        assertEquals(CredentialError.EMAIL_BLANK, validate(" ", "visionfit123").email)
        assertEquals(CredentialError.EMAIL_INVALID, validate("an@visionfit", "visionfit123").email)
        assertEquals(CredentialError.PASSWORD_TOO_SHORT, validate("an@visionfit.vn", "1234567").password)
        assertEquals(CredentialError.CONFIRMATION_MISMATCH, validate("an@visionfit.vn", "visionfit123", "visionfit12").confirmation)
        assertNull(validate("an@visionfit.vn", "visionfit123", "visionfit123").confirmation)
    }
}
