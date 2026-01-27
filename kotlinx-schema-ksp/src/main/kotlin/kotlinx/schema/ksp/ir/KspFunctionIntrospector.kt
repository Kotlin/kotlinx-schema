package kotlinx.schema.ksp.ir

import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.Nullability
import kotlinx.schema.generator.core.ir.ObjectNode
import kotlinx.schema.generator.core.ir.PrimitiveKind
import kotlinx.schema.generator.core.ir.PrimitiveNode
import kotlinx.schema.generator.core.ir.Property
import kotlinx.schema.generator.core.ir.SchemaIntrospector
import kotlinx.schema.generator.core.ir.TypeGraph
import kotlinx.schema.generator.core.ir.TypeId
import kotlinx.schema.generator.core.ir.TypeNode
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
 *
 * Does NOT support:
 * - Lambda parameters with complex signatures
 */
@Suppress("ReturnCount", "MaxLineLength", "NestedBlockDepth", "LongMethod", "CyclomaticComplexMethod")
internal class KspFunctionIntrospector : SchemaIntrospector<KSFunctionDeclaration> {
    override fun introspect(root: KSFunctionDeclaration): TypeGraph {
        val nodes = LinkedHashMap<TypeId, TypeNode>()

        fun toRef(type: KSType): TypeRef {
            val nullable = type.nullability == Nullability.NULLABLE

            // Try primitive types first
            KspTypeMappers.primitiveFor(type)?.let { prim ->
                return TypeRef.Inline(prim, nullable)
            }

            // Try collection types (List, Set, Map, Array)
            KspTypeMappers.collectionTypeRefOrNull(type, ::toRef)?.let { collectionRef ->
                return collectionRef
            }

            // For complex types (data classes, enums, etc.), use simplified approach
            // Function schemas inline complex types as strings for simplicity
            return TypeRef.Inline(PrimitiveNode(PrimitiveKind.STRING), nullable)
        }

        // Extract function information
        val functionName = root.simpleName.asString()
        val id = TypeId(functionName)

        val properties = mutableListOf<Property>()
        val requiredProperties = mutableSetOf<String>()

        // Process function parameters
        root.parameters.forEach { param ->
            val paramName = param.name?.asString() ?: return@forEach
            val paramType = param.type.resolve()

            val typeRef = toRef(paramType)

            // Extract description from annotations or KDoc
            val description =
                param.annotations.firstNotNullOfOrNull { it.descriptionOrNull() }

            // KSP limitation: hasDefault doesn't reliably detect default values in the same compilation unit
            // For function calling schemas, all parameters are marked as required by default (including nullable)
            // Nullable types are represented with union types: ["string", "null"]
            properties +=
                Property(
                    name = paramName,
                    type = typeRef,
                    description = description,
                    hasDefaultValue = false,
                    defaultValue = null, // KSP cannot extract default values at compile-time
                )

            requiredProperties += paramName
        }

        // Extract function description
        val functionDescription =
            root.annotations.firstNotNullOfOrNull { it.descriptionOrNull() }
                ?: root.descriptionFromKdoc()

        val objectNode =
            ObjectNode(
                name = functionName,
                properties = properties,
                required = requiredProperties,
                description = functionDescription,
            )

        nodes[id] = objectNode
        return TypeGraph(root = TypeRef.Ref(id, nullable = false), nodes = nodes)
    }
}
