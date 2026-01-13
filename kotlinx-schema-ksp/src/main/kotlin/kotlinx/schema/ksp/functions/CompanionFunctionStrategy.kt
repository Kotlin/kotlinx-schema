package kotlinx.schema.ksp.functions

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import kotlinx.schema.generator.json.TypeGraphToFunctionCallingSchemaTransformer
import kotlinx.schema.json.FunctionCallingSchema
import kotlinx.schema.ksp.SourceCodeGeneratorHelpers
import kotlinx.schema.ksp.SourceCodeGeneratorHelpers.escapeForKotlinString
import kotlinx.schema.ksp.generator.KspSchemaGeneratorConfig
import kotlinx.schema.ksp.generator.UnifiedKspSchemaGenerator
import kotlinx.schema.ksp.ir.KspFunctionIntrospector
import kotlinx.schema.ksp.strategy.CodeGenerationContext
import kotlinx.schema.ksp.strategy.SchemaGenerationStrategy

/**
 * Strategy for generating schemas for companion object function declarations.
 *
 * This strategy generates KClass extension functions on the parent class for accessing
 * function input parameter schemas:
 * - `KClass<ParentClass>.{functionName}JsonSchemaString(): String` (always generated)
 * - `KClass<ParentClass>.{functionName}JsonSchema(): FunctionCallingSchema` (conditionally generated)
 *
 * **Generated API Example:**
 * ```kotlin
 * class DatabaseConnection {
 *     companion object {
 *         @Schema
 *         fun create(host: String, port: Int): DatabaseConnection {
 *             return DatabaseConnection()
 *         }
 *     }
 * }
 *
 * // Generated extensions on the parent class:
 * val schema: String = DatabaseConnection::class.createJsonSchemaString()
 * val schemaObject: FunctionCallingSchema = DatabaseConnection::class.createJsonSchema()  // if withSchemaObject=true
 * ```
 *
 * **NEW API:**
 * This strategy introduces a new API where companion object methods generate KClass extensions
 * on the parent class. This provides better namespace organization and makes the factory/builder
 * pattern more intuitive.
 *
 * **Applies To:**
 * - Functions declared inside companion objects
 * - Both regular and suspend companion functions
 */
internal class CompanionFunctionStrategy : SchemaGenerationStrategy<KSFunctionDeclaration> {
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
     * This strategy handles functions declared in companion objects.
     *
     * @param declaration The function declaration to check
     * @return true if the function is in a companion object, false otherwise
     */
    override fun appliesTo(declaration: KSFunctionDeclaration): Boolean {
        val parent = declaration.parentDeclaration
        return parent is KSClassDeclaration && parent.isCompanionObject
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
     * Generates the Kotlin source code file with KClass extension functions.
     *
     * The extensions are generated on the parent class, not the companion object itself.
     *
     * This creates a file named `{functionName}FunctionSchema.kt` containing:
     * 1. KClass extension function on parent class returning schema string (always)
     * 2. KClass extension function on parent class returning schema object (conditional)
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

        // Get companion object's parent class information
        val companionObject = declaration.parentDeclaration as KSClassDeclaration
        val parentClass =
            companionObject.parentDeclaration as? KSClassDeclaration
                ?: error("Companion object must have a parent class")

        val parentClassName =
            parentClass.qualifiedName?.asString() ?: run {
                val simpleClassName = parentClass.simpleName.asString()
                "$packageName.$simpleClassName"
            }

        // Handle generic type parameters with star projection
        val typeParameters = parentClass.typeParameters
        val classNameWithGenerics =
            if (typeParameters.isNotEmpty()) {
                val starProjections = typeParameters.joinToString(", ") { "*" }
                "$parentClassName<$starProjections>"
            } else {
                parentClassName
            }

        // Generate the complete source file content
        val sourceCode =
            buildKClassExtensions(
                packageName,
                classNameWithGenerics,
                functionName,
                schemaString,
                context,
            )

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
     * Builds the complete source code for the KClass extension functions.
     *
     * @param packageName The package name for the generated file
     * @param classNameWithGenerics The parent class name with generic parameters (e.g., "MyClass<*>")
     * @param functionName The function name
     * @param schemaString The function calling schema JSON string
     * @param context Generation context for determining what to generate
     * @return Complete Kotlin source code as a string
     */
    private fun buildKClassExtensions(
        packageName: String,
        classNameWithGenerics: String,
        functionName: String,
        schemaString: String,
        context: CodeGenerationContext,
    ): String =
        buildString {
            // File header with suppressions
            append(
                SourceCodeGeneratorHelpers.generateFileHeader(
                    packageName = packageName,
                    additionalSuppressions = listOf("FunctionOnlyReturningConstant", "UnusedReceiverParameter"),
                ),
            )

            // Generate schema string extension function (always)
            append(
                SourceCodeGeneratorHelpers.generateKDoc(
                    targetName = functionName,
                    description = "extension function providing input parameters JSON schema as string",
                ),
            )
            append(
                // language=kotlin
                """
                |public fun kotlin.reflect.KClass<$classNameWithGenerics>.${functionName}JsonSchemaString(): String =
                |    // language=JSON
                |    ${schemaString.escapeForKotlinString()}
                |
                """.trimMargin(),
            )

            // Generate schema object extension function (conditional)
            if (SourceCodeGeneratorHelpers.shouldGenerateSchemaObject(context.options, context.parameters)) {
                append(
                    SourceCodeGeneratorHelpers.generateKDoc(
                        targetName = functionName,
                        description = "extension function providing input parameters JSON schema as JsonObject",
                    ),
                )
                append(
                    // language=kotlin
                    """
                |public fun kotlin.reflect.KClass<$classNameWithGenerics>.${functionName}JsonSchema(): kotlinx.serialization.json.JsonObject =
                |    kotlinx.serialization.json.Json.decodeFromString(this.${functionName}JsonSchemaString())
                |
                    """.trimMargin(),
                )
            }
        }
}
