package kotlinx.schema.ksp.ir

import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.Nullability
import kotlinx.schema.generator.core.InternalSchemaGeneratorApi
import kotlinx.schema.generator.core.ir.BaseIntrospectionContext
import kotlinx.schema.generator.core.ir.TypeRef

/**
 * Shared introspection context for KSP-based introspectors.
 *
 * Eliminates toRef() duplication between KspClassIntrospector and KspFunctionIntrospector
 * by providing a single, well-tested implementation of the type resolution strategy.
 *
 * Extends [BaseIntrospectionContext] to inherit state management and cycle detection,
 * while implementing KSP-specific type resolution logic.
 *
 * Resolution strategy (applied in order):
 * 1. Basic types (primitives and collections) via [resolveBasicTypeOrNull]
 * 2. Generic type parameters and unknowns -> kotlin.Any via [handleAnyFallback]
 * 3. Sealed class hierarchies -> PolymorphicNode via [handleSealedClass]
 * 4. Enum classes -> EnumNode via [handleEnum]
 * 5. Regular objects/classes -> ObjectNode via [handleObjectOrClass]
 */
@OptIn(InternalSchemaGeneratorApi::class)
internal class KspIntrospectionContext : BaseIntrospectionContext<KSClassDeclaration, KSType>() {
    /**
     * Exposes visiting declarations as a mutable set for handler functions.
     * KSP handlers are top-level functions that need direct mutable access.
     */
    internal val visiting: MutableSet<KSClassDeclaration>
        get() = visitingDeclarations

    /**
     * Converts a KSType to a TypeRef using the standard resolution strategy.
     *
     * This method implements the common type resolution pattern used across all KSP
     * introspectors. It tries each handler in priority order, using elvis operator
     * chain to return the first successful match.
     *
     * All types should be handled by one of the resolution steps. If not, an exception
     * is thrown to fail fast and help identify missing handler cases during development.
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
                ?: handleAnyFallback(type, _nodes)
                ?: handleSealedClass(type, nullable, _nodes, visiting, ::toRef)
                ?: handleEnum(type, nullable, _nodes, visiting)
                ?: handleObjectOrClass(type, nullable, _nodes, visiting, ::toRef),
        ) {
            "Unexpected type that couldn't be handled: ${type.declaration.qualifiedName}"
        }
    }
}
