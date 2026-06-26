package gg.refx.android.core.network

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.time.Instant
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

/**
 * Tolerant ISO-8601 [Instant] serializer. The backend emits timestamps **with and
 * without fractional seconds** (and occasionally with a numeric offset rather than
 * `Z`). The iOS app tries the fractional formatter first, then the plain one — we
 * replicate that tolerance here so neither form throws (§3.2).
 *
 * Serializes back to a canonical `...Z` instant.
 */
object InstantIso8601Serializer : KSerializer<Instant> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("Instant", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): Instant {
        val raw = decoder.decodeString()
        // 1) ISO_INSTANT handles both "…:00Z" and "…:00.123Z".
        runCatching { return Instant.parse(raw) }
        // 2) Explicit offsets like "…+00:00" / "…-05:00".
        runCatching { return OffsetDateTime.parse(raw, DateTimeFormatter.ISO_OFFSET_DATE_TIME).toInstant() }
        // 3) Last resort: a comma fractional separator some locales emit ("…:00,123Z").
        runCatching { return Instant.parse(raw.replace(',', '.')) }
        throw IllegalArgumentException("Unparseable ISO-8601 timestamp: $raw")
    }

    override fun serialize(encoder: Encoder, value: Instant) {
        encoder.encodeString(DateTimeFormatter.ISO_INSTANT.format(value))
    }
}
