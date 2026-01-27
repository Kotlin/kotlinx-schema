package kotlinx.schema.ksp.functions

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import kotlinx.schema.generator.json.TypeGraphToFunctionCallingSchemaTransformer
import kotlinx.schema.json.FunctionCallingSchema
import kotlinx.schema.ksp.SourceCodeGeneratorHelpers
import kotlinx.schema.ksp.SourceCodeGeneratorHelpers.buildKClassExtensions
import kotlinx.schema.ksp.SourceCodeGeneratorHelpers.escapeForKotlinString
import kotlinx.schema.ksp.generator.KspSchemaGeneratorConfig
import kotlinx.schema.ksp.generator.UnifiedKspSchemaGenerator
import kotlinx.schema.ksp.ir.KspFunctionIntrospector
import kotlinx.schema.ksp.strategy.CodeGenerationContext
import kotlinx.schema.ksp.strategy.SchemaGenerationStrategy

/**
 * Strategy for generating schemas for singleton object function declarations.
 *
 * This strategy generates KClass extension functions on the object type for accessing
 * function input parameter schemas:
 * - `KClass<ObjectName>.{functionName}JsonSchemaString(): String` (always generated)
 * - `KClass<ObjectName>.{functionName}JsonSchema(): FunctionCallingSchema` (conditionally generated)
 *
 * **Generated API Example:**
 * ```kotlin
 * object ConfigurationManager {
 *     @Schema
 *     fun loadConfig(filePath: String): Map<String, String> {
 *         return mapOf("loaded" to "true")
 *     }
 * }
 *
 * // Generated extensions:
 * val schema: String = ConfigurationManager::class.loadConfigJsonSchemaString()
 *
 * // if withSchemaObject=true
 * val schemaObject: FunctionCallingSchema = ConfigurationManager::class.loadConfigJsonSchema()
 * ```
 *
 * **NEW API:**
 * This strategy introduces a new API where singleton object methods generate KClass extensions
 * on the object type. This provides better namespace organization and makes it clear that
 * the function belongs to the singleton object.
 *
 * **Applies To:**
 * - Functions declared inside singleton objects (declared with `object` keyword)
 * - Both regular and suspend object functions
 */
internal class ObjectFunctionStrategy : SchemaGenerationStrategy<KSFunctionDeclaration> {
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
     * This strategy handles functions declared in singleton objects.
     *
     * @param declaration The function declaration to check
     * @return true if the function is in a singleton object, false otherwise
     */
    override fun appliesTo(declaration: KSFunctionDeclaration): Boolean {
        val parent = declaration.parentDeclaration
        return parent is KSClassDeclaration && parent.classKind == ClassKind.OBJECT
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
     * 1. KClass extension function on object type returning schema string (always)
     * 2. KClass extension function on object type returning schema object (conditional)
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

        // Get object information
        val objectDeclaration = declaration.parentDeclaration as KSClassDeclaration
        val objectName =
            objectDeclaration.qualifiedName?.asString() ?: run {
                val simpleObjectName = objectDeclaration.simpleName.asString()
                "$packageName.$simpleObjectName"
            }

        // Objects typically don't have type parameters, but handle them just in case
        val typeParameters = objectDeclaration.typeParameters
        val objectNameWithGenerics =
            if (typeParameters.isNotEmpty()) {
                val starProjections = typeParameters.joinToString(", ") { "*" }
                "$objectName<$starProjections>"
            } else {
                objectName
            }

        // Generate the complete source file content
        val sourceCode =
            buildKClassExtensions(
                packageName,
                objectNameWithGenerics,
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
}
