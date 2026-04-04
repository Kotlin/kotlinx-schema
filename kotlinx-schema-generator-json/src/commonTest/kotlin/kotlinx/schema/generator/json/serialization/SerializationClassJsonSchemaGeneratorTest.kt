package kotlinx.schema.generator.json.serialization

import io.kotest.assertions.json.shouldEqualJson
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.test.Test

class SerializationClassJsonSchemaGeneratorTest {
    @Serializable
    @CustomDescription("Nested property class")
    data class NestedProperty(
        @property:CustomDescription("Nested foo property")
        val foo: String,
        @property:CustomDescription("Nested bar property")
        val bar: Int,
    )

    @Serializable
    @SerialName("TestClosedPolymorphism")
    @CustomDescription("A closed polymorphism")
    sealed class TestClosedPolymorphism {
        abstract val id: String

        @Serializable
        @CustomDescription("First subclass")
        @Suppress("unused")
        data class SubClass1(
            @property:CustomDescription("Subclass identifier")
            override val id: String,
            @property:CustomDescription("First property")
            val property1: String,
        ) : TestClosedPolymorphism()

        @Serializable
        @CustomDescription("Second subclass")
        @Suppress("unused")
        data class SubClass2(
            @property:CustomDescription("Subclass identifier")
            override val id: String,
            @property:CustomDescription("Second property")
            val property2: Int,
        ) : TestClosedPolymorphism()
    }

    @Serializable
    @CustomDescription("A test enum")
    @Suppress("unused")
    enum class TestEnum {
        One,
        Two,
    }

    @Serializable
    @CustomDescription("A test data object")
    data object TestObject

    val generator =
        SerializationClassJsonSchemaGenerator(
            introspectorConfig =
                SerializationClassSchemaIntrospector.Config(
                    descriptionExtractor = { annotations ->
                        annotations.filterIsInstance<CustomDescription>().firstOrNull()?.value
                    },
                ),
        )

    @Test
    fun `Should generate JsonSchema for complex class`() {
        val schema = generator.generateSchemaString(TestClass.serializer().descriptor)

        schema shouldEqualJson
            // language=JSON
            $$"""
            {
              "$schema": "https://json-schema.org/draft/2020-12/schema",
              "$id": "kotlinx.schema.generator.json.serialization.TestClass",
              "description": "A test class",
              "type": "object",
              "properties": {
                "stringProperty": {
                  "type": "string",
                  "description": "A string property"
                },
                "intProperty": {
                  "type": "integer",
                  "description": "An int property"
                },
                "longProperty": {
                  "type": "integer",
                  "description": "A long property"
                },
                "doubleProperty": {
                  "type": "number",
                  "description": "A double property"
                },
                "floatProperty": {
                  "type": "number",
                  "description": "A float property"
                },
                "booleanNullableProperty": {
                  "type": [
                    "boolean",
                    "null"
                  ],
                  "description": "A nullable boolean property"
                },
                "nullableProperty": {
                  "type": [
                    "string",
                    "null"
                  ],
                  "description": "A nullable string property"
                },
                "listProperty": {
                  "type": "array",
                  "description": "A list of strings",
                  "items": {
                    "type": "string"
                  }
                },
                "mapProperty": {
                  "type": "object",
                  "description": "A map of integers",
                  "additionalProperties": {
                    "type": "integer"
                  }
                },
                "nestedProperty": {
                  "description": "A custom nested property",
                  "$ref": "#/$defs/kotlinx.schema.generator.json.serialization.SerializationClassJsonSchemaGeneratorTest.NestedProperty"
                },
                "nestedNullableProperty": {
                  "oneOf": [
                    {
                      "type": "null"
                    },
                    {
                      "$ref": "#/$defs/kotlinx.schema.generator.json.serialization.SerializationClassJsonSchemaGeneratorTest.NestedProperty"
                    }
                  ],
                  "description": "A custom nested nullable property"
                },
                "nestedListProperty": {
                  "type": "array",
                  "description": "A list of nested properties",
                  "items": {
                    "$ref": "#/$defs/kotlinx.schema.generator.json.serialization.SerializationClassJsonSchemaGeneratorTest.NestedProperty"
                  }
                },
                "nestedNullableListProperty": {
                  "type": [
                    "array",
                    "null"
                  ],
                  "description": "A custom nested nullable list property",
                  "items": {
                    "$ref": "#/$defs/kotlinx.schema.generator.json.serialization.SerializationClassJsonSchemaGeneratorTest.NestedProperty"
                  }
                },
                "nestedMapProperty": {
                  "type": "object",
                  "description": "A map of nested properties",
                  "additionalProperties": {
                    "$ref": "#/$defs/kotlinx.schema.generator.json.serialization.SerializationClassJsonSchemaGeneratorTest.NestedProperty"
                  }
                },
                "polymorphicProperty": {
                  "description": "A custom polymorphic property",
                  "$ref": "#/$defs/TestClosedPolymorphism"
                },
                "enumProperty": {
                  "$ref": "#/$defs/kotlinx.schema.generator.json.serialization.SerializationClassJsonSchemaGeneratorTest.TestEnum",
                  "description": "An enum property"
                },
                "objectProperty": {
                  "$ref": "#/$defs/kotlinx.schema.generator.json.serialization.SerializationClassJsonSchemaGeneratorTest.TestObject",
                  "description": "A test object property"
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
                "kotlinx.schema.generator.json.serialization.SerializationClassJsonSchemaGeneratorTest.NestedProperty": {
                  "type": "object",
                  "description": "Nested property class",
                  "properties": {
                    "foo": {
                      "type": "string",
                      "description": "Nested foo property"
                    },
                    "bar": {
                      "type": "integer",
                      "description": "Nested bar property"
                    }
                  },
                  "required": [
                    "foo",
                    "bar"
                  ],
                  "additionalProperties": false
                },
                "TestClosedPolymorphism": {
                  "description": "A closed polymorphism",
                  "oneOf": [
                    {
                      "$ref": "#/$defs/kotlinx.schema.generator.json.serialization.SerializationClassJsonSchemaGeneratorTest.TestClosedPolymorphism.SubClass1"
                    },
                    {
                      "$ref": "#/$defs/kotlinx.schema.generator.json.serialization.SerializationClassJsonSchemaGeneratorTest.TestClosedPolymorphism.SubClass2"
                    }
                  ]
                },
                "kotlinx.schema.generator.json.serialization.SerializationClassJsonSchemaGeneratorTest.TestClosedPolymorphism.SubClass1": {
                  "type": "object",
                  "description": "First subclass",
                  "properties": {
                    "type": {
                      "type": "string",
                      "const": "kotlinx.schema.generator.json.serialization.SerializationClassJsonSchemaGeneratorTest.TestClosedPolymorphism.SubClass1"
                    },
                    "id": {
                      "type": "string",
                      "description": "Subclass identifier"
                    },
                    "property1": {
                      "type": "string",
                      "description": "First property"
                    }
                  },
                  "required": [
                    "type",
                    "id",
                    "property1"
                  ],
                  "additionalProperties": false
                },
                "kotlinx.schema.generator.json.serialization.SerializationClassJsonSchemaGeneratorTest.TestClosedPolymorphism.SubClass2": {
                  "type": "object",
                  "description": "Second subclass",
                  "properties": {
                    "type": {
                      "type": "string",
                      "const": "kotlinx.schema.generator.json.serialization.SerializationClassJsonSchemaGeneratorTest.TestClosedPolymorphism.SubClass2"
                    },
                    "id": {
                      "type": "string",
                      "description": "Subclass identifier"
                    },
                    "property2": {
                      "type": "integer",
                      "description": "Second property"
                    }
                  },
                  "required": [
                    "type",
                    "id",
                    "property2"
                  ],
                  "additionalProperties": false
                },
                "kotlinx.schema.generator.json.serialization.SerializationClassJsonSchemaGeneratorTest.TestEnum": {
                  "type": "string",
                  "description": "A test enum",
                  "enum": [
                    "One",
                    "Two"
                  ]
                },
                "kotlinx.schema.generator.json.serialization.SerializationClassJsonSchemaGeneratorTest.TestObject": {
                  "type": "object",
                  "description": "A test data object",
                  "properties": {},
                  "required": [],
                  "additionalProperties": false
                }
              }
            }
            """.trimIndent()
    }
}
