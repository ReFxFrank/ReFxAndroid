package gg.refx.android

import gg.refx.android.core.network.InstantIso8601Serializer
import gg.refx.android.core.network.Money
import gg.refx.android.core.network.RefxJson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

/** Date tolerance (§3.2) and money formatting (§3.2) contract guards. */
class InstantAndMoneyTest {

    @Test fun parses_iso_without_fractional_seconds() {
        val parsed = RefxJson.decodeFromString(InstantIso8601Serializer, "\"2024-01-02T03:04:05Z\"")
        assertEquals(Instant.parse("2024-01-02T03:04:05Z"), parsed)
    }

    @Test fun parses_iso_with_fractional_seconds() {
        val parsed = RefxJson.decodeFromString(InstantIso8601Serializer, "\"2024-01-02T03:04:05.123Z\"")
        assertEquals(Instant.parse("2024-01-02T03:04:05.123Z"), parsed)
    }

    @Test fun parses_iso_with_numeric_offset() {
        val parsed = RefxJson.decodeFromString(InstantIso8601Serializer, "\"2024-01-02T03:04:05+00:00\"")
        assertEquals(Instant.parse("2024-01-02T03:04:05Z"), parsed)
    }

    @Test fun money_uses_minor_units_for_two_digit_currency() {
        // 1299 minor USD == 12.99 major.
        assertEquals(12.99, Money(1299, "USD").majorValue, 0.0001)
        assertTrue(Money(1299, "USD").formatted.contains("12.99"))
    }

    @Test fun money_respects_zero_fraction_currency() {
        // JPY has 0 minor-unit digits: 1000 minor == 1000 major.
        assertEquals(1000.0, Money(1000, "JPY").majorValue, 0.0001)
    }

    @Test fun money_of_returns_null_when_incomplete() {
        assertEquals(null, Money.of(null, "USD"))
        assertEquals(null, Money.of(100, null))
        assertEquals(Money(100, "USD"), Money.of(100, "USD"))
    }
}
