package gg.refx.android.core.network

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/**
 * Base serializer that decodes every server enum **permissively**: an unknown raw
 * value maps to the enum's [unknown] case instead of throwing, so a backend
 * addition never crashes the client (§3.3).
 *
 * Usage:
 * ```
 * @Serializable(with = ServerState.Serializer::class)
 * enum class ServerState(val raw: String) {
 *     RUNNING("running"), STOPPED("stopped"), UNKNOWN("unknown");
 *     internal object Serializer :
 *         PermissiveEnumSerializer<ServerState>("ServerState", entries.toTypedArray(), UNKNOWN, { it.raw })
 * }
 * ```
 */
abstract class PermissiveEnumSerializer<T : Enum<T>>(
    serialName: String,
    private val values: Array<T>,
    private val unknown: T,
    private val rawOf: (T) -> String,
) : KSerializer<T> {

    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor(serialName, PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): T {
        val raw = decoder.decodeString()
        return values.firstOrNull { rawOf(it).equals(raw, ignoreCase = true) } ?: unknown
    }

    override fun serialize(encoder: Encoder, value: T) {
        encoder.encodeString(rawOf(value))
    }
}
