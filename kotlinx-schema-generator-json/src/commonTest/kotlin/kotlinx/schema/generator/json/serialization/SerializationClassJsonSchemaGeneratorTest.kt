@file:Suppress("LargeClass", "LongMethod")

package kotlinx.schema.generator.json.serialization

import io.kotest.assertions.json.shouldEqualJson
import kotlinx.schema.Description
import kotlinx.schema.json.encodeToString
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.test.Test

@Serializable
@SerialName("PrimitiveTypes")
private data class PrimitiveTypes(
    val stringProperty: String,
    val intProperty: Int,
    val longProperty: Long,
    val doubleProperty: Double,
    val floatProperty: Float,
    val booleanProperty: Boolean,
)

@Serializable
@SerialName("NullableProperties")
private data class NullableProperties(
    val requiredProperty: String,
    val nullableProperty: String?,
    val nullableWithDefault: String? = null,
)

@Serializable
@SerialName("Collections")
private data class Collections(
    val listProperty: List<String>,
    val mapProperty: Map<String, Int>,
    val emptyListWithDefault: List<String> = emptyList(),
    val emptyMapWithDefault: Map<String, Int> = emptyMap(),
)

@Serializable
@SerialName("Inner")
private data class Inner(
    val value: String,
)

@Serializable
@SerialName("NestedObjects")
private data class NestedObjects(
    val nested: Inner,
    val nestedList: List<Inner>,
    val nestedMap: Map<String, Inner>,
)

@Serializable
@SerialName("Shape")
private sealed class Shape {
    @Serializable
    @SerialName("Circle")
    data class Circle(
        val radius: Double,
    ) : Shape()

    @Serializable
    @SerialName("Rectangle")
    data class Rectangle(
        val width: Double,
        val height: Double,
    ) : Shape()
}

@Serializable
@SerialName("PolymorphicContainer")
private data class PolymorphicContainer(
    val shape: Shape,
)

@Serializable
@SerialName("Node")
private sealed class Node {
    abstract val id: String

    @Serializable
    @SerialName("Leaf")
    data class Leaf(
        override val id: String,
        val value: String,
    ) : Node()

    @Serializable
    @SerialName("Branch")
    data class Branch(
        override val id: String,
        val left: Node?,
        val right: Node,
    ) : Node()
}

@Serializable
@SerialName("Tree")
private data class Tree(
    val root: Node,
)

@Suppress("unused")
@Serializable
@SerialName("Status")
private enum class Status {
    Active,
    Inactive,
    Pending,
}

@Serializable
@SerialName("WithEnum")
private data class WithEnum(
    val status: Status,
)

@Serializable
@SerialName("Singleton")
private data object Singleton

@Serializable
@SerialName("WithDataObject")
private data class WithDataObject(
    val singleton: Singleton,
)

@Serializable
@SerialName("InnerWithDescription")
@Description("Inner class description")
private data class InnerWithDescription(
    @property:Description("Inner value description")
    val value: String,
)

@Serializable
@SerialName("WithDescriptions")
@Description("A class with descriptions")
private data class WithDescriptions(
    @property:Description("A documented property")
    val documentedProperty: String,
    val undocumentedProperty: Int,
    @property:Description("A documented nested property")
    val nested: InnerWithDescription,
)

@Serializable
@SerialName("CompleteNestedProperty")
@Description("Nested property class")
private data class CompleteNestedProperty(
    @property:Description("Nested foo property")
    val foo: String,
    val bar: Int,
)

@Serializable
@SerialName("TestClosedPolymorphism")
private sealed class TestClosedPolymorphism {
    abstract val id: String

    @Serializable
    @SerialName("ClosedSubclass1")
    data class SubClass1(
        override val id: String,
        val property1: String,
    ) : TestClosedPolymorphism()

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
@Serializable
@SerialName("TestEnum")
private enum class TestEnum {
    One,
    Two,
}

@Serializable
@SerialName("TestObject")
private data object TestObject

@Serializable
@SerialName("TestClass")
@Description("A test class")
private data class TestClass(
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
    val nestedProperty: CompleteNestedProperty = CompleteNestedProperty("foo", 1),
    val nestedListProperty: List<CompleteNestedProperty> = emptyList(),
    val nestedMapProperty: Map<String, CompleteNestedProperty> = emptyMap(),
    val polymorphicProperty: TestClosedPolymorphism = TestClosedPolymorphism.SubClass1("id1", "property1"),
    val enumProperty: TestEnum = TestEnum.One,
    val objectProperty: TestObject = TestObject,
)

class SerializationClassJsonSchemaGeneratorTest {
    private val generator = SerializationClassJsonSchemaGenerator()
    private val json = Json { prettyPrint = true }

    @Test
    fun `Should generate schema for primitive types`() {
        val expectedSchemaJson =
            //language=JSON
            $$"""
            {
                "$schema": "https://json-schema.org/draft/2020-12/schema",
                "$id": "PrimitiveTypes",
                "type": "object",
                "additionalProperties": false,
                "$defs": {
                    "PrimitiveTypes": {
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
                            "booleanProperty": {
                                "type": "boolean"
                            }
                        },
                        "required": [
                            "stringProperty",
                            "intProperty",
                            "longProperty",
                            "doubleProperty",
                            "floatProperty",
                            "booleanProperty"
                        ],
                        "additionalProperties": false
                    }
                }
            }
            """.trimIndent()

        val schema = generator.generateSchema(PrimitiveTypes.serializer().descriptor)
        val actualSchemaJson = schema.encodeToString(json)

        actualSchemaJson shouldEqualJson expectedSchemaJson
    }

    @Test
    fun `Should generate schema for nullable properties`() {
        val expectedSchemaJson =
            //language=JSON
            $$"""
            {
                "$schema": "https://json-schema.org/draft/2020-12/schema",
                "$id": "NullableProperties",
                "type": "object",
                "additionalProperties": false,
                "$defs": {
                    "NullableProperties": {
                        "type": "object",
                        "properties": {
                            "requiredProperty": {
                                "type": "string"
                            },
                            "nullableProperty": {
                                "type": [
                                    "string",
                                    "null"
                                ]
                            },
                            "nullableWithDefault": {
                                "type": [
                                    "string",
                                    "null"
                                ]
                            }
                        },
                        "required": [
                            "requiredProperty",
                            "nullableProperty"
                        ],
                        "additionalProperties": false
                    }
                }
            }
            """.trimIndent()

        val schema = generator.generateSchema(NullableProperties.serializer().descriptor)
        val actualSchemaJson = schema.encodeToString(json)

        actualSchemaJson shouldEqualJson expectedSchemaJson
    }

    @Test
    fun `Should generate schema for collections`() {
        val expectedSchemaJson =
            //language=JSON
            $$"""
            {
                "$schema": "https://json-schema.org/draft/2020-12/schema",
                "$id": "Collections",
                "type": "object",
                "additionalProperties": false,
                "$defs": {
                    "Collections": {
                        "type": "object",
                        "properties": {
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
                            "emptyListWithDefault": {
                                "type": "array",
                                "items": {
                                    "type": "string"
                                }
                            },
                            "emptyMapWithDefault": {
                                "type": "object",
                                "additionalProperties": {
                                    "type": "integer"
                                }
                            }
                        },
                        "required": [
                            "listProperty",
                            "mapProperty"
                        ],
                        "additionalProperties": false
                    }
                }
            }
            """.trimIndent()

        val schema = generator.generateSchema(Collections.serializer().descriptor)
        val actualSchemaJson = schema.encodeToString(json)

        actualSchemaJson shouldEqualJson expectedSchemaJson
    }

    @Test
    fun `Should generate schema for nested objects`() {
        val expectedSchemaJson =
            //language=JSON
            $$"""
            {
                "$schema": "https://json-schema.org/draft/2020-12/schema",
                "$id": "NestedObjects",
                "type": "object",
                "additionalProperties": false,
                "$defs": {
                    "Inner": {
                        "type": "object",
                        "properties": {
                            "value": {
                                "type": "string"
                            }
                        },
                        "required": [
                            "value"
                        ],
                        "additionalProperties": false
                    },
                    "NestedObjects": {
                        "type": "object",
                        "properties": {
                            "nested": {
                                "$ref": "#/$defs/Inner"
                            },
                            "nestedList": {
                                "type": "array",
                                "items": {
                                    "$ref": "#/$defs/Inner"
                                }
                            },
                            "nestedMap": {
                                "type": "object",
                                "additionalProperties": {
                                    "$ref": "#/$defs/Inner"
                                }
                            }
                        },
                        "required": [
                            "nested",
                            "nestedList",
                            "nestedMap"
                        ],
                        "additionalProperties": false
                    }
                }
            }
            """.trimIndent()

        val schema = generator.generateSchema(NestedObjects.serializer().descriptor)
        val actualSchemaJson = schema.encodeToString(json)

        actualSchemaJson shouldEqualJson expectedSchemaJson
    }

    @Test
    fun `Should generate schema for sealed class polymorphism`() {
        val expectedSchemaJson =
            //language=JSON
            $$"""
            {
                "$schema": "https://json-schema.org/draft/2020-12/schema",
                "$id": "PolymorphicContainer",
                "type": "object",
                "additionalProperties": false,
                "$defs": {
                    "Circle": {
                        "type": "object",
                        "properties": {
                            "radius": {
                                "type": "number"
                            }
                        },
                        "required": [
                            "radius"
                        ],
                        "additionalProperties": false
                    },
                    "Rectangle": {
                        "type": "object",
                        "properties": {
                            "width": {
                                "type": "number"
                            },
                            "height": {
                                "type": "number"
                            }
                        },
                        "required": [
                            "width",
                            "height"
                        ],
                        "additionalProperties": false
                    },
                    "Shape": {
                        "oneOf": [
                            {
                                "$ref": "#/$defs/Circle"
                            },
                            {
                                "$ref": "#/$defs/Rectangle"
                            }
                        ]
                    },
                    "PolymorphicContainer": {
                        "type": "object",
                        "properties": {
                            "shape": {
                                "$ref": "#/$defs/Shape"
                            }
                        },
                        "required": [
                            "shape"
                        ],
                        "additionalProperties": false
                    }
                }
            }
            """.trimIndent()

        val schema = generator.generateSchema(PolymorphicContainer.serializer().descriptor)
        val actualSchemaJson = schema.encodeToString(json)

        actualSchemaJson shouldEqualJson expectedSchemaJson
    }

    @Test
    fun `Should generate schema for recursive types`() {
        val expectedSchemaJson =
            //language=JSON
            $$"""
            {
                "$schema": "https://json-schema.org/draft/2020-12/schema",
                "$id": "Tree",
                "type": "object",
                "additionalProperties": false,
                "$defs": {
                    "Branch": {
                        "type": "object",
                        "properties": {
                            "id": {
                                "type": "string"
                            },
                            "left": {
                                "$ref": "#/$defs/Node?"
                            },
                            "right": {
                                "$ref": "#/$defs/Node"
                            }
                        },
                        "required": [
                            "id",
                            "left",
                            "right"
                        ],
                        "additionalProperties": false
                    },
                    "Leaf": {
                        "type": "object",
                        "properties": {
                            "id": {
                                "type": "string"
                            },
                            "value": {
                                "type": "string"
                            }
                        },
                        "required": [
                            "id",
                            "value"
                        ],
                        "additionalProperties": false
                    },
                    "Node?": {
                        "anyOf": [
                            {
                                "oneOf": [
                                    {
                                        "$ref": "#/$defs/Branch"
                                    },
                                    {
                                        "$ref": "#/$defs/Leaf"
                                    }
                                ]
                            },
                            {
                                "type": "null"
                            }
                        ]
                    },
                    "Node": {
                        "oneOf": [
                            {
                                "$ref": "#/$defs/Branch"
                            },
                            {
                                "$ref": "#/$defs/Leaf"
                            }
                        ]
                    },
                    "Tree": {
                        "type": "object",
                        "properties": {
                            "root": {
                                "$ref": "#/$defs/Node"
                            }
                        },
                        "required": [
                            "root"
                        ],
                        "additionalProperties": false
                    }
                }
            }
            """.trimIndent()

        val schema = generator.generateSchema(Tree.serializer().descriptor)
        val actualSchemaJson = schema.encodeToString(json)

        actualSchemaJson shouldEqualJson expectedSchemaJson
    }

    @Test
    fun `Should generate schema for enums`() {
        val expectedSchemaJson =
            //language=JSON
            $$"""
            {
                "$schema": "https://json-schema.org/draft/2020-12/schema",
                "$id": "WithEnum",
                "type": "object",
                "additionalProperties": false,
                "$defs": {
                    "Status": {
                        "type": "string",
                        "enum": [
                            "Active",
                            "Inactive",
                            "Pending"
                        ]
                    },
                    "WithEnum": {
                        "type": "object",
                        "properties": {
                            "status": {
                                "$ref": "#/$defs/Status"
                            }
                        },
                        "required": [
                            "status"
                        ],
                        "additionalProperties": false
                    }
                }
            }
            """.trimIndent()

        val schema = generator.generateSchema(WithEnum.serializer().descriptor)
        val actualSchemaJson = schema.encodeToString(json)

        actualSchemaJson shouldEqualJson expectedSchemaJson
    }

    @Test
    fun `Should generate schema for data objects`() {
        val expectedSchemaJson =
            //language=JSON
            $$"""
            {
                "$schema": "https://json-schema.org/draft/2020-12/schema",
                "$id": "WithDataObject",
                "type": "object",
                "additionalProperties": false,
                "$defs": {
                    "Singleton": {
                        "type": "object",
                        "properties": {},
                        "required": [],
                        "additionalProperties": false
                    },
                    "WithDataObject": {
                        "type": "object",
                        "properties": {
                            "singleton": {
                                "$ref": "#/$defs/Singleton"
                            }
                        },
                        "required": [
                            "singleton"
                        ],
                        "additionalProperties": false
                    }
                }
            }
            """.trimIndent()

        val schema = generator.generateSchema(WithDataObject.serializer().descriptor)
        val actualSchemaJson = schema.encodeToString(json)

        actualSchemaJson shouldEqualJson expectedSchemaJson
    }

    @Test
    fun `Should propagate descriptions from annotations`() {
        val expectedSchemaJson =
            //language=JSON
            $$"""
            {
                "$schema": "https://json-schema.org/draft/2020-12/schema",
                "$id": "WithDescriptions",
                "type": "object",
                "additionalProperties": false,
                "$defs": {
                    "InnerWithDescription": {
                        "type": "object",
                        "properties": {
                            "value": {
                                "type": "string"
                            }
                        },
                        "required": [
                            "value"
                        ],
                        "additionalProperties": false
                    },
                    "WithDescriptions": {
                        "type": "object",
                        "properties": {
                            "documentedProperty": {
                                "type": "string"
                            },
                            "undocumentedProperty": {
                                "type": "integer"
                            },
                            "nested": {
                                "$ref": "#/$defs/InnerWithDescription"
                            }
                        },
                        "required": [
                            "documentedProperty",
                            "undocumentedProperty",
                            "nested"
                        ],
                        "additionalProperties": false
                    }
                }
            }
            """.trimIndent()

        val schema = generator.generateSchema(WithDescriptions.serializer().descriptor)
        val actualSchemaJson = schema.encodeToString(json)

        actualSchemaJson shouldEqualJson expectedSchemaJson
    }

    @Test
    fun `Should generate complete schema with all features`() {
        val expectedSchemaJson =
            //language=JSON
            $$"""
            {
                "$schema": "https://json-schema.org/draft/2020-12/schema",
                "$id": "TestClass",
                "type": "object",
                "additionalProperties": false,
                "$defs": {
                    "CompleteNestedProperty": {
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
                                "$ref": "#/$defs/CompleteNestedProperty"
                            },
                            "nestedListProperty": {
                                "type": "array",
                                "items": {
                                    "$ref": "#/$defs/CompleteNestedProperty"
                                }
                            },
                            "nestedMapProperty": {
                                "type": "object",
                                "additionalProperties": {
                                    "$ref": "#/$defs/CompleteNestedProperty"
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
        val actualSchemaJson = schema.encodeToString(json)

        actualSchemaJson shouldEqualJson expectedSchemaJson
    }
}
