package kotlinx.schema.generator.json.serialization

import io.kotest.assertions.json.shouldEqualJson
import kotlinx.schema.Description
import kotlinx.schema.json.encodeToString
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.test.Test

class SerializationClassJsonSchemaGeneratorTest {
    @Serializable
    @SerialName("TestClass")
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

    @Serializable
    @SerialName("NestedProperty")
    @Description("Nested property class")
    data class NestedProperty(
        @property:Description("Nested foo property")
        val foo: String,
        val bar: Int,
    )

    @Serializable
    @SerialName("TestClosedPolymorphism")
    sealed class TestClosedPolymorphism {
        abstract val id: String

        @Suppress("unused")
        @Serializable
        @SerialName("ClosedSubclass1")
        data class SubClass1(
            override val id: String,
            val property1: String,
        ) : TestClosedPolymorphism()

        @Suppress("unused")
        @Serializable
        @SerialName("ClosedSubclass2")
        data class SubClass2(
            override val id: String,
            val property2: Int,
            val recursiveTypeProperty: TestClosedPolymorphism?,
            val recursiveTypeNullableProperty: TestClosedPolymorphism,
        ) : TestClosedPolymorphism()
    }

    @Suppress("unused")
    @SerialName("TestEnum")
    enum class TestEnum {
        One,
        Two,
    }

    @SerialName("TestObject")
    @Serializable
    data object TestObject

    val generator = SerializationClassJsonSchemaGenerator()

    @Suppress("LongMethod")
    @Test
    fun `Should generate JsonSchema from SerialDescriptor`() {
        @Suppress("UNUSED_VARIABLE")
        val expectedSchemaJson =
            //language=JSON
            $$"""
            {
                "$schema": "https://json-schema.org/draft/2020-12/schema",
                "$id": "TestClass",
                "type": "object",
                "additionalProperties": false,
                "$defs": {
                    "NestedProperty": {
                        "type": "object",
                        "properties": {
                            "foo": {
                                "type": "string"
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
                    "ClosedSubclass1": {
                        "type": "object",
                        "properties": {
                            "id": {
                                "type": "string"
                            },
                            "property1": {
                                "type": "string"
                            }
                        },
                        "required": [
                            "id",
                            "property1"
                        ],
                        "additionalProperties": false
                    },
                    "TestClosedPolymorphism?": {
                        "anyOf": [
                            {
                                "oneOf": [
                                    {
                                        "$ref": "#/$defs/ClosedSubclass1"
                                    },
                                    {
                                        "$ref": "#/$defs/ClosedSubclass2"
                                    }
                                ]
                            },
                            {
                                "type": "null"
                            }
                        ]
                    },
                    "ClosedSubclass2": {
                        "type": "object",
                        "properties": {
                            "id": {
                                "type": "string"
                            },
                            "property2": {
                                "type": "integer"
                            },
                            "recursiveTypeProperty": {
                                "$ref": "#/$defs/TestClosedPolymorphism?"
                            },
                            "recursiveTypeNullableProperty": {
                                "$ref": "#/$defs/TestClosedPolymorphism"
                            }
                        },
                        "required": [
                            "id",
                            "property2",
                            "recursiveTypeProperty",
                            "recursiveTypeNullableProperty"
                        ],
                        "additionalProperties": false
                    },
                    "TestClosedPolymorphism": {
                        "oneOf": [
                            {
                                "$ref": "#/$defs/ClosedSubclass1"
                            },
                            {
                                "$ref": "#/$defs/ClosedSubclass2"
                            }
                        ]
                    },
                    "TestEnum": {
                        "type": "string",
                        "enum": [
                            "One",
                            "Two"
                        ]
                    },
                    "TestObject": {
                        "type": "object",
                        "properties": {},
                        "required": [],
                        "additionalProperties": false
                    },
                    "TestClass": {
                        "type": "object",
                        "properties": {
                            "stringProperty": {
                                "type": "string"
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
                                "$ref": "#/$defs/NestedProperty"
                            },
                            "nestedListProperty": {
                                "type": "array",
                                "items": {
                                    "$ref": "#/$defs/NestedProperty"
                                }
                            },
                            "nestedMapProperty": {
                                "type": "object",
                                "additionalProperties": {
                                    "$ref": "#/$defs/NestedProperty"
                                }
                            },
                            "polymorphicProperty": {
                                "$ref": "#/$defs/TestClosedPolymorphism"
                            },
                            "enumProperty": {
                                "$ref": "#/$defs/TestEnum"
                            },
                            "objectProperty": {
                                "$ref": "#/$defs/TestObject"
                            }
                        },
                        "required": [
                            "stringProperty",
                            "intProperty",
                            "longProperty",
                            "doubleProperty",
                            "floatProperty",
                            "booleanNullableProperty"
                        ],
                        "additionalProperties": false
                    }
                }
            } 
            """.trimIndent()

        val schema = generator.generateSchema(TestClass.serializer().descriptor)
        val actualSchemaJson = schema.encodeToString(Json { prettyPrint = true })

        actualSchemaJson shouldEqualJson expectedSchemaJson
    }
}
