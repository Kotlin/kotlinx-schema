package kotlinx.schema.generator.json

import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import kotlinx.schema.generator.core.SchemaGeneratorService
import kotlinx.schema.json.ArrayPropertyDefinition
import kotlinx.schema.json.JsonSchema
import kotlinx.schema.json.NumericPropertyDefinition
import kotlinx.schema.json.ObjectPropertyDefinition
import kotlinx.schema.json.StringPropertyDefinition
import kotlin.reflect.KClass
import kotlin.test.Test

class JsonSchemaTypesTest {
    private val generator =
        requireNotNull(
            SchemaGeneratorService.getGenerator(
                KClass::class,
                JsonSchema::class,
            ),
        )

    // Primitive Types Tests

    @Test
    fun `Should handle all numeric types correctly`() {
        val jsonSchema = generator.generateSchema(WithNumericTypes::class)
        val schema = jsonSchema.schema
        val properties = schema.properties

        // Non-nullable integer types
        schema.numericProperty("intVal") shouldNotBeNull {
            type shouldBe listOf("integer")
            nullable.shouldBeNull()
        }

        val longProperty = properties["longVal"] as NumericPropertyDefinition
        longProperty.type shouldBe listOf("integer")
        longProperty.nullable.shouldBeNull()

        // Non-nullable floating point types
        val floatProperty = properties["floatVal"] as NumericPropertyDefinition
        floatProperty.type shouldBe listOf("number")
        floatProperty.nullable.shouldBeNull()

        val doubleProperty = properties["doubleVal"] as NumericPropertyDefinition
        doubleProperty.type shouldBe listOf("number")
        doubleProperty.nullable.shouldBeNull()

        // Nullable integer types
        val nullableIntProperty = properties["nullableInt"] as NumericPropertyDefinition
        nullableIntProperty.type shouldBe listOf("integer")
        nullableIntProperty.nullable shouldBe true

        val nullableLongProperty = properties["nullableLong"] as NumericPropertyDefinition
        nullableLongProperty.type shouldBe listOf("integer")
        nullableLongProperty.nullable shouldBe true

        // Nullable floating point types
        val nullableFloatProperty = properties["nullableFloat"] as NumericPropertyDefinition
        nullableFloatProperty.type shouldBe listOf("number")
        nullableFloatProperty.nullable shouldBe true

        val nullableDoubleProperty = properties["nullableDouble"] as NumericPropertyDefinition
        nullableDoubleProperty.type shouldBe listOf("number")
        nullableDoubleProperty.nullable shouldBe true
    }

    @Test
    fun `Should handle enum types`() {
        val schema = generator.generateSchema(WithEnum::class)
        val properties = schema.schema.properties

        val statusProperty = properties["status"] as StringPropertyDefinition
        statusProperty.type shouldBe listOf("string")
        statusProperty.nullable.shouldBeNull()
        statusProperty.enum shouldNotBeNull {
            size shouldBe 3
            this shouldContainAll
                listOf(
                    "ACTIVE",
                    "INACTIVE",
                    "PENDING",
                )
        }

        val optStatusProperty = properties["optStatus"] as StringPropertyDefinition
        optStatusProperty.type shouldBe listOf("string")
        optStatusProperty.nullable shouldBe true
        optStatusProperty.enum.shouldNotBeNull()
    }

    // Collection Types Tests

    @Test
    fun `Should handle collection types`() {
        val schema = generator.generateSchema(WithCollections::class)
        val properties = schema.schema.properties

        val itemsProperty = properties["items"] as ArrayPropertyDefinition
        itemsProperty.type shouldBe listOf("array")
        itemsProperty.nullable.shouldBeNull()
        itemsProperty.items.shouldNotBeNull()

        val dataProperty = properties["data"] as ObjectPropertyDefinition
        dataProperty.type shouldBe listOf("object")
        dataProperty.nullable.shouldBeNull()
        dataProperty.additionalProperties.shouldNotBeNull()

        val optListProperty = properties["optList"] as ArrayPropertyDefinition
        optListProperty.type shouldBe listOf("array")
        optListProperty.nullable shouldBe true

        val optMapProperty = properties["optMap"] as ObjectPropertyDefinition
        optMapProperty.type shouldBe listOf("object")
        optMapProperty.nullable shouldBe true
    }

    @Test
    fun `Should handle list of nested objects`() {
        val schema = generator.generateSchema(ListOfNested::class)
        val properties = schema.schema.properties

        val itemsProperty = properties["items"] as ArrayPropertyDefinition
        itemsProperty.items.shouldNotBeNull()
        val itemType = itemsProperty.items as ObjectPropertyDefinition
        itemType.type shouldBe listOf("object")

        val optionalItemsProperty = properties["optionalItems"] as ArrayPropertyDefinition
        optionalItemsProperty.nullable shouldBe true
    }

    @Test
    fun `Should handle map of nested objects`() {
        val schema = generator.generateSchema(MapOfNested::class)
        val properties = schema.schema.properties

        val dataProperty = properties["data"] as ObjectPropertyDefinition
        dataProperty.additionalProperties.shouldNotBeNull()

        val optionalDataProperty = properties["optionalData"] as ObjectPropertyDefinition
        optionalDataProperty.nullable shouldBe true
    }

    // Nested Object Tests

    @Test
    fun `Should handle nested objects`() {
        val schema = generator.generateSchema(WithNested::class)
        val properties = schema.schema.properties

        properties.size shouldBe 3

        val addressProperty = properties["address"] as ObjectPropertyDefinition
        addressProperty.type shouldBe listOf("object")
        addressProperty.nullable.shouldBeNull()
        addressProperty.properties.shouldNotBeNull()
        addressProperty.properties!!.size shouldBe 2

        val optAddressProperty = properties["optAddress"] as ObjectPropertyDefinition
        optAddressProperty.type shouldBe listOf("object")
        optAddressProperty.nullable shouldBe true
    }

    @Test
    fun `Should handle deeply nested structures`() {
        val schema = generator.generateSchema(DeepNested::class)
        val properties = schema.schema.properties

        val level1Property = properties["level1"] as ObjectPropertyDefinition
        level1Property.properties.shouldNotBeNull()
        level1Property.properties!!.size shouldBe 2

        val level2Property = level1Property.properties!!["level2"] as ObjectPropertyDefinition
        level2Property.properties.shouldNotBeNull()
        level2Property.properties!!.size shouldBe 3

        val level3Property = level2Property.properties!!["level3"] as ObjectPropertyDefinition
        level3Property.properties.shouldNotBeNull()
        level3Property.properties!!.size shouldBe 1
    }

    // Required Fields Tests

    @Test
    fun `Should correctly distinguish required vs optional vs default`() {
        val schema = generator.generateSchema(MixedRequiredOptional::class)
        val required = schema.schema.required

        // Only truly required (no defaults) should be in required list
        required.size shouldBe 2
        required.toSet() shouldBe setOf("req1", "req2")
    }

    @Test
    fun `Should handle class with all optional fields`() {
        val schema = generator.generateSchema(EmptyClass::class)
        val required = schema.schema.required

        // No required fields
        required.size shouldBe 0
    }

    @Test
    fun `Should handle class with single required field`() {
        val schema = generator.generateSchema(SingleRequired::class)
        val required = schema.schema.required

        required.size shouldBe 1
        required[0] shouldBe "value"
    }

    // Description Preservation Tests

    @Test
    fun `Should preserve descriptions through transformations`() {
        val schema = generator.generateSchema(MixedRequiredOptional::class)
        val properties = schema.schema.properties

        val req1Property = properties["req1"] as StringPropertyDefinition
        req1Property.description shouldBe "Required string"

        val opt1Property = properties["opt1"] as StringPropertyDefinition
        opt1Property.description shouldBe "Optional string"

        val def1Property = properties["def1"] as StringPropertyDefinition
        def1Property.description shouldBe "Default string"
    }

    // Nullable Optional Fields Tests

    @Test
    fun `Should handle nullable optional fields with default config`() {
        val schema = generator.generateSchema(PersonWithOptionals::class)
        val properties = schema.schema.properties
        val required = schema.schema.required

        // Only required properties (no defaults) should be in required list
        required.size shouldBe 1
        required shouldContainAll listOf("name")

        // Nullable optional fields should have nullable=true
        val ageProperty = properties["age"] as NumericPropertyDefinition
        ageProperty.type shouldBe listOf("integer")
        ageProperty.nullable shouldBe true

        val emailProperty = properties["email"] as StringPropertyDefinition
        emailProperty.type shouldBe listOf("string")
        emailProperty.nullable shouldBe true
    }
}
