package kotlinx.schema.ksp

import kotlinx.schema.ksp.SourceCodeGeneratorHelpers.escapeForKotlinString

/**
 * Generator for class schema extension properties.
 *
 * Generates extension properties like:
 * - `KClass<MyClass>.jsonSchemaString` - Always generated
 * - `KClass<MyClass>.jsonSchema` - Only if withSchemaObject=true
 */
internal object ClassSourceCodeGenerator {
    /**
     * Generates code for class schema extension properties.
     *
     * @param packageName The package name for the generated file
     * @param classNameWithGenerics The class name with generic parameters (e.g., "MyClass<*>")
     * @param options KSP processor options
     * @param parameters Annotation parameters from @Schema
     * @param schemaString The JSON schema string for the class
     * @return Generated Kotlin code as a string
     */
    fun generateCode(
        packageName: String,
        classNameWithGenerics: String,
        options: Map<String, String>,
        parameters: Map<String, Any?>,
        schemaString: String,
    ): String =
        buildString {
            // File header with class-specific suppressions
            append(
                SourceCodeGeneratorHelpers.generateFileHeader(
                    packageName = packageName,
                    additionalSuppressions = listOf("UnusedReceiverParameter"),
                ),
            )

            // Generate jsonSchemaString extension property (always)
            append(
                SourceCodeGeneratorHelpers.generateKDoc(
                    targetName = classNameWithGenerics,
                    description = "extension property providing JSON schema",
                ),
            )
            append(
                // language=kotlin
                """
                |public val kotlin.reflect.KClass<$classNameWithGenerics>.jsonSchemaString: String
                |    get() =
                |        // language=JSON
                |        ${schemaString.escapeForKotlinString()}
                |
                """.trimMargin(),
            )

            // Generate jsonSchema extension property (optional)
            if (SourceCodeGeneratorHelpers.shouldGenerateSchemaObject(options, parameters)) {
                append(
                    SourceCodeGeneratorHelpers.generateKDoc(
                        targetName = classNameWithGenerics,
                        description = "extension property providing JSON schema as JsonObject",
                    ),
                )
                append(
                    // language=kotlin
                    """
                |public val kotlin.reflect.KClass<$classNameWithGenerics>.jsonSchema: kotlinx.serialization.json.JsonObject
                |    get() = kotlinx.serialization.json.Json.decodeFromString<kotlinx.serialization.json.JsonObject>(jsonSchemaString)
                |
                    """.trimMargin(),
                )
            }
        }
}
