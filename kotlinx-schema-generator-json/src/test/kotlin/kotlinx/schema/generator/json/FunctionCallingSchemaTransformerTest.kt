package kotlinx.schema.generator.json

import io.kotest.matchers.maps.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import kotlinx.schema.Description
import kotlinx.schema.generator.core.SchemaGeneratorService
import kotlinx.schema.json.ArrayPropertyDefinition
import kotlinx.schema.json.BooleanPropertyDefinition
import kotlinx.schema.json.FunctionCallingSchema
import kotlinx.schema.json.NumericPropertyDefinition
import kotlinx.schema.json.ObjectPropertyDefinition
import kotlinx.schema.json.StringPropertyDefinition
import kotlin.reflect.KCallable
import kotlin.test.Test

/**
 * Comprehensive tests for TypeGraphToFunctionCallingSchemaTransformer.
 * Focuses on coverage of all code paths, nullable handling, and error cases.
 */
@Suppress("unused")
class FunctionCallingSchemaTransformerTest {
    private val generator =
        requireNotNull(
            SchemaGeneratorService.getGenerator(
                KCallable::class,
                FunctionCallingSchema::class,
            ),
        )

    // Nullable Types Tests

    object NullablePrimitives {
        @Description("Test nullable primitives")
        @Suppress("LongParameterList")
        fun testNullables(
            nullableString: String?,
            nullableInt: Int?,
            nullableLong: Long?,
            nullableFloat: Float?,
            nullableDouble: Double?,
            nullableBoolean: Boolean?,
        ) {
            // No-op
        }
    }

    @Test
    fun `Should handle nullable primitive types with null in type array`() {
        val schema = generator.generateSchema(NullablePrimitives::testNullables)

        val properties = schema.parameters.properties.shouldNotBeNull()

        val nullableString = properties["nullableString"] as StringPropertyDefinition
        nullableString.type shouldBe listOf("string", "null")
        nullableString.nullable.shouldBeNull()

        val nullableInt = properties["nullableInt"] as NumericPropertyDefinition
        nullableInt.type shouldBe listOf("integer", "null")
        nullableInt.nullable.shouldBeNull()

        val nullableLong = properties["nullableLong"] as NumericPropertyDefinition
        nullableLong.type shouldBe listOf("integer", "null")
        nullableLong.nullable.shouldBeNull()

        val nullableFloat = properties["nullableFloat"] as NumericPropertyDefinition
        nullableFloat.type shouldBe listOf("number", "null")
        nullableFloat.nullable.shouldBeNull()

        val nullableDouble = properties["nullableDouble"] as NumericPropertyDefinition
        nullableDouble.type shouldBe listOf("number", "null")
        nullableDouble.nullable.shouldBeNull()

        val nullableBoolean = properties["nullableBoolean"] as BooleanPropertyDefinition
        nullableBoolean.type shouldBe listOf("boolean", "null")
        nullableBoolean.nullable.shouldBeNull()
    }

    // Non-Nullable Types Tests

    object NonNullablePrimitives {
        @Description("Test non-nullable primitives")
        @Suppress("LongParameterList")
        fun testNonNullables(
            string: String,
            int: Int,
            long: Long,
            float: Float,
            double: Double,
            boolean: Boolean,
        ) {
            // No-op
        }
    }

    @Test
    fun `Should handle non-nullable primitive types without null in type array`() {
        val schema = generator.generateSchema(NonNullablePrimitives::testNonNullables)

        val properties = schema.parameters.properties.shouldNotBeNull()

        val string = properties["string"] as StringPropertyDefinition
        string.type shouldBe listOf("string")
        string.nullable.shouldBeNull()

        val int = properties["int"] as NumericPropertyDefinition
        int.type shouldBe listOf("integer")
        int.nullable.shouldBeNull()

        val long = properties["long"] as NumericPropertyDefinition
        long.type shouldBe listOf("integer")
        long.nullable.shouldBeNull()

        val float = properties["float"] as NumericPropertyDefinition
        float.type shouldBe listOf("number")
        float.nullable.shouldBeNull()

        val double = properties["double"] as NumericPropertyDefinition
        double.type shouldBe listOf("number")
        double.nullable.shouldBeNull()

        val boolean = properties["boolean"] as BooleanPropertyDefinition
        boolean.type shouldBe listOf("boolean")
        boolean.nullable.shouldBeNull()
    }

    // Nullable Complex Types Tests

    object NullableComplexTypes {
        data class Config(
            val host: String,
        )

        enum class LogLevel { DEBUG, INFO, ERROR }

        @Description("Test nullable complex types")
        fun testNullableComplex(
            nullableObject: Config?,
            nullableEnum: LogLevel?,
            nullableList: List<String>?,
            nullableMap: Map<String, Int>?,
        ) {
            // No-op
        }
    }

    @Test
    fun `Should handle nullable complex types`() {
        val schema = generator.generateSchema(NullableComplexTypes::testNullableComplex)

        val properties = schema.parameters.properties.shouldNotBeNull()

        val nullableObject = properties["nullableObject"] as ObjectPropertyDefinition
        nullableObject.type shouldBe listOf("object", "null")
        nullableObject.nullable.shouldBeNull()

        val nullableEnum = properties["nullableEnum"] as StringPropertyDefinition
        nullableEnum.type shouldBe listOf("string", "null")
        nullableEnum.nullable.shouldBeNull()
        nullableEnum.enum shouldBe listOf("DEBUG", "INFO", "ERROR")

        val nullableList = properties["nullableList"] as ArrayPropertyDefinition
        nullableList.type shouldBe listOf("array", "null")
        nullableList.nullable.shouldBeNull()

        val nullableMap = properties["nullableMap"] as ObjectPropertyDefinition
        nullableMap.type shouldBe listOf("object", "null")
        nullableMap.nullable.shouldBeNull()
    }

    // Non-Nullable Complex Types Tests

    object NonNullableComplexTypes {
        data class Config(
            val host: String,
        )

        enum class LogLevel { DEBUG, INFO, ERROR }

        @Description("Test non-nullable complex types")
        fun testNonNullableComplex(
            obj: Config,
            enumVal: LogLevel,
            list: List<String>,
            map: Map<String, Int>,
        ) {
            // No-op
        }
    }

    @Test
    fun `Should handle non-nullable complex types`() {
        val schema = generator.generateSchema(NonNullableComplexTypes::testNonNullableComplex)

        val properties = schema.parameters.properties.shouldNotBeNull()

        val obj = properties["obj"] as ObjectPropertyDefinition
        obj.type shouldBe listOf("object")
        obj.nullable.shouldBeNull()

        val enumVal = properties["enumVal"] as StringPropertyDefinition
        enumVal.type shouldBe listOf("string")
        enumVal.nullable.shouldBeNull()
        enumVal.enum shouldBe listOf("DEBUG", "INFO", "ERROR")

        val list = properties["list"] as ArrayPropertyDefinition
        list.type shouldBe listOf("array")
        list.nullable.shouldBeNull()

        val map = properties["map"] as ObjectPropertyDefinition
        map.type shouldBe listOf("object")
        map.nullable.shouldBeNull()
    }

    // Description Propagation Tests

    object DescriptionsTest {
        @Description("Test description propagation")
        fun testDescriptions(
            @Description("String description")
            stringParam: String,
            @Description("Int description")
            intParam: Int,
            @Description("Boolean description")
            boolParam: Boolean,
            @Description("List description")
            listParam: List<String>,
            @Description("Object description")
            objectParam: SimpleObject,
        ) {
            // No-op
        }

        data class SimpleObject(
            val value: String,
        )
    }

    @Test
    fun `Should propagate descriptions to all property types`() {
        val schema = generator.generateSchema(DescriptionsTest::testDescriptions)

        val properties = schema.parameters.properties.shouldNotBeNull()

        val stringParam = properties["stringParam"] as StringPropertyDefinition
        stringParam.description shouldBe "String description"

        val intParam = properties["intParam"] as NumericPropertyDefinition
        intParam.description shouldBe "Int description"

        val boolParam = properties["boolParam"] as BooleanPropertyDefinition
        boolParam.description shouldBe "Boolean description"

        val listParam = properties["listParam"] as ArrayPropertyDefinition
        listParam.description shouldBe "List description"

        val objectParam = properties["objectParam"] as ObjectPropertyDefinition
        objectParam.description shouldBe "Object description"
    }

    // Nested Objects Tests

    object NestedObjectsTest {
        data class Address(
            @property:Description("Street name")
            val street: String,
            val city: String,
        )

        data class Person(
            val name: String,
            val address: Address,
        )

        @Description("Test nested objects")
        fun testNested(person: Person) {
            // No-op
        }
    }

    @Test
    fun `Should handle nested objects with required fields`() {
        val schema = generator.generateSchema(NestedObjectsTest::testNested)

        val properties = schema.parameters.properties.shouldNotBeNull()

        val person = properties["person"] as ObjectPropertyDefinition
        person.properties!!.size shouldBe 2
        person.required shouldBe listOf("name", "address")

        val address = person.properties!!["address"] as ObjectPropertyDefinition
        address.properties!!.size shouldBe 2
        address.required shouldBe listOf("street", "city")

        // Check that nested properties are captured correctly
        val street = address.properties!!["street"] as StringPropertyDefinition
        street.type shouldBe listOf("string")
    }

    // Complex Nested Collections Tests

    object ComplexCollectionsTest {
        data class Item(
            val id: Int,
            val name: String,
        )

        @Description("Test complex nested collections")
        fun testComplexCollections(
            listOfObjects: List<Item>,
            mapOfLists: Map<String, List<Int>>,
            nullableListOfNullables: List<String?>?,
        ) {
            // No-op
        }
    }

    @Test
    fun `Should handle complex nested collections`() {
        val schema = generator.generateSchema(ComplexCollectionsTest::testComplexCollections)

        val properties = requireNotNull(schema.parameters.properties)

        val listOfObjects = properties["listOfObjects"] as ArrayPropertyDefinition
        listOfObjects.type shouldBe listOf("array")
        val itemDef = listOfObjects.items as ObjectPropertyDefinition
        itemDef.properties!!.size shouldBe 2

        val mapOfLists = properties["mapOfLists"] as ObjectPropertyDefinition
        mapOfLists.type shouldBe listOf("object")

        val nullableListOfNullables = properties["nullableListOfNullables"] as ArrayPropertyDefinition
        nullableListOfNullables.type shouldBe listOf("array", "null")
    }

    // Edge Case: Function with no description

    object NoDescriptionTest {
        fun noDescription(param: String) {
            // No-op
        }
    }

    @Test
    fun `Should handle function with no description`() {
        val schema = generator.generateSchema(NoDescriptionTest::noDescription)

        schema.description shouldBe ""
        schema.parameters.properties?.shouldHaveSize(1)
    }

    // Edge Case: Byte and Short types

    object ByteShortTest {
        @Description("Test byte and short types")
        fun testByteShort(
            byteVal: Byte,
            shortVal: Short,
            nullableByte: Byte?,
            nullableShort: Short?,
        ) {
            // No-op
        }
    }

    @Test
    fun `Should handle Byte and Short as integer types`() {
        val schema = generator.generateSchema(ByteShortTest::testByteShort)

        schema.parameters shouldNotBeNull {
            properties.shouldNotBeNull()

            numericProperty("byteVal") shouldNotBeNull {
                type shouldBe listOf("integer")
            }

            numericProperty("shortVal") shouldNotBeNull {
                type shouldBe listOf("integer")
            }

            numericProperty("nullableByte") shouldNotBeNull {
                type shouldBe listOf("integer", "null")
            }

            numericProperty("nullableShort") shouldNotBeNull {
                type shouldBe listOf("integer", "null")
            }
        }
    }

    // All Required Fields Test

    object AllRequiredTest {
        @Description("Test all fields are required")
        fun allRequired(
            required1: String,
            required2: Int,
            required3: Boolean,
        ) {
            // No-op
        }
    }

    @Test
    fun `Should mark all fields as required`() {
        val schema = generator.generateSchema(AllRequiredTest::allRequired)

        schema.parameters.required shouldBe listOf("required1", "required2", "required3")
    }

    // Map with Complex Values Test

    object MapComplexTest {
        data class Value(
            val data: String,
        )

        @Description("Test map with complex values")
        fun testMapComplex(
            mapOfObjects: Map<String, Value>,
            nullableMapOfObjects: Map<String, Value>?,
        ) {
            // No-op
        }
    }

    @Test
    fun `Should handle maps with complex value types`() {
        val schema = generator.generateSchema(MapComplexTest::testMapComplex)

        val properties = schema.parameters.properties

        schema.parameters shouldNotBeNull {
            properties.shouldNotBeNull()

            objectProperty("mapOfObjects") shouldNotBeNull {
                type shouldBe listOf("object")
                additionalProperties.shouldNotBeNull()
            }

            objectProperty("nullableMapOfObjects") shouldNotBeNull {
                type shouldBe listOf("object", "null")
            }
        }
    }
}
