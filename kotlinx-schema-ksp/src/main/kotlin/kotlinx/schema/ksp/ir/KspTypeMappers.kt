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
            "kotlin.collections.Iterable",
            "kotlin.collections.MutableIterable",
            "kotlin.collections.Collection",
            "kotlin.collections.MutableCollection",
            "kotlin.collections.List",
            "kotlin.collections.MutableList",
            "kotlin.collections.Set",
            "kotlin.collections.MutableSet",
            "kotlin.Array",
            "kotlin.BooleanArray",
            "kotlin.ByteArray",
            "kotlin.ShortArray",
            "kotlin.IntArray",
            "kotlin.LongArray",
            "kotlin.FloatArray",
            "kotlin.DoubleArray",
            "kotlin.CharArray",
            -> {
                listOrSetTypeRef(type, nullable, recursiveMapper)
            }

            "kotlin.collections.Map",
            "kotlin.collections.MutableMap",
            -> {
                mapTypeRef(type, nullable, recursiveMapper)
            }

            else -> {
                null
            }
        }
    }

    /**
     * Creates TypeRef for List/Set/Array collections.
     */
    private fun listOrSetTypeRef(
        type: KSType,
        nullable: Boolean,
        recursiveMapper: (KSType) -> TypeRef,
    ): TypeRef {
        val qn = type.declaration.qualifiedName?.asString()
        val primitiveElemKind =
            when (qn) {
                "kotlin.BooleanArray" -> PrimitiveKind.BOOLEAN
                "kotlin.ByteArray", "kotlin.ShortArray", "kotlin.IntArray" -> PrimitiveKind.INT
                "kotlin.LongArray" -> PrimitiveKind.LONG
                "kotlin.FloatArray" -> PrimitiveKind.FLOAT
                "kotlin.DoubleArray" -> PrimitiveKind.DOUBLE
                "kotlin.CharArray" -> PrimitiveKind.STRING
                else -> null
            }

        val elementRef =
            if (primitiveElemKind != null) {
                TypeRef.Inline(PrimitiveNode(primitiveElemKind))
            } else {
                val elem =
                    type.arguments
                        .firstOrNull()
                        ?.type
                        ?.resolve()
                if (elem != null) {
                    recursiveMapper(elem)
                } else {
                    TypeRef.Inline(PrimitiveNode(PrimitiveKind.STRING))
                }
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
        val keyType =
            type.arguments
                .getOrNull(0)
                ?.type
                ?.resolve()
        val valueType =
            type.arguments
                .getOrNull(1)
                ?.type
                ?.resolve()

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
}
