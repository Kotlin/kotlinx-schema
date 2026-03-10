@file:Suppress("FunctionOnlyReturningConstant", "LongMethod", "LongParameterList", "UnusedParameter", "unused")

package kotlinx.schema.generator.json

import io.kotest.assertions.json.shouldEqualJson
import kotlinx.schema.Description
import kotlinx.serialization.json.Json
import kotlin.test.Test

class JsonSchemaGeneratorTest {
    @Description("A test class")
    data class TestClass(
        @property:Description("A string property")
        val stringProperty: String,
        val intProperty: Int,
        val longProperty: Long,
        val doubleProperty: Double,
        val floatProperty: Float,
        val booleanNullableProperty: Boolean?,
        val nullableProperty: String? = null,
        val listProperty: List<String> = emptyList(),
        val mapProperty: Map<String, Int> = emptyMap(),
        @property:Description("A custom nested property")
        val nestedProperty: NestedProperty = NestedProperty("foo", 1),
        val nestedListProperty: List<NestedProperty> = emptyList(),
        val nestedMapProperty: Map<String, NestedProperty> = emptyMap(),
        @property:Description("A custom polymorphic property")
        val polymorphicProperty: TestClosedPolymorphism = TestClosedPolymorphism.SubClass1("id1", "property1"),
        val enumProperty: TestEnum = TestEnum.One,
        val objectProperty: TestObject = TestObject,
    )

    @Description("Nested property class")
    data class NestedProperty(
        @property:Description("Nested foo property")
        val foo: String,
        val bar: Int,
    )

    @Suppress("unused")
    sealed class TestClosedPolymorphism {
        abstract val id: String

        data class SubClass1(
            override val id: String,
            val property1: String,
        ) : TestClosedPolymorphism()

        data class SubClass2(
            override val id: String,
            val property2: Int,
        ) : TestClosedPolymorphism()
    }

    @Suppress("unused")
    enum class TestEnum {
        One,
        Two,
    }

    data object TestObject

    private val generator =
        ReflectionClassJsonSchemaGenerator(
            json = Json { prettyPrint = true },
            config = JsonSchemaConfig.Default,
        )

    @Test
    fun `Should generate JsonSchema for complex class`() {
        println("Generating schema for TestClass with generator config: ${JsonSchemaConfig.Default}")

        val schema = generator.generateSchemaString(TestClass::class)

        schema shouldEqualJson
            $$"""
            {
              "$schema": "https://json-schema.org/draft/2020-12/schema",
              "$id": "kotlinx.schema.generator.json.JsonSchemaGeneratorTest.TestClass",
              "description": "A test class",
              "type": "object",
              "properties": {
                "stringProperty": {
                  "type": "string",
                  "description": "A string property"
                },
                "intProperty": {
                  "type": "integer"
                },
                "longProperty": {
                  "type": "integer"
                },
                "doubleProperty": {
                  "type": "number"
                },
                "floatProperty": {
                  "type": "number"
                },
                "booleanNullableProperty": {
                  "type": [
                    "boolean",
                    "null"
                  ]
                },
                "nullableProperty": {
                  "type": [
                    "string",
                    "null"
                  ]
                },
                "listProperty": {
                  "type": "array",
                  "default": [],
                  "items": {
                    "type": "string"
                  }
                },
                "mapProperty": {
                  "type": "object",
                  "default": {},
                  "additionalProperties": {
                    "type": "integer"
                  }
                },
                "nestedProperty": {
                  "$ref": "#/$defs/kotlinx.schema.generator.json.JsonSchemaGeneratorTest.NestedProperty",
                  "description": "A custom nested property"
                },
                "nestedListProperty": {
                  "type": "array",
                  "default": [],
                  "items": {
                    "$ref": "#/$defs/kotlinx.schema.generator.json.JsonSchemaGeneratorTest.NestedProperty"
                  }
                },
                "nestedMapProperty": {
                  "type": "object",
                  "default": {},
                  "additionalProperties": {
                    "$ref": "#/$defs/kotlinx.schema.generator.json.JsonSchemaGeneratorTest.NestedProperty"
                  }
                },
                "polymorphicProperty": {
                  "$ref": "#/$defs/kotlinx.schema.generator.json.JsonSchemaGeneratorTest.TestClosedPolymorphism",
                  "description": "A custom polymorphic property"
                },
                "enumProperty": {
                  "$ref": "#/$defs/kotlinx.schema.generator.json.JsonSchemaGeneratorTest.TestEnum"
                },
                "objectProperty": {
                  "$ref": "#/$defs/kotlinx.schema.generator.json.JsonSchemaGeneratorTest.TestObject"
                }
              },
              "additionalProperties": false,
              "required": [
                "stringProperty",
                "intProperty",
                "longProperty",
                "doubleProperty",
                "floatProperty",
                "booleanNullableProperty"
              ],
              "$defs": {
                "kotlinx.schema.generator.json.JsonSchemaGeneratorTest.NestedProperty": {
                  "type": "object",
                  "description": "Nested property class",
                  "properties": {
                    "foo": { "type": "string", "description": "Nested foo property" },
                    "bar": { "type": "integer" }
                  },
                  "required": ["foo", "bar"],
                  "additionalProperties": false
                },
                "kotlinx.schema.generator.json.JsonSchemaGeneratorTest.TestClosedPolymorphism": {
                  "oneOf": [
                    { "$ref": "#/$defs/kotlinx.schema.generator.json.JsonSchemaGeneratorTest.TestClosedPolymorphism.SubClass1" },
                    { "$ref": "#/$defs/kotlinx.schema.generator.json.JsonSchemaGeneratorTest.TestClosedPolymorphism.SubClass2" }
                  ]
                },
                "kotlinx.schema.generator.json.JsonSchemaGeneratorTest.TestClosedPolymorphism.SubClass1": {
                  "type": "object",
                  "properties": {
                    "type": {
                      "type": "string",
                      "const": "kotlinx.schema.generator.json.JsonSchemaGeneratorTest.TestClosedPolymorphism.SubClass1"
                    },
                    "id": { "type": "string" },
                    "property1": { "type": "string" }
                  },
                  "required": ["type", "id", "property1"],
                  "additionalProperties": false
                },
                "kotlinx.schema.generator.json.JsonSchemaGeneratorTest.TestClosedPolymorphism.SubClass2": {
                  "type": "object",
                  "properties": {
                    "type": {
                      "type": "string",
                      "const": "kotlinx.schema.generator.json.JsonSchemaGeneratorTest.TestClosedPolymorphism.SubClass2"
                    },
                    "id": { "type": "string" },
                    "property2": { "type": "integer" }
                  },
                  "required": ["type", "id", "property2"],
                  "additionalProperties": false
                },
                "kotlinx.schema.generator.json.JsonSchemaGeneratorTest.TestEnum": {
                  "type": "string",
                  "enum": ["One", "Two"]
                },
                "kotlinx.schema.generator.json.JsonSchemaGeneratorTest.TestObject": {
                  "type": "object",
                  "properties": {},
                  "required": [],
                  "additionalProperties": false
                }
              }
            }
            """.trimIndent()
    }
}
