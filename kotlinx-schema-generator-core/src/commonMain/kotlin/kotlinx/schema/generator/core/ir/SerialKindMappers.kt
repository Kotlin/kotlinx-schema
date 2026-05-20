package kotlinx.schema.generator.core.ir

import kotlinx.serialization.descriptors.PrimitiveKind as SerialPrimitiveKind
import kotlinx.serialization.descriptors.SerialKind

/**
 * Maps a kotlinx-serialization [SerialKind] to a kotlinx-schema [PrimitiveKind] when
 * the kind describes a primitive. Returns `null` for any structural, enum, or polymorphic
 * kind so the caller can fall through to existing handling.
 *
 * This mapping is shared by both the reflection-based and serialization-based introspectors
 * to guarantee consistent primitive resolution across code paths.
 */
public fun serialKindToPrimitiveKind(kind: SerialKind): PrimitiveKind? = when (kind) {
    SerialPrimitiveKind.STRING, SerialPrimitiveKind.CHAR -> PrimitiveKind.STRING
    SerialPrimitiveKind.BOOLEAN -> PrimitiveKind.BOOLEAN
    SerialPrimitiveKind.BYTE, SerialPrimitiveKind.SHORT, SerialPrimitiveKind.INT -> PrimitiveKind.INT
    SerialPrimitiveKind.LONG -> PrimitiveKind.LONG
    SerialPrimitiveKind.FLOAT -> PrimitiveKind.FLOAT
    SerialPrimitiveKind.DOUBLE -> PrimitiveKind.DOUBLE
    else -> null
}
