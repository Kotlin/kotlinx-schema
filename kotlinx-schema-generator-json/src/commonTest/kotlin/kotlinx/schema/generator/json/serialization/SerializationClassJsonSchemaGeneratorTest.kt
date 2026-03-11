package kotlinx.schema.generator.json.serialization

import io.kotest.assertions.json.shouldEqualJson
import kotlinx.serialization.SerialInfo
import kotlinx.serialization.Serializable
import kotlin.test.Test

class SerializationClassJsonSchemaGeneratorTest {
    @SerialInfo
    annotation class CustomDescription(
        val value: String,
    )

    @Serializable
    @CustomDescription("A test class")
    data class TestClass(
        @property:CustomDescription("A string property")
        val stringProperty: String,
        val intProperty: Int,
        val longProperty: Long,
        val doubleProperty: Double,
        val floatProperty: Float,
        val booleanNullableProperty: Boolean?,
        val nullableProperty: String? = null,
        val listProperty: List<String> = emptyList(),
        val mapProperty: Map<String, Int> = emptyMap(),
        @property:CustomDescription("A custom nested property")
        val nestedProperty: NestedProperty = NestedProperty("foo", 1),
        @property:CustomDescription("A custom nested nullable property")
        val nestedNullableProperty: NestedProperty? = null,
        val nestedListProperty: List<NestedProperty> = emptyList(),
        @property:CustomDescription("A custom nested nullable list property")
        val nestedNullableListProperty: List<NestedProperty>? = null,
        val nestedMapProperty: Map<String, NestedProperty> = emptyMap(),
        @property:CustomDescription("A custom polymorphic property")
        val polymorphicProperty: TestClosedPolymorphism = TestClosedPolymorphism.SubClass1("id1", "property1"),
        val enumProperty: TestEnum = TestEnum.One,
        val objectProperty: TestObject = TestObject,
    )

    @Serializable
    @CustomDescription("Nested property class")
    data class NestedProperty(
        @property:CustomDescription("Nested foo property")
        val foo: String,
        val bar: Int,
    )

    @Serializable
    data class UnsignedPropertyHolder(
        @property:CustomDescription("Unsigned byte property")
        val uByteProperty: UByte,
        @property:CustomDescription("Unsigned short property")
        val uShortProperty: UShort,
        @property:CustomDescription("Unsigned int property")
        val uIntProperty: UInt,
        @property:CustomDescription("Unsigned long property")
        val uLongProperty: ULong,
        @property:CustomDescription("Nullable unsigned int property")
        val nullableUIntProperty: UInt? = null,
    )

    @Serializable
    sealed class TestClosedPolymorphism {
        abstract val id: String

        @Serializable
        @Suppress("unused")
        data class SubClass1(
            override val id: String,
            val property1: String,
        ) : TestClosedPolymorphism()

        @Serializable
        @Suppress("unused")
        data class SubClass2(
            override val id: String,
            val property2: Int,
        ) : TestClosedPolymorphism()
    }

    @Serializable
    @Suppress("unused")
    enum class TestEnum {
        One,
        Two,
    }

    @Serializable
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
              "$id": "kotlinx.schema.generator.json.serialization.SerializationClassJsonSchemaGeneratorTest.TestClass",
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
                  "items": {
                    "type": "string"
                  }
                },
                "mapProperty": {
                  "type": "object",
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
                  "additionalProperties": {
                    "$ref": "#/$defs/kotlinx.schema.generator.json.serialization.SerializationClassJsonSchemaGeneratorTest.NestedProperty"
                  }
                },
                "polymorphicProperty": {
                  "description": "A custom polymorphic property",
                  "$ref": "#/$defs/kotlinx.schema.generator.json.serialization.SerializationClassJsonSchemaGeneratorTest.TestClosedPolymorphism"
                },
                "enumProperty": {
                  "$ref": "#/$defs/kotlinx.schema.generator.json.serialization.SerializationClassJsonSchemaGeneratorTest.TestEnum"
                },
                "objectProperty": {
                  "$ref": "#/$defs/kotlinx.schema.generator.json.serialization.SerializationClassJsonSchemaGeneratorTest.TestObject"
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
                      "type": "integer"
                    }
                  },
                  "required": [
                    "foo",
                    "bar"
                  ],
                  "additionalProperties": false
                },
                "kotlinx.schema.generator.json.serialization.SerializationClassJsonSchemaGeneratorTest.TestClosedPolymorphism": {
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
                  "properties": {
                    "type": {
                      "type": "string",
                      "const": "kotlinx.schema.generator.json.serialization.SerializationClassJsonSchemaGeneratorTest.TestClosedPolymorphism.SubClass1"
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
                "kotlinx.schema.generator.json.serialization.SerializationClassJsonSchemaGeneratorTest.TestClosedPolymorphism.SubClass2": {
                  "type": "object",
                  "properties": {
                    "type": {
                      "type": "string",
                      "const": "kotlinx.schema.generator.json.serialization.SerializationClassJsonSchemaGeneratorTest.TestClosedPolymorphism.SubClass2"
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
                },
                "kotlinx.schema.generator.json.serialization.SerializationClassJsonSchemaGeneratorTest.TestEnum": {
                  "type": "string",
                  "enum": [
                    "One",
                    "Two"
                  ]
                },
                "kotlinx.schema.generator.json.serialization.SerializationClassJsonSchemaGeneratorTest.TestObject": {
                  "type": "object",
                  "properties": {},
                  "required": [],
                  "additionalProperties": false
                }
              }
            }
            """.trimIndent()
    }

    @Test
    fun `Should generate JsonSchema for unsigned numeric properties`() {
        val schema = generator.generateSchemaString(UnsignedPropertyHolder.serializer().descriptor)

        schema shouldEqualJson
            // language=JSON
            $$"""
            {
              "$schema": "https://json-schema.org/draft/2020-12/schema",
              "$id": "kotlinx.schema.generator.json.serialization.SerializationClassJsonSchemaGeneratorTest.UnsignedPropertyHolder",
              "type": "object",
              "properties": {
                "uByteProperty": {
                  "type": "integer",
                  "description": "Unsigned byte property",
                  "minimum": 0
                },
                "uShortProperty": {
                  "type": "integer",
                  "description": "Unsigned short property",
                  "minimum": 0
                },
                "uIntProperty": {
                  "type": "integer",
                  "description": "Unsigned int property",
                  "minimum": 0
                },
                "uLongProperty": {
                  "type": "integer",
                  "description": "Unsigned long property",
                  "minimum": 0
                },
                "nullableUIntProperty": {
                  "type": [
                    "integer",
                    "null"
                  ],
                  "description": "Nullable unsigned int property",
                  "minimum": 0
                }
              },
              "additionalProperties": false,
              "required": [
                "uByteProperty",
                "uShortProperty",
                "uIntProperty",
                "uLongProperty"
              ]
            }
            """.trimIndent()
    }
}
