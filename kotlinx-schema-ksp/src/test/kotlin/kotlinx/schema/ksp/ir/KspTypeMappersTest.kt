package kotlinx.schema.ksp.ir

import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSName
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.KSTypeArgument
import com.google.devtools.ksp.symbol.KSTypeReference
import com.google.devtools.ksp.symbol.Nullability
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.every
import io.mockk.mockk
import kotlinx.schema.generator.core.ir.ListNode
import kotlinx.schema.generator.core.ir.MapNode
import kotlinx.schema.generator.core.ir.PrimitiveKind
import kotlinx.schema.generator.core.ir.PrimitiveNode
import kotlinx.schema.generator.core.ir.TypeRef
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource

/**
 * Unit tests for KspTypeMappers utility functions.
 */
class KspTypeMappersTest {
    @ParameterizedTest(name = "{0} should map to {1}")
    @CsvSource(
        "kotlin.String, STRING",
        "kotlin.Boolean, BOOLEAN",
        "kotlin.Int, INT",
        "kotlin.Byte, INT",
        "kotlin.Short, INT",
        "kotlin.Long, LONG",
        "kotlin.Float, FLOAT",
        "kotlin.Double, DOUBLE",
    )
    fun `primitiveFor maps Kotlin primitives to PrimitiveKind`(
        qualifiedName: String,
        expectedKind: String,
    ) {
        // Given
        val type = mockKSType(qualifiedName, Nullability.NOT_NULL)

        // When
        val result = KspTypeMappers.primitiveFor(type)

        // Then
        result.shouldBeInstanceOf<PrimitiveNode>()
        result.kind shouldBe PrimitiveKind.valueOf(expectedKind)
    }

    @Test
    fun `primitiveFor returns null for unknown type`() {
        // Given
        val type = mockKSType("com.example.CustomType", Nullability.NOT_NULL)

        // When
        val result = KspTypeMappers.primitiveFor(type)

        // Then
        result.shouldBeNull()
    }

    @Test
    fun `primitiveFor returns null when qualified name is null`() {
        // Given
        val type =
            mockk<KSType> {
                every { declaration.qualifiedName } returns null
            }

        // When
        val result = KspTypeMappers.primitiveFor(type)

        // Then
        result.shouldBeNull()
    }

    @ParameterizedTest(name = "{0} should map to ListNode")
    @ValueSource(
        strings = [
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
        ],
    )
    fun `collectionTypeRefOrNull maps list-like collections to ListNode`(qualifiedName: String) {
        // Given
        val elementType = mockKSType("kotlin.String", Nullability.NOT_NULL)
        val collectionType = mockKSType(qualifiedName, typeArgs = listOf(elementType))

        // When
        val result =
            KspTypeMappers.collectionTypeRefOrNull(collectionType) { type ->
                // Recursive mapper for element type
                TypeRef.Inline(KspTypeMappers.primitiveFor(type)!!)
            }

        // Then
        result.shouldBeInstanceOf<TypeRef.Inline>()
        result.node.shouldBeInstanceOf<ListNode>()
        result.nullable shouldBe false

        val listNode = result.node as ListNode
        listNode.element.shouldBeInstanceOf<TypeRef.Inline>()
        (listNode.element as TypeRef.Inline).node.shouldBeInstanceOf<PrimitiveNode>()
    }

    @ParameterizedTest(name = "{0} should map to MapNode")
    @CsvSource(
        "kotlin.collections.Map",
        "kotlin.collections.MutableMap",
    )
    fun `collectionTypeRefOrNull maps Map types to MapNode with key and value types`(qualifiedName: String) {
        // Given
        val keyType = mockKSType("kotlin.String", Nullability.NOT_NULL)
        val valueType = mockKSType("kotlin.Int", Nullability.NOT_NULL)
        val mapType = mockKSType(qualifiedName, typeArgs = listOf(keyType, valueType))

        // When
        val result =
            KspTypeMappers.collectionTypeRefOrNull(mapType) { type ->
                TypeRef.Inline(KspTypeMappers.primitiveFor(type)!!)
            }

        // Then
        result.shouldBeInstanceOf<TypeRef.Inline>()
        result.node.shouldBeInstanceOf<MapNode>()
        result.nullable shouldBe false

        val mapNode = result.node as MapNode
        mapNode.key.shouldBeInstanceOf<TypeRef.Inline>()
        mapNode.value.shouldBeInstanceOf<TypeRef.Inline>()
    }

    @Test
    fun `collectionTypeRefOrNull handles nullable collections`() {
        // Given
        val elementType = mockKSType("kotlin.String", Nullability.NOT_NULL)
        val nullableList = mockKSType("kotlin.collections.List", Nullability.NULLABLE, listOf(elementType))

        // When
        val result =
            KspTypeMappers.collectionTypeRefOrNull(nullableList) { type ->
                TypeRef.Inline(KspTypeMappers.primitiveFor(type)!!)
            }

        // Then
        result.shouldBeInstanceOf<TypeRef.Inline>()
        result.nullable shouldBe true
    }

    @Test
    fun `collectionTypeRefOrNull defaults to String element when type argument missing`() {
        // Given
        val listWithoutTypeArg = mockKSType("kotlin.collections.List")

        // When
        val result =
            KspTypeMappers.collectionTypeRefOrNull(listWithoutTypeArg) { type ->
                TypeRef.Inline(KspTypeMappers.primitiveFor(type)!!)
            }

        // Then
        result.shouldBeInstanceOf<TypeRef.Inline>()
        val listNode = result.node as ListNode
        val elementNode = (listNode.element as TypeRef.Inline).node as PrimitiveNode
        elementNode.kind shouldBe PrimitiveKind.STRING
    }

    @Test
    fun `collectionTypeRefOrNull defaults Map to String key and value when type arguments missing`() {
        // Given
        val mapWithoutTypeArgs = mockKSType("kotlin.collections.Map")

        // When
        val result =
            KspTypeMappers.collectionTypeRefOrNull(mapWithoutTypeArgs) { type ->
                TypeRef.Inline(KspTypeMappers.primitiveFor(type)!!)
            }

        // Then
        result.shouldBeInstanceOf<TypeRef.Inline>()
        val mapNode = result.node as MapNode
        val keyNode = (mapNode.key as TypeRef.Inline).node as PrimitiveNode
        val valueNode = (mapNode.value as TypeRef.Inline).node as PrimitiveNode
        keyNode.kind shouldBe PrimitiveKind.STRING
        valueNode.kind shouldBe PrimitiveKind.STRING
    }

    @Test
    fun `collectionTypeRefOrNull returns null for non-collection types`() {
        // Given
        val customType = mockKSType("com.example.MyClass", Nullability.NOT_NULL)

        // When
        val result =
            KspTypeMappers.collectionTypeRefOrNull(customType) { _ ->
                TypeRef.Inline(PrimitiveNode(PrimitiveKind.STRING))
            }

        // Then
        result.shouldBeNull()
    }

    @Test
    fun `collectionTypeRefOrNull returns null when qualified name is null`() {
        // Given
        val type =
            mockk<KSType> {
                every { nullability } returns Nullability.NOT_NULL
                every { declaration.qualifiedName } returns null
            }

        // When
        val result =
            KspTypeMappers.collectionTypeRefOrNull(type) { _ ->
                TypeRef.Inline(PrimitiveNode(PrimitiveKind.STRING))
            }

        // Then
        result.shouldBeNull()
    }

    // Helper functions

    private fun mockKSType(
        qualifiedName: String,
        nullability: Nullability = Nullability.NOT_NULL,
        typeArgs: List<KSType> = emptyList(),
    ): KSType {
        val ksName =
            mockk<KSName> {
                every { asString() } returns qualifiedName
            }
        val declaration =
            mockk<KSDeclaration> {
                every { this@mockk.qualifiedName } returns ksName
            }
        val arguments =
            typeArgs.map { typeArg ->
                mockk<KSTypeArgument> {
                    every { type } returns
                        mockk<KSTypeReference> {
                            every { resolve() } returns typeArg
                        }
                }
            }
        return mockk {
            every { this@mockk.declaration } returns declaration
            every { this@mockk.nullability } returns nullability
            every { this@mockk.arguments } returns arguments
        }
    }
}
