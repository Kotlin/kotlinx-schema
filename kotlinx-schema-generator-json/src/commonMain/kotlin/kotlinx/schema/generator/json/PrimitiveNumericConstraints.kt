package kotlinx.schema.generator.json

import kotlinx.schema.generator.core.ir.PrimitiveKind
import kotlinx.schema.generator.core.ir.PrimitiveNode

internal fun PrimitiveNode.minimumOrNull(): Double? =
    if (unsigned && (kind == PrimitiveKind.INT || kind == PrimitiveKind.LONG)) {
        0.0
    } else {
        null
    }
