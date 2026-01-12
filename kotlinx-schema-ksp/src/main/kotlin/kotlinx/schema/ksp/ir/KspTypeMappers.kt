package kotlinx.schema.ksp.ir

import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.Nullability
import kotlinx.schema.generator.core.ir.ListNode
import kotlinx.schema.generator.core.ir.MapNode
import kotlinx.schema.generator.core.ir.PrimitiveKind
import kotlinx.schema.generator.core.ir.PrimitiveNode
import kotlinx.schema.generator.core.ir.TypeRef

/**
 * Shared type mapping utilities for KSP introspectors.
 *
 * This object provides common type conversion logic used by both
 * KspClassIntrospector and KspFunctionIntrospector to avoid code duplication.
 */
internal object KspTypeMappers {
    /**
     * Maps a Kotlin primitive type to a PrimitiveNode, or returns null if not a primitive.
     *
     * Supported primitives:
     * - String → STRING
     * - Boolean → BOOLEAN
     * - Int, Byte, Short → INT
     * - Long → LONG
     * - Float → FLOAT
     * - Double → DOUBLE
     */
    fun primitiveFor(type: KSType): PrimitiveNode? {
        val qn = type.declaration.qualifiedName?.asString()
        return when (qn) {
            "kotlin.String" -> PrimitiveNode(PrimitiveKind.STRING)
            "kotlin.Boolean" -> PrimitiveNode(PrimitiveKind.BOOLEAN)
            "kotlin.Int", "kotlin.Byte", "kotlin.Short" -> PrimitiveNode(PrimitiveKind.INT)
            "kotlin.Long" -> PrimitiveNode(PrimitiveKind.LONG)
            "kotlin.Float" -> PrimitiveNode(PrimitiveKind.FLOAT)
            "kotlin.Double" -> PrimitiveNode(PrimitiveKind.DOUBLE)
            else -> null
        }
    }

    /**
     * Attempts to map collection types (List, Set, Map, Array) to TypeRef.
     *
     * Returns null if the type is not a recognized collection type.
     *
     * @param type The KSType to check
     * @param recursiveMapper A function to recursively resolve element/key/value types
     * @return TypeRef if this is a collection, null otherwise
     */
    fun collectionTypeRefOrNull(
        type: KSType,
        recursiveMapper: (KSType) -> TypeRef,
    ): TypeRef? {
        val nullable = type.nullability == Nullability.NULLABLE
        val qn = type.declaration.qualifiedName?.asString() ?: return null

        return when (qn) {
            "kotlin.collections.List", "kotlin.collections.Set" -> {
                listOrSetTypeRef(type, nullable, recursiveMapper)
            }

            "kotlin.collections.Map" -> {
                mapTypeRef(type, nullable, recursiveMapper)
            }

            "kotlin.Array" -> {
                arrayTypeRef(type, nullable, recursiveMapper)
            }

            else -> null
        }
    }

    /**
     * Creates TypeRef for List/Set collections.
     */
    private fun listOrSetTypeRef(
        type: KSType,
        nullable: Boolean,
        recursiveMapper: (KSType) -> TypeRef,
    ): TypeRef {
        val elem = type.arguments.firstOrNull()?.type?.resolve()
        val elementRef =
            if (elem != null) {
                recursiveMapper(elem)
            } else {
                TypeRef.Inline(PrimitiveNode(PrimitiveKind.STRING))
            }
        return TypeRef.Inline(ListNode(element = elementRef), nullable)
    }

    /**
     * Creates TypeRef for Map collections.
     */
    private fun mapTypeRef(
        type: KSType,
        nullable: Boolean,
        recursiveMapper: (KSType) -> TypeRef,
    ): TypeRef {
        val keyType = type.arguments.getOrNull(0)?.type?.resolve()
        val valueType = type.arguments.getOrNull(1)?.type?.resolve()

        val keyRef =
            if (keyType != null) {
                recursiveMapper(keyType)
            } else {
                TypeRef.Inline(PrimitiveNode(PrimitiveKind.STRING))
            }

        val valueRef =
            if (valueType != null) {
                recursiveMapper(valueType)
            } else {
                TypeRef.Inline(PrimitiveNode(PrimitiveKind.STRING))
            }

        return TypeRef.Inline(MapNode(key = keyRef, value = valueRef), nullable)
    }

    /**
     * Creates TypeRef for Array collections.
     */
    private fun arrayTypeRef(
        type: KSType,
        nullable: Boolean,
        recursiveMapper: (KSType) -> TypeRef,
    ): TypeRef {
        val elem = type.arguments.firstOrNull()?.type?.resolve()
        val elementRef =
            if (elem != null) {
                recursiveMapper(elem)
            } else {
                TypeRef.Inline(PrimitiveNode(PrimitiveKind.STRING))
            }
        return TypeRef.Inline(ListNode(element = elementRef), nullable)
    }
}
