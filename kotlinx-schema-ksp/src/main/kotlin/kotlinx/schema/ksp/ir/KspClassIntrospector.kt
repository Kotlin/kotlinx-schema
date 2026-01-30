package kotlinx.schema.ksp.ir

import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.Nullability
import kotlinx.schema.generator.core.ir.SchemaIntrospector
import kotlinx.schema.generator.core.ir.TypeGraph
import kotlinx.schema.generator.core.ir.TypeId
import kotlinx.schema.generator.core.ir.TypeNode
import kotlinx.schema.generator.core.ir.TypeRef

/**
 * KSP-backed Schema IR introspector. Focuses on classes and enums; generics use star-projection.
 */
internal class KspClassIntrospector : SchemaIntrospector<KSClassDeclaration> {
    @Suppress("CyclomaticComplexMethod")
    override fun introspect(root: KSClassDeclaration): TypeGraph {
        val nodes = LinkedHashMap<TypeId, TypeNode>()
        val visiting = HashSet<KSClassDeclaration>()

        /**
         * Converts a KSType to a TypeRef by attempting each handler in priority order.
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

        val rootRef = TypeRef.Ref(root.typeId())
        // ensure root node is populated
        toRef(root.asType(emptyList()))
        return TypeGraph(root = rootRef, nodes = nodes)
    }
}
