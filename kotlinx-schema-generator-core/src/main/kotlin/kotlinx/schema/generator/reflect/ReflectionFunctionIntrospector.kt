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
        return TypeGraph(root = rootRef, nodes = context.nodes)
    }

    /**
     * Maintains state during function introspection including discovered nodes,
     * visited classes, and type reference cache.
     */
    private class IntrospectionContext : ReflectionIntrospectionContext() {
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
            // Try to extract default values by creating an instance
            val defaultValues = DefaultValueExtractor.extractDefaultValues(klass)

            // Extract properties from primary constructor using shared method
            val (properties, requiredProperties) = extractConstructorProperties(klass, defaultValues)

            return ObjectNode(
                name = klass.simpleName ?: "UnknownClass",
                properties = properties,
                required = requiredProperties,
                description = extractDescription(klass.annotations),
            )
        }
    }
}
