package kotlinx.schema.generator.reflect

import kotlinx.schema.generator.core.ir.ObjectNode
import kotlinx.schema.generator.core.ir.Property
import kotlinx.schema.generator.core.ir.SchemaIntrospector
import kotlinx.schema.generator.core.ir.TypeGraph
import kotlinx.schema.generator.core.ir.TypeId
import kotlinx.schema.generator.core.ir.TypeRef
import kotlin.reflect.KCallable
import kotlin.reflect.KFunction
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

        // Find implemented methods if this function is an override
        val implementedMethods =
            (root as? KFunction<*>)
                ?.findImplementedMethods()
                .orEmpty()

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

            // Find all annotations on the same parameter in parent functions too
            val allParamAnnotations =
                param.annotations + implementedMethods
                        .map { method -> method.parameters.first { it.name == paramName } }
                        .flatMap { it.annotations }

            val description = extractDescription(allParamAnnotations)

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

        // Find all annotations on the function and on the parent functions too
        val allFunctionAnnotations = root.annotations + implementedMethods.flatMap { it.annotations }

        val objectNode =
            ObjectNode(
                name = functionName,
                properties = properties,
                required = requiredProperties,
                description = extractDescription(allFunctionAnnotations),
            )

        // Add an object generated from a function to the nodes
        val nodes = context.nodes + (id to objectNode)

        return TypeGraph(root = TypeRef.Ref(id, nullable = false), nodes = nodes)
    }
}
