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
        val nestedProperty: NestedProperty = NestedProperty("foo", 1),
        val nestedListProperty: List<NestedProperty> = emptyList(),
        val nestedMapProperty: Map<String, NestedProperty> = emptyMap(),
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

    sealed class TestClosedPolymorphism {
        abstract val id: String

        @Suppress("unused")
        data class SubClass1(
            override val id: String,
            val property1: String,
        ) : TestClosedPolymorphism()

        @Suppress("unused")
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
            // Default, but with includePolymorphicDiscriminator enabled
            config =
                JsonSchemaConfig(
                    respectDefaultPresence = true,
                    requireNullableFields = true,
                    useUnionTypes = true,
                    useNullableField = false,
                    includePolymorphicDiscriminator = true,
                    includeOpenAPIPolymorphicDiscriminator = false,
                ),
        )

    @Test
    fun `Should generate JsonSchema for complex class`() {
        val schema = generator.generateSchemaString(TestClass::class)

        schema shouldEqualJson
            // language=JSON
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
                  "type": "object",
                  "description": "Nested property class",
                  "properties": {
                    "foo": {
                      "type": "string",
                      "description": "Nested foo property"
                    },
                    "bar": {
                      "type": "integer"
                    }
                  },
                  "required": [
                    "foo",
                    "bar"
                  ],
                  "additionalProperties": false
                },
                "nestedListProperty": {
                  "type": "array",
                  "default": [],
                  "items": {
                    "type": "object",
                    "description": "Nested property class",
                    "properties": {
                      "foo": {
                        "type": "string",
                        "description": "Nested foo property"
                      },
                      "bar": {
                        "type": "integer"
                      }
                    },
                    "required": [
                      "foo",
                      "bar"
                    ],
                    "additionalProperties": false
                  }
                },
                "nestedMapProperty": {
                  "type": "object",
                  "default": {},
                  "additionalProperties": {
                    "type": "object",
                    "description": "Nested property class",
                    "properties": {
                      "foo": {
                        "type": "string",
                        "description": "Nested foo property"
                      },
                      "bar": {
                        "type": "integer"
                      }
                    },
                    "required": [
                      "foo",
                      "bar"
                    ],
                    "additionalProperties": false
                  }
                },
                "polymorphicProperty": {
                  "oneOf": [
                    {
                      "$ref": "#/$defs/TestClosedPolymorphism.SubClass1"
                    },
                    {
                      "$ref": "#/$defs/TestClosedPolymorphism.SubClass2"
                    }
                  ]
                },
                "enumProperty": {
                  "type": "string",
                  "default": "One",
                  "enum": [
                    "One",
                    "Two"
                  ]
                },
                "objectProperty": {
                  "type": "object",
                  "properties": {},
                  "required": [],
                  "additionalProperties": false
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
                "TestClosedPolymorphism.SubClass1": {
                  "type": "object",
                  "properties": {
                    "type": {
                      "type": "string",
                      "const": "TestClosedPolymorphism.SubClass1"
                    },
                    "id": {
                      "type": "string"
                    },
                    "property1": {
                      "type": "string"
                    }
                  },
                  "required": [
                    "type",
                    "id",
                    "property1"
                  ],
                  "additionalProperties": false
                },
                "TestClosedPolymorphism.SubClass2": {
                  "type": "object",
                  "properties": {
                    "type": {
                      "type": "string",
                      "const": "TestClosedPolymorphism.SubClass2"
                    },
                    "id": {
                      "type": "string"
                    },
                    "property2": {
                      "type": "integer"
                    }
                  },
                  "required": [
                    "type",
                    "id",
                    "property2"
                  ],
                  "additionalProperties": false
                }
              }
            } 
            """.trimIndent()
    }
}
