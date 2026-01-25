package kotlinx.schema.generator.json

import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import kotlinx.schema.json.ArrayPropertyDefinition
import kotlinx.schema.json.BooleanPropertyDefinition
import kotlinx.schema.json.NumericPropertyDefinition
import kotlinx.schema.json.ObjectPropertyDefinition
import kotlinx.schema.json.StringPropertyDefinition
import kotlin.test.Test

/**
 * Tests for JsonSchemaConfig options.
 * Focuses on configuration-specific behavior.
 */
class JsonSchemaConfigTest {
    @Test
    fun `treatNullableOptionalAsRequired true should add null to type array`() {
        val config = JsonSchemaTransformerConfig(treatNullableOptionalAsRequired = true)
        val transformer = TypeGraphToJsonSchemaTransformer(config)
        val typeGraph =
            kotlinx.schema.generator.reflect.ReflectionIntrospector
                .introspect(PersonWithOptionals::class)
        val schema = transformer.transform(typeGraph, "PersonWithOptionals")

        val properties = schema.schema.properties
        val required = schema.schema.required

        // All properties should be required
        required.size shouldBe 5
        required shouldContainAll listOf("name", "age", "email", "score", "active")

        // Nullable optional fields should have ["type", "null"] in type array
        val ageProperty = properties["age"] as NumericPropertyDefinition
        ageProperty.type shouldBe listOf("integer", "null")
        ageProperty.nullable.shouldBeNull()

        val emailProperty = properties["email"] as StringPropertyDefinition
        emailProperty.type shouldBe listOf("string", "null")
        emailProperty.nullable.shouldBeNull()

        val scoreProperty = properties["score"] as NumericPropertyDefinition
        scoreProperty.type shouldBe listOf("number", "null")
        scoreProperty.nullable.shouldBeNull()
    }

    @Test
    fun `treatNullableOptionalAsRequired false should not include optionals in required`() {
        val config = JsonSchemaTransformerConfig(treatNullableOptionalAsRequired = false)
        val transformer = TypeGraphToJsonSchemaTransformer(config)
        val typeGraph =
            kotlinx.schema.generator.reflect.ReflectionIntrospector
                .introspect(PersonWithOptionals::class)
        val schema = transformer.transform(typeGraph, "PersonWithOptionals")

        val required = schema.schema.required

        // Only required properties (no defaults) should be in required list
        required.size shouldBe 1
        required shouldContainAll listOf("name")
    }

    @Test
    fun `treatNullableOptionalAsRequired should handle all property types`() {
        val config = JsonSchemaTransformerConfig(treatNullableOptionalAsRequired = true)
        val transformer = TypeGraphToJsonSchemaTransformer(config)
        val typeGraph =
            kotlinx.schema.generator.reflect.ReflectionIntrospector
                .introspect(AllTypesOptional::class)
        val schema = transformer.transform(typeGraph, "AllTypesOptional")

        val properties = schema.schema.properties
        val required = schema.schema.required

        // All should be required
        required.size shouldBe 9

        // Check type arrays include null for all nullable types
        val numProperty = properties["num"] as NumericPropertyDefinition
        numProperty.type shouldBe listOf("integer", "null")

        val longProperty = properties["longNum"] as NumericPropertyDefinition
        longProperty.type shouldBe listOf("integer", "null")

        val floatProperty = properties["floatNum"] as NumericPropertyDefinition
        floatProperty.type shouldBe listOf("number", "null")

        val doubleProperty = properties["doubleNum"] as NumericPropertyDefinition
        doubleProperty.type shouldBe listOf("number", "null")

        val boolProperty = properties["flag"] as BooleanPropertyDefinition
        boolProperty.type shouldBe listOf("boolean", "null")

        val listProperty = properties["items"] as ArrayPropertyDefinition
        listProperty.type shouldBe listOf("array", "null")

        val mapProperty = properties["data"] as ObjectPropertyDefinition
        mapProperty.type shouldBe listOf("object", "null")

        val nestedProperty = properties["nested"] as ObjectPropertyDefinition
        nestedProperty.type shouldBe listOf("object", "null")
    }

    @Test
    fun `Default config should have correct defaults`() {
        val config = JsonSchemaTransformerConfig.Default
        config.treatNullableOptionalAsRequired shouldBe false
        config.requiredFieldStrategy shouldBe RequiredFieldStrategy.ALL_REQUIRED
    }
}
