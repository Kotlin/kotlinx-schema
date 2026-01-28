package kotlinx.schema.ksp.strategy

import com.google.devtools.ksp.processing.KSPLogger
import kotlinx.schema.ksp.SchemaExtensionProcessor.Companion.OPTION_VISIBILITY
import kotlinx.schema.ksp.SchemaExtensionProcessor.Companion.OPTION_WITH_SCHEMA_OBJECT
import kotlinx.schema.ksp.SchemaExtensionProcessor.Companion.PARAM_WITH_SCHEMA_OBJECT

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
        this
            .resolve(
                paramName = null,
                optionName = OPTION_VISIBILITY,
            ).orEmpty()
            .trim()
    require(visibility in setOf("public", "internal", "private", "")) { "Invalid visibility option: $visibility" }
    return visibility
}

private fun CodeGenerationContext.resolve(
    paramName: String? = null,
    optionName: String,
): String? =
    if (paramName != null && parameters.containsKey(paramName)) {
        parameters[paramName]?.toString()
    } else if (options.containsKey(optionName)) {
        options[optionName]
    } else {
        null
    }

/**
 * Determines whether to generate schema object functions/properties.
 *
 * Priority:
 * 1. @Schema annotation parameter (withSchemaObject) - most specific
 * 2. KSP processor option (kotlinx.schema.withSchemaObject) - global fallback
 * 3. Default: false
 */
internal fun CodeGenerationContext.shouldGenerateSchemaObject(): Boolean =
    resolve(paramName = PARAM_WITH_SCHEMA_OBJECT, optionName = OPTION_WITH_SCHEMA_OBJECT)
        ?.let { it == "true" } ?: false
