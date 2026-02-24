@file:Suppress("LongMethod", "LongParameterList", "UnusedParameter", "unused")

package kotlinx.schema.generator.json

import io.kotest.assertions.json.shouldEqualJson
import kotlinx.schema.Description
import kotlinx.schema.generator.core.SchemaGeneratorService
import kotlinx.schema.json.FunctionCallingSchema
import kotlin.reflect.KCallable
import kotlin.test.Test

class FunctionCallingSchemaGeneratorTest {
    private val generator =
        requireNotNull(
            SchemaGeneratorService.getGenerator(
                KCallable::class,
                FunctionCallingSchema::class,
            ),
        ) {
            "${ReflectionFunctionCallingSchemaGenerator::class} must be registered"
        }

    object SimplePrimitives {
        @Description("Greets a person")
        fun greet(
            @Description("Person's name")
            name: String,
            @Description("Person's age")
            age: Int?,
            byteVal: Byte,
            shortVal: Short?,
            intVal: Int,
            longVal: Long,
            floatVal: Float,
            doubleVal: Double,
        ): String = "$name: $age"
    }

    @Test
    fun `generates schema for simple function with primitives, numbers and nullable parameters`() {
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
                    "type": [
                      "integer",
                      "null"
                    ]
                  },
                  "intVal": {
                    "type": "integer"
                  },
                  "longVal": {
                    "type": "integer"
                  },
                  "floatVal": {
                    "type": "number"
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
            // language=json
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
        val nestedProperty: NestedProperty = NestedProperty("foo", 1),
        val nestedListProperty: List<NestedProperty> = emptyList(),
        val nestedMapProperty: Map<String, NestedProperty> = emptyMap(),
        // Doesn't work
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
            // language=json
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
}
