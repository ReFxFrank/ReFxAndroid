package gg.refx.android.core.network

import kotlinx.serialization.Serializable
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

/**
 * Money as **integer minor units** + ISO-4217 currency code. Never use floats for
 * money (§3.2). Mirrors the iOS `Money` value type and its `.formatted`.
 *
 * Some API payloads carry the pair inline as `{ amountMinor, currency }`; this
 * type is `@Serializable` for those cases, but most call sites build it from two
 * scalar fields.
 */
@Serializable
data class Money(
    val minorUnits: Long,
    val currency: String,
) {
    /** Number of minor-unit fraction digits for [currency] (e.g. 2 for USD, 0 for JPY). */
    private val fractionDigits: Int
        get() = runCatching { Currency.getInstance(currency.uppercase()).defaultFractionDigits }
            .getOrDefault(2)
            .coerceAtLeast(0)

    /** The major-unit decimal value (e.g. 1299 minor USD → 12.99). */
    val majorValue: Double
        get() = minorUnits.toDouble() / Math.pow(10.0, fractionDigits.toDouble())

    /** Localized currency string, e.g. "$12.99". Mirrors the iOS `.formatted`. */
    val formatted: String
        get() = runCatching {
            val fmt = NumberFormat.getCurrencyInstance(Locale.getDefault())
            fmt.currency = Currency.getInstance(currency.uppercase())
            fmt.minimumFractionDigits = fractionDigits
            fmt.maximumFractionDigits = fractionDigits
            fmt.format(majorValue)
        }.getOrElse {
            // Unknown/non-ISO currency: fall back to "12.99 XXX".
            "%.2f %s".format(majorValue, currency.uppercase())
        }

    companion object {
        fun of(minorUnits: Long?, currency: String?): Money? =
            if (minorUnits != null && !currency.isNullOrBlank()) Money(minorUnits, currency) else null
    }
}
