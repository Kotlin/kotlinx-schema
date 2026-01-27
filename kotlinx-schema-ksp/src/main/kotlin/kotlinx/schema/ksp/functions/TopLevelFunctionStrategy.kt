package kotlinx.schema.ksp.functions

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.symbol.KSFile
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import kotlinx.schema.generator.json.TypeGraphToFunctionCallingSchemaTransformer
import kotlinx.schema.json.FunctionCallingSchema
import kotlinx.schema.ksp.SourceCodeGeneratorHelpers
import kotlinx.schema.ksp.SourceCodeGeneratorHelpers.escapeForKotlinString
import kotlinx.schema.ksp.SourceCodeGeneratorHelpers.visibilityPrefix
import kotlinx.schema.ksp.generator.KspSchemaGeneratorConfig
import kotlinx.schema.ksp.generator.UnifiedKspSchemaGenerator
import kotlinx.schema.ksp.ir.KspFunctionIntrospector
import kotlinx.schema.ksp.strategy.CodeGenerationContext
import kotlinx.schema.ksp.strategy.SchemaGenerationStrategy
import kotlinx.schema.ksp.strategy.visibility

/**
 * Strategy for generating schemas for top-level function declarations.
 *
 * This strategy generates top-level functions for accessing function input parameter schemas:
 * - `{functionName}JsonSchemaString(): String` (always generated)
 * - `{functionName}JsonSchema(): FunctionCallingSchema` (conditionally generated)
 *
 * **Generated API Example:**
 * ```kotlin
 * @Schema
 * fun greetPerson(name: String, greeting: String = "Hello"): String {
 *     return "$greeting, $name"
 * }
 *
 * // Generated functions:
 * val schema: String = greetPersonJsonSchemaString()
 * val schemaObject: FunctionCallingSchema = greetPersonJsonSchema()  // if withSchemaObject=true
 * ```
 *
 * **Backward Compatibility:**
 * This strategy maintains the exact same API as the previous implementation,
 * ensuring no breaking changes for top-level function schema generation.
 *
 * **Applies To:**
 * - Functions declared at package level (parentDeclaration is null or KSFile)
 * - Extension functions declared at package level
 */
internal class TopLevelFunctionStrategy : SchemaGenerationStrategy<KSFunctionDeclaration> {
    /**
     * Unified schema generator configured for function schemas.
     *
     * Configuration:
     * - Compact JSON (no pretty-print)
     * - Excludes default values
     * - Uses FunctionCallingSchema serializer
     */
    private val generator =
        UnifiedKspSchemaGenerator(
            KspSchemaGeneratorConfig(
                introspector = KspFunctionIntrospector(),
                transformer = TypeGraphToFunctionCallingSchemaTransformer(),
                serializer = FunctionCallingSchema.serializer(),
                jsonPrettyPrint = false,
                jsonEncodeDefaults = false,
            ),
        )

    /**
     * Determines if this strategy applies to the given function.
     *
     * This strategy handles top-level functions (those declared at package level).
     *
     * @param declaration The function declaration to check
     * @return true if the function is top-level, false otherwise
     */
    override fun appliesTo(declaration: KSFunctionDeclaration): Boolean {
        val parent = declaration.parentDeclaration
        return parent == null || parent is KSFile
    }

    /**
     * Generates the function calling schema string for the function.
     *
     * @param declaration The function declaration to generate schema for
     * @param context Generation context (unused for schema generation, but required by interface)
     * @return Function calling schema as JSON string
     */
    override fun generateSchema(
        declaration: KSFunctionDeclaration,
        context: CodeGenerationContext,
    ): String = generator.generateSchemaString(declaration)

    /**
     * Generates the Kotlin source code file with top-level schema functions.
     *
     * This creates a file named `{functionName}FunctionSchema.kt` containing:
     * 1. Top-level function returning schema string (always)
     * 2. Top-level function returning schema object (conditional)
     *
     * @param declaration The function declaration to generate code for
     * @param schemaString The pre-generated schema JSON string
     * @param context Generation context with options and annotation parameters
     * @param codeGenerator KSP code generator for creating files
     */
    override fun generateCode(
        declaration: KSFunctionDeclaration,
        schemaString: String,
        context: CodeGenerationContext,
        codeGenerator: CodeGenerator,
    ) {
        val functionName = declaration.simpleName.asString()
        val packageName = declaration.packageName.asString()

        // Generate the complete source file content
        val sourceCode = buildTopLevelFunctions(packageName, functionName, schemaString, context)

        // Write the file
        val fileName = "${functionName}FunctionSchema"
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
     * Builds the complete source code for the top-level schema functions.
     *
     * @param packageName The package name for the generated file
     * @param functionName The function name
     * @param schemaString The function calling schema JSON string
     * @param context Generation context for determining what to generate
     * @return Complete Kotlin source code as a string
     */
    private fun buildTopLevelFunctions(
        packageName: String,
        functionName: String,
        schemaString: String,
        context: CodeGenerationContext,
    ): String =
        buildString {
            // File header with suppressions
            append(
                SourceCodeGeneratorHelpers.generateFileHeader(
                    packageName = packageName,
                    additionalSuppressions = listOf("FunctionOnlyReturningConstant"),
                ),
            )

            // Generate schema string function (always)
            append(
                SourceCodeGeneratorHelpers.generateKDoc(
                    targetName = functionName,
                    description = "function providing input parameters JSON schema as string",
                ),
            )
            val visibilityPrefix = visibilityPrefix(context)

            append(
                // language=kotlin
                """
                |${visibilityPrefix}fun ${functionName}JsonSchemaString(): String =
                |    // language=JSON
                |    ${schemaString.escapeForKotlinString()}
                |
                """.trimMargin(),
            )

            // Generate schema object function (conditional)
            if (SourceCodeGeneratorHelpers.shouldGenerateSchemaObject(context.options, context.parameters)) {
                append(
                    SourceCodeGeneratorHelpers.generateKDoc(
                        targetName = functionName,
                        description = "function providing input parameters JSON schema as JsonObject",
                    ),
                )
                append(
                    // language=kotlin
                    """
                |${visibilityPrefix}fun ${functionName}JsonSchema(): kotlinx.serialization.json.JsonObject =
                |    kotlinx.serialization.json.Json.decodeFromString(${functionName}JsonSchemaString())
                |
                    """.trimMargin(),
                )
            }
        }
}
