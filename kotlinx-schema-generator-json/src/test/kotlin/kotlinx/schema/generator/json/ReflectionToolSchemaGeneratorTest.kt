package kotlinx.schema.generator.json

import io.kotest.assertions.json.shouldEqualJson
import kotlinx.schema.Description
import kotlin.test.Test

class ReflectionToolSchemaGeneratorTest {
    private val generator = ReflectionToolSchemaGenerator.Default

    object SimplePrimitives {
        @Description("Greets a person")
        fun greet(
            @Description("Person's name")
            name: String,
            age: Int,
        ): String = "$name: $age"
    }

    @Test
    fun `generates schema for simple function with primitives`() {
        val schemaString = generator.generateSchemaString(SimplePrimitives::greet)
        schemaString shouldEqualJson
            // language=json
            """
            {
                "type": "object",
                "properties": {
                    "name": {
                        "type": "string",
                        "description": "Person's name"
                    },
                    "age": {
                        "type": "integer"
                    }
                },
                "required": ["name", "age"]
            }
            """.trimIndent()
    }

    object Collections {
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
                "required": ["items", "metadata"]
            }
            """.trimIndent()
    }

    object NullableParameters {
        fun processData(
            required: String,
            optional: String?,
            optional2: String? = "foo",
        ): String = "$required: $optional, $optional2"
    }

    @Test
    fun `generates schema for function with nullable parameters`() {
        val schemaString = generator.generateSchemaString(NullableParameters::processData)
        schemaString shouldEqualJson
            // language=json
            """
            {
                "type": "object",
                "properties": {
                    "required": {
                        "type": "string"
                    },
                    "optional": {
                        "type": ["string", "null"]
                    },
                    "optional2": {
                        "type": ["string", "null"]
                    }
                },
                "required": ["required", "optional", "optional2"]
            }
            """.trimIndent()
    }

    object ComplexTypes {
        data class Config(
            val host: String,
            val port: Int = 8080,
        )

        fun connect(
            config: Config,
            timeout: Long = 5000,
        ): String = "${config.host}:${config.port} - $timeout"
    }

    @Test
    fun `generates schema for function with complex types`() {
        val schemaString = generator.generateSchemaString(ComplexTypes::connect)
        schemaString shouldEqualJson
            // language=json
            """
            {
                "type": "object",
                "properties": {
                    "config": {
                        "type": "object",
                        "properties": {
                            "host": {
                                "type": "string"
                            },
                            "port": {
                                "type": "integer"
                            }
                        },
                        "required": ["host", "port"],
                        "additionalProperties": false
                    },
                    "timeout": {
                        "type": "integer"
                    }
                },
                "required": ["config", "timeout"]
            }
            """.trimIndent()
    }

    object EnumParameter {
        enum class LogLevel { DEBUG, INFO, WARN, ERROR }

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
                "required": ["message", "level"]
            }
            """.trimIndent()
    }

    object VariousNumericTypes {
        @Suppress("LongParameterList")
        fun calculate(
            byteVal: Byte,
            shortVal: Short,
            intVal: Int,
            longVal: Long,
            floatVal: Float,
            doubleVal: Double,
        ): Double = byteVal + shortVal + intVal + longVal + floatVal + doubleVal
    }

    @Test
    fun `generates schema for function with various numeric types`() {
        val schemaString = generator.generateSchemaString(VariousNumericTypes::calculate)
        schemaString shouldEqualJson
            // language=json
            """
            {
                "type": "object",
                "properties": {
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
                        "type": "number"
                    },
                    "doubleVal": {
                        "type": "number"
                    }
                },
                "required": ["byteVal", "shortVal", "intVal", "longVal", "floatVal", "doubleVal"]
            }
            """.trimIndent()
    }
}
