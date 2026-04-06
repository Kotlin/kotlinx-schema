package kotlinx.schema.generator.json.serialization

import kotlinx.serialization.descriptors.PolymorphicKind
import kotlinx.serialization.descriptors.SerialDescriptor

private const val POLYMORPHIC_PREFIX = "kotlinx.serialization.Polymorphic<"
private const val POLYMORPHIC_SUFFIX = ">"

/**
 * Returns the base type serial name, unwrapping the `kotlinx.serialization.Polymorphic<Name>`
 * wrapper for open polymorphic descriptors.
 *
 * For all other descriptor kinds, returns [SerialDescriptor.serialName] unchanged.
 */
internal fun SerialDescriptor.unwrapSerialName(): String =
    if (kind is PolymorphicKind.OPEN) {
        serialName.removeSurrounding(POLYMORPHIC_PREFIX, POLYMORPHIC_SUFFIX)
    } else {
        serialName
    }
