package kotlinx.schema.ksp.ir

import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.Nullability
import kotlinx.schema.generator.core.ir.ObjectNode
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
 * - Complex parameter types (data classes, enums, sealed classes)
 *
 * Does NOT support:
 * - Lambda parameters with complex signatures
 */
internal class KspFunctionIntrospector : SchemaIntrospector<KSFunctionDeclaration> {
    override fun introspect(root: KSFunctionDeclaration): TypeGraph {
        val nodes = LinkedHashMap<TypeId, TypeNode>()
        val visiting = HashSet<com.google.devtools.ksp.symbol.KSClassDeclaration>()

        /**
         * Converts a KSType to a TypeRef using the same resolution strategy as KspClassIntrospector.
         *
         * Resolution strategy:
         * 1. Basic types (primitives and collections) via [resolveBasicTypeOrNull]
         * 2. Generic type parameters and unknowns -> kotlin.Any via [handleAnyFallback]
         * 3. Sealed class hierarchies -> PolymorphicNode via [handleSealedClass]
         * 4. Enum classes -> EnumNode via [handleEnum]
         * 5. Regular objects/classes -> ObjectNode via [handleObjectOrClass]
         *
         * All types should be handled by one of the above handlers. If not, an exception is thrown
         * to fail fast and help identify missing handler cases during development.
         *
         * @param type The KSType to convert
         * @return TypeRef representing the type in the schema IR
         * @throws IllegalArgumentException if the type cannot be handled by any handler
         */
        fun toRef(type: KSType): TypeRef {
            val nullable = type.nullability == Nullability.NULLABLE

            // Try each handler in order, using elvis operator chain for single return
            return requireNotNull(
                resolveBasicTypeOrNull(type, ::toRef)
                    ?: handleAnyFallback(type, nodes)
                    ?: handleSealedClass(type, nullable, nodes, visiting, ::toRef)
                    ?: handleEnum(type, nullable, nodes, visiting)
                    ?: handleObjectOrClass(type, nullable, nodes, visiting, ::toRef),
            ) {
                "Unexpected type that couldn't be handled: ${type.declaration.qualifiedName}"
            }
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
            val description = extractDescription(param) { null }

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

        nodes[id] = objectNode
        return TypeGraph(root = TypeRef.Ref(id, nullable = false), nodes = nodes)
    }
}
