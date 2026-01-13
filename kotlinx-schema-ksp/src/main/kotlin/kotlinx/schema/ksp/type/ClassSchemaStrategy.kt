package kotlinx.schema.ksp.type

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.symbol.KSClassDeclaration
import kotlinx.schema.generator.json.TypeGraphToJsonObjectSchemaTransformer
import kotlinx.schema.ksp.SourceCodeGeneratorHelpers
import kotlinx.schema.ksp.SourceCodeGeneratorHelpers.escapeForKotlinString
import kotlinx.schema.ksp.generator.KspSchemaGeneratorConfig
import kotlinx.schema.ksp.generator.UnifiedKspSchemaGenerator
import kotlinx.schema.ksp.ir.KspClassIntrospector
import kotlinx.schema.ksp.strategy.CodeGenerationContext
import kotlinx.schema.ksp.strategy.SchemaGenerationStrategy
import kotlinx.serialization.json.JsonObject

/**
 * Strategy for generating schemas for class declarations.
 *
 * This strategy generates KClass extension properties for accessing class schemas:
 * - `KClass<MyClass>.jsonSchemaString: String` (always generated)
 * - `KClass<MyClass>.jsonSchema: JsonObject` (conditionally generated)
 *
 * **Generated API Example:**
 * ```kotlin
 * @Schema
 * data class User(val name: String, val age: Int)
 *
 * // Generated extensions:
 * val schema: String = User::class.jsonSchemaString
 * val schemaObject: JsonObject = User::class.jsonSchema  // if withSchemaObject=true
 * ```
 *
 * **Backward Compatibility:**
 * This strategy maintains the exact same API as the previous implementation,
 * ensuring no breaking changes for class schema generation.
 */
internal class ClassSchemaStrategy : SchemaGenerationStrategy<KSClassDeclaration> {
    /**
     * Unified schema generator configured for class schemas.
     *
     * Configuration:
     * - Pretty-printed JSON
     * - Includes default values
     * - Uses JsonObject serializer
     */
    private val generator =
        UnifiedKspSchemaGenerator(
            KspSchemaGeneratorConfig(
                introspector = KspClassIntrospector(),
                transformer = TypeGraphToJsonObjectSchemaTransformer(),
                serializer = JsonObject.serializer(),
                jsonPrettyPrint = true,
                jsonEncodeDefaults = true,
            ),
        )

    /**
     * This strategy applies to all class declarations.
     *
     * @param declaration The class declaration to check
     * @return Always true (handles all classes)
     */
    override fun appliesTo(declaration: KSClassDeclaration): Boolean = true

    /**
     * Generates the JSON schema string for the class.
     *
     * @param declaration The class declaration to generate schema for
     * @param context Generation context (unused for schema generation, but required by interface)
     * @return JSON schema string for the class
     */
    override fun generateSchema(
        declaration: KSClassDeclaration,
        context: CodeGenerationContext,
    ): String = generator.generateSchemaString(declaration)

    /**
     * Generates the Kotlin source code file with KClass extension properties.
     *
     * This creates a file named `{ClassName}SchemaExtensions.kt` containing:
     * 1. Extension property for schema string (always)
     * 2. Extension property for schema object (conditional)
     *
     * @param declaration The class declaration to generate code for
     * @param schemaString The pre-generated schema JSON string
     * @param context Generation context with options and annotation parameters
     * @param codeGenerator KSP code generator for creating files
     */
    override fun generateCode(
        declaration: KSClassDeclaration,
        schemaString: String,
        context: CodeGenerationContext,
        codeGenerator: CodeGenerator,
    ) {
        val className = declaration.simpleName.asString()
        val packageName = declaration.packageName.asString()
        val qualifiedName = declaration.qualifiedName?.asString() ?: "$packageName.$className"

        // Handle generic type parameters with star projection
        val typeParameters = declaration.typeParameters
        val classNameWithGenerics =
            if (typeParameters.isNotEmpty()) {
                val starProjections = typeParameters.joinToString(", ") { "*" }
                "$qualifiedName<$starProjections>"
            } else {
                qualifiedName
            }

        // Generate the complete source file content
        val sourceCode = buildClassExtensions(packageName, classNameWithGenerics, schemaString, context)

        // Write the file
        val fileName = "${className}SchemaExtensions"
        val file =
            codeGenerator.createNewFile(
                dependencies = Dependencies(aggregating = false, declaration.containingFile!!),
                packageName = packageName,
                fileName = fileName,
            )

        file.write(sourceCode.toByteArray())
        file.close()
    }

    /**
     * Builds the complete source code for the class schema extensions.
     *
     * @param packageName The package name for the generated file
     * @param classNameWithGenerics The class name with generic parameters (e.g., "MyClass<*>")
     * @param schemaString The JSON schema string
     * @param context Generation context for determining what to generate
     * @return Complete Kotlin source code as a string
     */
    private fun buildClassExtensions(
        packageName: String,
        classNameWithGenerics: String,
        schemaString: String,
        context: CodeGenerationContext,
    ): String =
        buildString {
            // File header with suppressions
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

            // Generate jsonSchema extension property (conditional)
            if (SourceCodeGeneratorHelpers.shouldGenerateSchemaObject(context.options, context.parameters)) {
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
