package kotlinx.schema.ksp.ir

import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import kotlinx.schema.generator.core.ir.ObjectNode
import kotlinx.schema.generator.core.ir.Property
import kotlinx.schema.generator.core.ir.SchemaIntrospector
import kotlinx.schema.generator.core.ir.TypeGraph
import kotlinx.schema.generator.core.ir.TypeId
import kotlinx.schema.generator.core.ir.TypeRef

/**
 * KSP-backed function introspector that converts function declarations to TypeGraph.
 *
 * This introspector analyzes function parameters using KSP and builds a TypeGraph
 * representing the function's parameter schema suitable for function calling schemas.
 *
 * Supports:
 * - Regular functions
 * - Suspend functions (treated as regular functions)
 * - Extension functions (receiver type handled separately)
 * - Generic functions (using star projection)
 * - Complex parameter types (data classes, enums, sealed classes)
 *
 * Does NOT support:
 * - Lambda parameters with complex signatures
 */
internal class KspFunctionIntrospector : SchemaIntrospector<KSFunctionDeclaration, Unit> {
    override val config = Unit

    override fun introspect(root: KSFunctionDeclaration): TypeGraph {
        val context = KspIntrospectionContext()

        // Extract function information
        val functionName = root.simpleName.asString()
        val id = TypeId(functionName)

        val properties = mutableListOf<Property>()
        val requiredProperties = mutableSetOf<String>()

        // Process function parameters
        root.parameters.forEach { param ->
            val paramName = param.name?.asString() ?: return@forEach
            val paramType = param.type.resolve()

            val typeRef = context.toRef(paramType)

            // Extract description from annotations, then fall back to function KDoc @param tag
            val description =
                extractPropertyDescription(
                    annotated = param,
                    propertyName = paramName,
                    parentKdoc = root.docString,
                    kdocTagName = "param",
                    elementKdocFallback = { null },
                )

            // KSP limitation: hasDefault doesn't reliably detect default values in the same compilation unit
            // For function calling schemas, all parameters are marked as required by default (including nullable)
            // Nullable types are represented with union types: ["string", "null"]
            properties +=
                createProperty(
                    name = paramName,
                    type = typeRef,
                    description = description,
                    hasDefaultValue = false,
                )

            requiredProperties += paramName
        }

        // Extract function description
        val functionDescription = extractDescription(root) { root.descriptionFromKdoc() }

        val objectNode =
            ObjectNode(
                name = functionName,
                properties = properties,
                required = requiredProperties,
                description = functionDescription,
            )

        context.nodes[id] = objectNode
        return TypeGraph(root = TypeRef.Ref(id, nullable = false), nodes = context.nodes)
    }
}
