@file:Suppress("FunctionOnlyReturningConstant", "LongMethod", "LongParameterList", "UnusedParameter", "unused")

package kotlinx.schema.generator.json

import io.kotest.assertions.json.shouldEqualJson
import kotlinx.schema.Description
import kotlinx.schema.generator.core.SchemaGeneratorService
import kotlinx.schema.json.FunctionCallingSchema
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.reflect.KCallable
import kotlin.test.Test

/**
 * Local test annotation mirroring the shape of [ai.koog.agents.core.tools.annotations.LLMDescription]:
 * two String elements — [value] (shorthand) and [description] (verbose).
 *
 * Matched by simple name "LLMDescription" via [kotlinx.schema.generator.core.ir.Introspections].
 */
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
internal annotation class LLMDescription(
    val value: String = "",
    val description: String = "",
)

class FunctionCallingSchemaGeneratorTest {
    private val generator =
        ReflectionFunctionCallingSchemaGenerator(
            json = Json { prettyPrint = true },
            // Like Strict, but with
            config =
                FunctionCallingSchemaConfig(
                    respectDefaultPresence = false,
                    requireNullableFields = true,
                    useUnionTypes = true,
                    useNullableField = false,
                    includePolymorphicDiscriminator = true,
                    strictMode = true,
                ),
        )

    object SimplePrimitives {
        @Description("Greets a person")
        fun greet(
            @Description("Person's name")
            name: String,
            @Description("Person's age")
            age: Int?,
            byteVal: Byte,
            shortVal: Short,
            intVal: Int,
            longVal: Long,
            floatVal: Float? = null,
            doubleVal: Double = 2.0,
        ): String = "$name: $age"
    }

    @Test
    fun `generates schema for simple function with primitives, numbers, nullable and default values`() {
        val schema = generator.generateSchema(SimplePrimitives::greet)

        val schemaString = generator.generateSchemaString(SimplePrimitives::greet)
        schemaString shouldEqualJson
            // language=json
            """
            {
              "type": "function",
              "name": "greet",
              "description": "Greets a person",
              "strict": true,
              "parameters": {
                "type": "object",
                "properties": {
                  "name": {
                    "type": "string",
                    "description": "Person's name"
                  },
                  "age": {
                    "type": [
                      "integer",
                      "null"
                    ],
                    "description": "Person's age"
                  },
                  "byteVal": {
                    "type": "integer"
                  },
                  "shortVal": {
                    "type": "integer"
                  },
                  "intVal": {
                    "type": "integer"
                  },
                  "longVal": {
                    "type": "integer"
                  },
                  "floatVal": {
                    "type": [
                      "number",
                      "null"
                    ]
                  },
                  "doubleVal": {
                    "type": "number"
                  }
                },
                "required": [
                  "name",
                  "age",
                  "byteVal",
                  "shortVal",
                  "intVal",
                  "longVal",
                  "floatVal",
                  "doubleVal"
                ],
                "additionalProperties": false
              }
            }
            """.trimIndent()

        json.encodeToString(schema) shouldEqualJson schemaString
    }

    object Collections {
        @Description("Process items with metadata")
        fun processItems(
            items: List<String>,
            metadata: Map<String, Int>,
        ): String = "$items: $metadata"
    }

    @Test
    fun `generates schema for function with collections`() {
        val schemaString = generator.generateSchemaString(Collections::processItems)
        schemaString shouldEqualJson
            // language=json
            """
            {
                "type": "function",
                "name": "processItems",
                "description": "Process items with metadata",
                "strict": true,
                "parameters": {
                    "type": "object",
                    "properties": {
                        "items": {
                            "type": "array",
                            "items": {
                                "type": "string"
                            }
                        },
                        "metadata": {
                            "type": "object",
                            "additionalProperties": {
                                "type": "integer"
                            }
                        }
                    },
                    "required": ["items", "metadata"],
                    "additionalProperties": false
                }
            }
            """.trimIndent()
    }

    object EnumParameter {
        @Suppress("unused")
        enum class LogLevel { DEBUG, INFO, WARN, ERROR }

        @Description("Log a message")
        fun log(
            message: String,
            level: LogLevel = LogLevel.INFO,
        ) {
            println("$level: $message")
        }
    }

    @Test
    fun `generates schema for function with enum parameter`() {
        val schemaString = generator.generateSchemaString(EnumParameter::log)
        schemaString shouldEqualJson
            """
            {
                "type": "function",
                "name": "log",
                "description": "Log a message",
                "strict": true,
                "parameters": {
                    "type": "object",
                    "properties": {
                        "message": {
                            "type": "string"
                        },
                        "level": {
                            "type": "string",
                            "enum": ["DEBUG", "INFO", "WARN", "ERROR"]
                        }
                    },
                    "required": ["message", "level"],
                    "additionalProperties": false
                }
            }
            """.trimIndent()
    }

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

    /**
     * Suspendable complex
     */
    object SuspendableComplexTypes {
        @Suppress("RedundantSuspendModifier")
        @Description("Sample function")
        suspend fun sampleFunction(
            @Description("Sample parameter")
            a: String,
            @Description("Another sample parameter")
            b: TestClass? = null,
        ): String = ""
    }

    @Test
    fun `generates schema for suspendable function with complex parameters`() {
        val schemaString = generator.generateSchemaString(SuspendableComplexTypes::sampleFunction)
        schemaString shouldEqualJson
            """
            {
              "type": "function",
              "name": "sampleFunction",
              "description": "Sample function",
              "strict": true,
              "parameters": {
                "type": "object",
                "properties": {
                  "a": {
                    "type": "string",
                    "description": "Sample parameter"
                  },
                  "b": {
                    "type": [
                      "object",
                      "null"
                    ],
                    "description": "Another sample parameter",
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
                        "type": "object",
                        "description": "A custom nested property",
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
                        "anyOf": [
                          {
                            "type": "object",
                            "properties": {
                              "type": {
                                "type": "string",
                                "const": "kotlinx.schema.generator.json.FunctionCallingSchemaGeneratorTest.TestClosedPolymorphism.SubClass1"
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
                          {
                            "type": "object",
                            "properties": {
                              "type": {
                                "type": "string",
                                "const": "kotlinx.schema.generator.json.FunctionCallingSchemaGeneratorTest.TestClosedPolymorphism.SubClass2"
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
                        ],
                        "description": "A custom polymorphic property"
                      },
                      "enumProperty": {
                        "type": "string",
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
                    "required": [
                      "stringProperty",
                      "intProperty",
                      "longProperty",
                      "doubleProperty",
                      "floatProperty",
                      "booleanNullableProperty",
                      "nullableProperty",
                      "listProperty",
                      "mapProperty",
                      "nestedProperty",
                      "nestedListProperty",
                      "nestedMapProperty",
                      "polymorphicProperty",
                      "enumProperty",
                      "objectProperty"
                    ],
                    "additionalProperties": false
                  }
                },
                "required": [
                  "a",
                  "b"
                ],
                "additionalProperties": false
              }
            } 
            """.trimIndent()
    }

    // Service locator test

    object SimpleFunction {
        @Description("Greet a person")
        fun greet(name: String) = "Hello, $name"
    }

    @Test
    fun `should use SchemaGeneratorService for function calling`() {
        val generator =
            SchemaGeneratorService.getGenerator(
                KCallable::class,
                FunctionCallingSchema::class,
            )
        val result = generator?.generateSchemaString(SimpleFunction::greet)
        result!! shouldEqualJson
            """
            {
                "type": "function",
                "name": "greet",
                "description": "Greet a person",
                "strict": true,
                "parameters": {
                    "type": "object",
                    "properties": {
                        "name": {
                            "type": "string"
                        }
                    },
                    "required": ["name"],
                    "additionalProperties": false
                }
            }
            """.trimIndent()
    }

    @Serializable
    data class TestArg(
        @property:Description("int argument")
        val a: Int,
        @property:Description("string argument")
        val b: String? = null,
    )

    @Serializable
    data class TestResult(
        val field1: String,
        val field2: Int,
    )

    @Description("Serialization args tool")
    fun serializationArgsTool(
        @Description("Test argument")
        foo: TestArg,
    ): TestResult =
        TestResult(
            field1 = "foo",
            field2 = foo.a,
        )

    /**
     * kotlinx.serialization generates synthetic primary constructor, check that it's handled properly and
     * effective primary constructor is used for schema generation.
     */
    @Test
    fun `generates schema for function with kotlinx serialization annotated args`() {
        val schemaString = generator.generateSchemaString(::serializationArgsTool)

        schemaString shouldEqualJson
            """
            {
                "type": "function",
                "name": "serializationArgsTool",
                "description": "Serialization args tool",
                "strict": true,
                "parameters": {
                    "type": "object",
                    "properties": {
                        "foo": {
                            "type": "object",
                            "description": "Test argument",
                            "properties": {
                                "a": {
                                    "type": "integer",
                                    "description": "int argument"
                                },
                                "b": {
                                    "type": [
                                        "string",
                                        "null"
                                    ],
                                    "description": "string argument"
                                }
                            },
                            "required": [
                                "a",
                                "b"
                            ],
                            "additionalProperties": false
                        }
                    },
                    "required": [
                        "foo"
                    ],
                    "additionalProperties": false
                }
            } 
            """.trimIndent()
    }

    object AnyTypedParameters {
        @Description("Process arbitrary data")
        fun process(
            data: Map<String, Any>,
            extra: Any?,
            @Description("Labelled content")
            content: Any,
        ): String = ""
    }

    @Test
    fun `generates schema for function with kotlin Any typed parameters`() {
        val schemaString = generator.generateSchemaString(AnyTypedParameters::process)

        schemaString shouldEqualJson
            // language=JSON
            """
            {
              "type": "function",
              "name": "process",
              "description": "Process arbitrary data",
              "strict": true,
              "parameters": {
                "type": "object",
                "properties": {
                  "data": {
                    "type": "object",
                    "additionalProperties": {}
                  },
                  "extra": {},
                  "content": {
                    "description": "Labelled content"
                  }
                },
                "required": ["data", "extra", "content"],
                "additionalProperties": false
              }
            }
            """.trimIndent()
    }

    open class Base {
        @Description("Tool from a base class")
        open fun overriddenTool(
            @Description("Foo description")
            foo: String,
            @Description("Bar description")
            bar: Int,
        ): String = "$foo, $bar"
    }

    object BaseImpl : Base() {
        override fun overriddenTool(
            foo: String,
            @Description("Overridden bar description")
            bar: Int,
        ): String = "Overridden $foo, $bar"
    }

    @Test
    fun `generates schema for function overridden from the base class`() {
        val schemaString = generator.generateSchemaString(BaseImpl::overriddenTool)

        schemaString shouldEqualJson
            // language=JSON
            """
            {
                "type": "function",
                "name": "overriddenTool",
                "description": "Tool from a base class",
                "strict": true,
                "parameters": {
                    "type": "object",
                    "properties": {
                        "foo": {
                            "type": "string",
                            "description": "Foo description"
                        },
                        "bar": {
                            "type": "integer",
                            "description": "Overridden bar description"
                        }
                    },
                    "required": [
                        "foo",
                        "bar"
                    ],
                    "additionalProperties": false
                }
            }
            """.trimIndent()
    }

    //region LLMDescription regression tests
    object LLMDescriptionTool {
        // Regression: @LLMDescription has two elements (value, description).
        // Using the verbose description= style leaves value="" — the extractor must not return ""
        // and instead find the non-empty description field.
        @LLMDescription(description = "Search for products in the catalog")
        fun search(
            @LLMDescription(description = "Search query string")
            query: String,
            @LLMDescription("Maximum number of results")
            limit: Int = 10,
        ): String = ""
    }

    @Test
    fun `extracts descriptions from LLMDescription verbose style on function and parameters`() {
        // Regression: @LLMDescription(description = "text") must not produce empty description.
        // Previously getDescriptionFromAnnotation returned "" because value="" was the first match.
        val schemaString = generator.generateSchemaString(LLMDescriptionTool::search)

        schemaString shouldEqualJson
            // language=json
            """
            {
                "type": "function",
                "name": "search",
                "description": "Search for products in the catalog",
                "strict": true,
                "parameters": {
                    "type": "object",
                    "properties": {
                        "query": {
                            "type": "string",
                            "description": "Search query string"
                        },
                        "limit": {
                            "type": "integer",
                            "description": "Maximum number of results"
                        }
                    },
                    "required": ["query", "limit"],
                    "additionalProperties": false
                }
            }
            """.trimIndent()
    }
    //endregion

    //region @SerialDescription
    object SerialDescribed {
        @SerialDescription("Looks up a product by identifier")
        fun findProduct(
            @SerialDescription("The product id to look up")
            id: Long,
            includeArchived: Boolean = false,
        ): String = ""
    }

    @Test
    fun `reflection generator recognizes @SerialDescription on function and parameter`() {
        val schema = generator.generateSchemaString(SerialDescribed::findProduct)

        schema shouldEqualJson
            // language=json
            """
            {
              "type": "function",
              "name": "findProduct",
              "description": "Looks up a product by identifier",
              "strict": true,
              "parameters": {
                "type": "object",
                "properties": {
                  "id": {
                    "type": "integer",
                    "description": "The product id to look up"
                  },
                  "includeArchived": {
                    "type": "boolean"
                  }
                },
                "required": ["id", "includeArchived"],
                "additionalProperties": false
              }
            }
            """.trimIndent()
    }
    //endregion
}
