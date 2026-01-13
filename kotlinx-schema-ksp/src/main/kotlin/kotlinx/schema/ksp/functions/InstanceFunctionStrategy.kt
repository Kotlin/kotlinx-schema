package kotlinx.schema.ksp.functions

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.symbol.ClassKind
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
 * Strategy for generating schemas for instance (member) function declarations.
 *
 * This strategy generates KClass extension functions for accessing function input parameter schemas:
 * - `KClass<ClassName>.{functionName}JsonSchemaString(): String` (always generated)
 * - `KClass<ClassName>.{functionName}JsonSchema(): FunctionCallingSchema` (conditionally generated)
 *
 * **Generated API Example:**
 * ```kotlin
 * class UserService {
 *     @Schema
 *     fun registerUser(username: String, email: String): String {
 *         return "registered"
 *     }
 * }
 *
 * // Generated extensions:
 * val schema: String = UserService::class.registerUserJsonSchemaString()
 * val schemaObject: FunctionCallingSchema = UserService::class.registerUserJsonSchema()  // if withSchemaObject=true
 * ```
 *
 * **NEW API:**
 * This strategy introduces a new API where instance methods generate KClass extensions
 * instead of top-level functions. This provides better namespace organization and makes
 * it clear which class the function belongs to.
 *
 * **Applies To:**
 * - Functions declared inside regular classes (not companion objects or singleton objects)
 * - Both regular and suspend instance functions
 */
internal class InstanceFunctionStrategy : SchemaGenerationStrategy<KSFunctionDeclaration> {
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
     * This strategy handles instance methods in regular classes.
     *
     * @param declaration The function declaration to check
     * @return true if the function is an instance method, false otherwise
     */
    override fun appliesTo(declaration: KSFunctionDeclaration): Boolean {
        val parent = declaration.parentDeclaration
        return parent is KSClassDeclaration &&
            !parent.isCompanionObject &&
            parent.classKind != ClassKind.OBJECT
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
     * This creates a file named `{functionName}FunctionSchema.kt` containing:
     * 1. KClass extension function returning schema string (always)
     * 2. KClass extension function returning schema object (conditional)
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

        // Get parent class information
        val parent = declaration.parentDeclaration as KSClassDeclaration
        val className = parent.qualifiedName?.asString() ?: run {
            val simpleClassName = parent.simpleName.asString()
            "$packageName.$simpleClassName"
        }

        // Handle generic type parameters with star projection
        val typeParameters = parent.typeParameters
        val classNameWithGenerics =
            if (typeParameters.isNotEmpty()) {
                val starProjections = typeParameters.joinToString(", ") { "*" }
                "$className<$starProjections>"
            } else {
                className
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
     * @param classNameWithGenerics The class name with generic parameters (e.g., "MyClass<*>")
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
