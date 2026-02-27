package kotlinx.schema.generator.reflect

import kotlinx.schema.generator.core.ir.ObjectNode
import kotlinx.schema.generator.core.ir.Property
import kotlinx.schema.generator.core.ir.SchemaIntrospector
import kotlinx.schema.generator.core.ir.TypeGraph
import kotlinx.schema.generator.core.ir.TypeId
import kotlinx.schema.generator.core.ir.TypeRef
import kotlin.reflect.KCallable
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
public object ReflectionFunctionIntrospector : SchemaIntrospector<KCallable<*>, Unit> {
    override val config: Unit = Unit

    override fun introspect(root: KCallable<*>): TypeGraph {
        require(root.parameters.none { it.kind == KParameter.Kind.EXTENSION_RECEIVER }) {
            "Extension functions are not supported"
        }

        val context = ReflectionIntrospectionContext()

        // Extract function information
        val functionName = root.name
        val id = TypeId(functionName)

        // Create an ObjectNode representing the function parameters
        val properties = mutableListOf<Property>()
        val requiredProperties = mutableSetOf<String>()

        root.parameters.forEach { param ->
            // Skip instance parameter for member functions
            if (param.kind == KParameter.Kind.INSTANCE) return@forEach

            val paramName = param.name ?: return@forEach
            val paramType = param.type
            val hasDefault = param.isOptional

            val typeRef = context.toRef(paramType)

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
                description = extractDescription(root.annotations),
            )

        // Add an object generated from a function to the nodes
        val nodes = context.nodes + (id to objectNode)

        return TypeGraph(root = TypeRef.Ref(id, nullable = false), nodes = nodes)
    }
}
