package kotlinx.schema.generator.reflect
import kotlinx.schema.generator.core.ir.ObjectNode
import kotlinx.schema.generator.core.ir.Property
import kotlinx.schema.generator.core.ir.SchemaIntrospector
import kotlinx.schema.generator.core.ir.TypeGraph
import kotlinx.schema.generator.core.ir.TypeId
import kotlinx.schema.generator.core.ir.TypeRef
import kotlin.reflect.KCallable
import kotlin.reflect.KClass
import kotlin.reflect.KParameter

/**
 * Introspects Kotlin functions/methods using reflection to build a [TypeGraph].
 *
 * This introspector analyzes function parameters and their types to generate
 * schema IR nodes suitable for tool/function schema generation.
 *
 * ## Example
 * ```kotlin
 * fun myFunction(name: String, age: Int = 0): String = "Hello"
 * val typeGraph = ReflectionFunctionIntrospector.introspect(::myFunction)
 * ```
 */
public object ReflectionFunctionIntrospector : SchemaIntrospector<KCallable<*>> {
    override fun introspect(root: KCallable<*>): TypeGraph {
        require(!root.isSuspend) { "Suspend functions are not supported" }
        require(root.parameters.none { it.kind == KParameter.Kind.EXTENSION_RECEIVER }) {
            "Extension functions are not supported"
        }

        val context = IntrospectionContext()
        val rootRef = context.convertFunctionToTypeRef(root)
        return TypeGraph(root = rootRef, nodes = context.discoveredNodes)
    }

    /**
     * Maintains state during function introspection including discovered nodes,
     * visited classes, and type reference cache.
     */
    @Suppress("TooManyFunctions")
    private class IntrospectionContext : BaseIntrospectionContext() {
        /**
         * Converts a KCallable (function) to a TypeRef representing its parameters as an object.
         */
        fun convertFunctionToTypeRef(callable: KCallable<*>): TypeRef {
            val functionName = callable.name
            val id = TypeId(functionName)

            // Create an ObjectNode representing the function parameters
            val properties = mutableListOf<Property>()
            val requiredProperties = mutableSetOf<String>()

            callable.parameters.forEach { param ->
                // Skip instance parameter for member functions
                if (param.kind == KParameter.Kind.INSTANCE) return@forEach

                val paramName = param.name ?: return@forEach
                val paramType = param.type
                val hasDefault = param.isOptional

                val typeRef = convertKTypeToTypeRef(paramType)

                // Extract description from annotations
                val description = extractDescription(param.annotations)

                properties +=
                    Property(
                        name = paramName,
                        type = typeRef,
                        description = description,
                        hasDefaultValue = hasDefault,
                    )

                if (!hasDefault) {
                    requiredProperties += paramName
                }
            }

            val objectNode =
                ObjectNode(
                    name = functionName,
                    properties = properties,
                    required = requiredProperties,
                    description = extractDescription(callable.annotations),
                )

            discoveredNodes[id] = objectNode
            return TypeRef.Ref(id, nullable = false)
        }

        override fun createObjectNode(
            klass: KClass<*>,
            parentPrefix: String?,
        ): ObjectNode {
            // Check for @Serializable annotation - not supported
            val hasSerializable =
                klass.annotations.any {
                    it.annotationClass.qualifiedName == "kotlinx.serialization.Serializable"
                }
            require(!hasSerializable) {
                "ReflectionFunctionIntrospector does not support classes annotated with @Serializable " +
                    "(${klass.qualifiedName}). The kotlinx.serialization compiler plugin modifies the " +
                    "constructor structure, making reflection-based introspection unreliable. " +
                    "Please remove the @Serializable annotation or use a different introspector."
            }

            val properties = mutableListOf<Property>()
            val requiredProperties = mutableSetOf<String>()

            // Try to extract default values by creating an instance
            val defaultValues = DefaultValueExtractor.extractDefaultValues(klass)

            // Extract properties from primary constructor
            klass.constructors.firstOrNull()?.parameters?.forEach { param ->
                val propertyName = param.name ?: return@forEach
                val hasDefault = param.isOptional

                // Find the corresponding property to get annotations
                val property = findPropertyByName(klass, propertyName)

                val propertyType = param.type
                val typeRef = convertKTypeToTypeRef(propertyType)

                // Get the actual default value if available
                val defaultValue = if (hasDefault) defaultValues[propertyName] else null

                properties +=
                    Property(
                        name = propertyName,
                        type = typeRef,
                        description = property?.let { extractDescription(it.annotations) },
                        hasDefaultValue = hasDefault,
                        defaultValue = defaultValue,
                    )

                if (!hasDefault) {
                    requiredProperties += propertyName
                }
            }

            return ObjectNode(
                name = klass.simpleName ?: "UnknownClass",
                properties = properties,
                required = requiredProperties,
                description = extractDescription(klass.annotations),
            )
        }
    }
}
