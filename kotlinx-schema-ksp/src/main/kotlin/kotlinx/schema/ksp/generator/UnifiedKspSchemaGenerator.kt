package kotlinx.schema.ksp.generator

import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import kotlinx.schema.generator.core.AbstractSchemaGenerator
import kotlinx.serialization.json.Json
import kotlin.reflect.KClass

/**
 * A unified implementation of schema generation that replaces both [KspClassSchemaGenerator]
 * and [KspFunctionSchemaGenerator].
 *
 * This class eliminates code duplication by making schema generation configuration-driven.
 * The same core logic works for both class and function schemas, with differences
 * expressed through the configuration.
 *
 * **Key differences handled by configuration:**
 * - Introspector (KspClassIntrospector vs KspFunctionIntrospector)
 * - Transformer (TypeGraphToJsonObjectSchemaTransformer vs TypeGraphToFunctionCallingSchemaTransformer)
 * - JSON formatting (prettyPrint, encodeDefaults)
 * - Return type (JsonObject vs FunctionCallingSchema)
 *
 * **Example usage:**
 * ```kotlin
 * // For classes
 * val classGenerator = UnifiedKspSchemaGenerator(
 *     KspSchemaGeneratorConfig(
 *         introspector = KspClassIntrospector(),
 *         transformer = TypeGraphToJsonObjectSchemaTransformer(),
 *         jsonPrettyPrint = true,
 *         jsonEncodeDefaults = true
 *     )
 * )
 *
 * // For functions
 * val functionGenerator = UnifiedKspSchemaGenerator(
 *     KspSchemaGeneratorConfig(
 *         introspector = KspFunctionIntrospector(),
 *         transformer = TypeGraphToFunctionCallingSchemaTransformer(),
 *         jsonPrettyPrint = false,
 *         jsonEncodeDefaults = false
 *     )
 * )
 * ```
 *
 * @param T the type of the KSP declaration for which the schema is being generated
 * @param R the type of the resulting schema representation
 * @property config Configuration specifying introspector, transformer, and JSON settings
 */
internal class UnifiedKspSchemaGenerator<T : Any, R : Any>(
    private val config: KspSchemaGeneratorConfig<T, R>,
) : AbstractSchemaGenerator<T, R>(
        introspector = config.introspector,
        typeGraphTransformer = config.transformer,
    ) {
    /**
     * JSON serializer configured according to the config settings.
     *
     * Classes use pretty-printed JSON with defaults, functions use compact JSON without defaults.
     */
    private val json =
        Json {
            prettyPrint = config.jsonPrettyPrint
            encodeDefaults = config.jsonEncodeDefaults
        }

    /**
     * Extracts the root name from the target declaration.
     *
     * The root name is the fully qualified name of the class or function,
     * used as the schema identifier in the type graph.
     *
     * @param target The declaration to extract the name from
     * @return Fully qualified name (e.g., "com.example.MyClass" or "com.example.myFunction")
     */
    override fun getRootName(target: T): String =
        when (target) {
            is KSClassDeclaration -> {
                val className = target.simpleName.asString()
                val packageName = target.packageName.asString()
                target.qualifiedName?.asString() ?: "$packageName.$className"
            }

            is KSFunctionDeclaration -> {
                val functionName = target.simpleName.asString()
                val packageName = target.packageName.asString()
                target.qualifiedName?.asString() ?: "$packageName.$functionName"
            }

            else -> {
                error("Unsupported target type: ${target::class.simpleName}")
            }
        }

    /**
     * Returns the KClass for the target type.
     *
     * This method is required by [AbstractSchemaGenerator] but not actually used in the KSP context,
     * as KSP uses compile-time symbol processing rather than runtime reflection.
     *
     * @throws UnsupportedOperationException Always, as this method should not be called
     */
    override fun targetType(): KClass<T> =
        throw UnsupportedOperationException("targetType() is not used in KSP-based generation")

    /**
     * Returns the KClass for the schema type.
     *
     * This method is required by [AbstractSchemaGenerator] but not actually used in the KSP context,
     * as KSP uses compile-time symbol processing rather than runtime reflection.
     *
     * @throws UnsupportedOperationException Always, as this method should not be called
     */
    override fun schemaType(): KClass<R> =
        throw UnsupportedOperationException("schemaType() is not used in KSP-based generation")

    /**
     * Encodes the schema to a JSON string using the configured JSON serializer.
     *
     * @param schema The schema object to encode
     * @return JSON string representation of the schema
     */
    override fun encodeToString(schema: R): String = json.encodeToString(config.serializer, schema)
}
