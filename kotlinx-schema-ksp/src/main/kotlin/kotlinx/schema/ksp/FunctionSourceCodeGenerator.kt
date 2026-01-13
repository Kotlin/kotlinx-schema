package kotlinx.schema.ksp

import kotlinx.schema.ksp.SourceCodeGeneratorHelpers.escapeForKotlinString

/**
 * Generator for function schema code.
 *
 * Generates functions for input parameter schemas:
 * - `{functionName}JsonSchemaString()` - Always generated
 * - `{functionName}JsonSchema()` - Only if withSchemaObject=true
 *
 * All functions are generated in a single file: `{functionName}FunctionSchema.kt`
 */
internal object FunctionSourceCodeGenerator {
    /**
     * Generates code for function input schema functions.
     *
     * @param packageName The package name for the generated file
     * @param functionName The function name
     * @param options KSP processor options
     * @param parameters Annotation parameters from @Schema
     * @param inputSchemaString The JSON schema string for input parameters
     * @param isExtensionFunction Whether the function is an extension function
     * @param receiverType The receiver type for extension functions (e.g., "String")
     * @return Generated Kotlin code as a string
     */
    @Suppress("LongParameterList")
    fun generateCode(
        packageName: String,
        functionName: String,
        options: Map<String, String>,
        parameters: Map<String, Any?>,
        inputSchemaString: String,
        isExtensionFunction: Boolean,
        receiverType: String?,
    ): String =
        buildString {
            // File header with function-specific suppressions
            append(
                SourceCodeGeneratorHelpers.generateFileHeader(
                    packageName = packageName,
                    additionalSuppressions = listOf("FunctionOnlyReturningConstant"),
                ),
            )

            // Determine extension prefix for extension functions
            val extensionPrefix =
                if (isExtensionFunction && receiverType != null) {
                    "$receiverType."
                } else {
                    ""
                }

            // Generate input schema string function (always)
            append(
                SourceCodeGeneratorHelpers.generateKDoc(
                    targetName = functionName,
                    description = "function providing input parameters JSON schema as string",
                ),
            )
            append(
                // language=kotlin
                """
                |public fun ${extensionPrefix}${functionName}JsonSchemaString(): String =
                |    // language=JSON
                |    ${inputSchemaString.escapeForKotlinString()}
                |
                """.trimMargin(),
            )

            // Generate input schema object function (optional)
            if (SourceCodeGeneratorHelpers.shouldGenerateSchemaObject(options, parameters)) {
                append(
                    SourceCodeGeneratorHelpers.generateKDoc(
                        targetName = functionName,
                        description = "function providing input parameters JSON schema as JsonObject",
                    ),
                )
                append(
                    // language=kotlin
                    """
                |public fun ${extensionPrefix}${functionName}JsonSchema(): kotlinx.serialization.json.JsonObject =
                |    kotlinx.serialization.json.Json.decodeFromString(${extensionPrefix}${functionName}JsonSchemaString())
                |
                    """.trimMargin(),
                )
            }
        }
}
