package kotlinx.schema.ksp.strategy

import com.google.devtools.ksp.processing.KSPLogger
import kotlinx.schema.ksp.SchemaExtensionProcessor.Companion.OPTION_VISIBILITY
import kotlinx.schema.ksp.SchemaExtensionProcessor.Companion.PARAM_VISIBILITY

/**
 * Context data for schema code generation.
 *
 * This class provides shared context information to all code generation strategies,
 * encapsulating configuration and logging capabilities.
 *
 * @property options KSP processor options (e.g., kotlinx.schema.withSchemaObject)
 * @property parameters @Schema annotation parameters (e.g., withSchemaObject = true)
 * @property logger KSP logger for diagnostic messages
 */
internal data class CodeGenerationContext(
    val options: Map<String, String>,
    val parameters: Map<String, Any?>,
    val logger: KSPLogger,
)

/**
 * Determines the visibility modifier for generated schema functions.
 *
 * Priority:
 * 1. @Schema annotation parameter (visibility) - most specific
 * 2. KSP processor option (kotlinx.schema.visibility) - global fallback
 * 3. Default: "" (no visibility modifier)
 *
 * @return Visibility modifier string ("public", "internal", "private", or "")
 * @throws IllegalArgumentException if visibility value is invalid
 */
internal fun CodeGenerationContext.visibility(): String {
    val visibility =
        (this.parameters[PARAM_VISIBILITY] as? String)
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?: this.options[OPTION_VISIBILITY]
                ?.trim()
                ?.takeIf { it.isNotEmpty() }
            .orEmpty()
    require(visibility in setOf("public", "internal", "private", "")) { "Invalid visibility option: $visibility" }
    return visibility
}
