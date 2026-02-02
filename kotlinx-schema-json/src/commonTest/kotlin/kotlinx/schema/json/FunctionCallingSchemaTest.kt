package kotlinx.schema.json

import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlin.test.Test

class FunctionCallingSchemaTest {
    private val json = Json { prettyPrint = true }

    @Test
    fun `FunctionCallingSchema with all fields`() {
        // language=json
        val jsonString =
            """
            {
              "type": "function",
              "name": "get_weather",
              "title": "Get Weather",
              "description": "Get the current weather for a location",
              "strict": true,
              "parameters": {
                "type": "object",
                "properties": {
                  "location": {
                    "type": "string",
                    "description": "The city and state, e.g. San Francisco, CA"
                  },
                  "unit": {
                    "type": "string",
                    "description": "The temperature unit",
                    "enum": ["celsius", "fahrenheit"]
                  }
                },
                "required": ["location"]
              }
            }
            """.trimIndent()

        val decoded = deserializeAndSerialize<FunctionCallingSchema>(jsonString, json)

        decoded.type shouldBe "function"
        decoded.name shouldBe "get_weather"
        decoded.title shouldBe "Get Weather"
        decoded.description shouldBe "Get the current weather for a location"
        decoded.strict shouldBe true
        decoded.parameters.shouldNotBeNull {
            properties.shouldNotBeNull {
                val locationProp = this["location"].shouldBeInstanceOf<StringPropertyDefinition>()
                locationProp.description shouldBe "The city and state, e.g. San Francisco, CA"

                val unitProp = this["unit"].shouldBeInstanceOf<StringPropertyDefinition>()
                unitProp.enum shouldBe listOf("celsius", "fahrenheit")
            }
            required shouldBe listOf("location")
        }
    }

    @Test
    fun `FunctionCallingSchema with minimal fields`() {
        // language=json
        val jsonString =
            """
            {
              "name": "simple_function",
              "parameters": {
                "type": "object"
              }
            }
            """.trimIndent()

        val decoded = json.decodeFromString<FunctionCallingSchema>(jsonString)

        decoded.type shouldBe "function" // Default value
        decoded.name shouldBe "simple_function"
        decoded.title.shouldBeNull()
        decoded.description.shouldBeNull()
        decoded.strict shouldBe true // Default value
        decoded.parameters.shouldNotBeNull {
            properties.shouldBeNull()
            required.shouldBeNull()
        }
    }

    @Test
    fun `supports complex nested parameters`() {
        // language=json
        val jsonString =
            """
            {
              "name": "complex_function",
              "description": "A function with complex nested parameters",
              "parameters": {
                "type": "object",
                "properties": {
                  "user": {
                    "type": "object",
                    "description": "User information",
                    "properties": {
                      "name": {
                        "type": "string",
                        "description": "User's name"
                      },
                      "age": {
                        "type": "number",
                        "description": "User's age"
                      },
                      "active": {
                        "type": "boolean",
                        "description": "Is user active"
                      }
                    },
                    "required": ["name"]
                  },
                  "tags": {
                    "type": "array",
                    "description": "List of tags",
                    "items": {
                      "type": "string"
                    }
                  }
                },
                "required": ["user"]
              }
            }
            """.trimIndent()

        val decoded = json.decodeFromString<FunctionCallingSchema>(jsonString)

        decoded.name shouldBe "complex_function"
        decoded.parameters.properties.shouldNotBeNull {
            val userProp = this["user"].shouldBeInstanceOf<ObjectPropertyDefinition>()
            userProp.description shouldBe "User information"
            userProp.properties.shouldNotBeNull {
                this["name"].shouldBeInstanceOf<StringPropertyDefinition>()
                this["age"].shouldBeInstanceOf<NumericPropertyDefinition>()
                this["active"].shouldBeInstanceOf<BooleanPropertyDefinition>()
            }
            userProp.required shouldBe listOf("name")

            val tagsProp = this["tags"].shouldBeInstanceOf<ArrayPropertyDefinition>()
            tagsProp.description shouldBe "List of tags"
            tagsProp.items.shouldBeInstanceOf<StringPropertyDefinition>()
        }
        decoded.parameters.required shouldBe listOf("user")
    }

    @Test
    fun `supports parameters with default and const values`() {
        // language=json
        val jsonString =
            """
            {
              "name": "api_request",
              "parameters": {
                "type": "object",
                "properties": {
                  "version": {
                    "type": "string",
                    "description": "API version",
                    "const": "v1"
                  },
                  "timeout": {
                    "type": "number",
                    "description": "Request timeout in seconds",
                    "default": 30
                  }
                }
              }
            }
            """.trimIndent()

        val decoded = json.decodeFromString<FunctionCallingSchema>(jsonString)

        decoded.parameters.properties.shouldNotBeNull {
            val versionProp = this["version"].shouldBeInstanceOf<StringPropertyDefinition>()
            versionProp.description shouldBe "API version"
            versionProp.constValue shouldBe JsonPrimitive("v1")

            val timeoutProp = this["timeout"].shouldBeInstanceOf<NumericPropertyDefinition>()
            timeoutProp.description shouldBe "Request timeout in seconds"
            timeoutProp.default shouldBe JsonPrimitive(30)
        }
    }

    @Test
    fun `supports title field for MCP Tool definition`() {
        // language=json
        val jsonString =
            """
            {
              "name": "mcp_tool",
              "title": "MCP Tool Title",
              "description": "MCP tool description",
              "parameters": {
                "type": "object"
              }
            }
            """.trimIndent()

        val decoded = json.decodeFromString<FunctionCallingSchema>(jsonString)

        decoded.name shouldBe "mcp_tool"
        decoded.title shouldBe "MCP Tool Title"
        decoded.description shouldBe "MCP tool description"
    }

    @Test
    fun `strict mode can be disabled`() {
        // language=json
        val jsonString =
            """
            {
              "name": "flexible_function",
              "strict": false,
              "parameters": {
                "type": "object"
              }
            }
            """.trimIndent()

        val decoded = json.decodeFromString<FunctionCallingSchema>(jsonString)

        decoded.strict shouldBe false
    }

    @Test
    fun `round-trip serialization preserves all data`() {
        // language=json
        val originalJson =
            """
            {
              "type": "function",
              "name": "round_trip_test",
              "title": "Round Trip Test",
              "description": "Testing round-trip serialization",
              "strict": false,
              "parameters": {
                "type": "object",
                "properties": {
                  "param1": {
                    "type": "string",
                    "description": "Parameter 1"
                  },
                  "param2": {
                    "type": "number",
                    "description": "Parameter 2"
                  }
                },
                "required": ["param1"]
              }
            }
            """.trimIndent()

        deserializeAndSerialize<FunctionCallingSchema>(originalJson)
    }

    @Test
    fun `encodeToString extension function works correctly`() {
        // language=json
        val jsonString =
            """
            {
              "name": "test_encode_string",
              "description": "Test encodeToString extension",
              "parameters": {
                "type": "object",
                "properties": {
                  "param": {
                    "type": "string"
                  }
                }
              }
            }
            """.trimIndent()

        val schema = json.decodeFromString<FunctionCallingSchema>(jsonString)

        // Call the extension function
        val encoded = schema.encodeToString(json)

        // Verify it produces valid JSON that can be decoded back
        val decoded = json.decodeFromString<FunctionCallingSchema>(encoded)
        decoded.name shouldBe "test_encode_string"
        decoded.description shouldBe "Test encodeToString extension"
    }

    @Test
    fun `encodeToJsonObject extension function works correctly`() {
        // language=json
        val jsonString =
            """
            {
              "name": "test_encode_object",
              "title": "Test Title",
              "description": "Test encodeToJsonObject extension",
              "strict": false,
              "parameters": {
                "type": "object",
                "properties": {
                  "arg": {
                    "type": "number"
                  }
                },
                "required": ["arg"]
              }
            }
            """.trimIndent()

        val schema = json.decodeFromString<FunctionCallingSchema>(jsonString)

        // Call the extension function
        val jsonObject = schema.encodeToJsonObject(json)

        // Verify the JsonObject structure
        jsonObject["name"]?.toString() shouldBe "\"test_encode_object\""
        jsonObject["title"]?.toString() shouldBe "\"Test Title\""
        jsonObject["description"]?.toString() shouldBe "\"Test encodeToJsonObject extension\""
        jsonObject["strict"]?.toString() shouldBe "false"
        jsonObject["parameters"].shouldNotBeNull()
    }

    @Test
    fun `encodeToString uses default Json when not specified`() {
        // language=json
        val jsonString =
            """
            {
              "name": "default_json_test",
              "parameters": {
                "type": "object"
              }
            }
            """.trimIndent()

        val schema = json.decodeFromString<FunctionCallingSchema>(jsonString)

        // Call without specifying Json instance (uses default)
        val encoded = schema.encodeToString()

        // Should still be valid JSON
        val decoded = Json.decodeFromString<FunctionCallingSchema>(encoded)
        decoded.name shouldBe "default_json_test"
    }

    @Test
    fun `encodeToJsonObject uses default Json when not specified`() {
        // language=json
        val jsonString =
            """
            {
              "name": "default_json_object_test",
              "parameters": {
                "type": "object"
              }
            }
            """.trimIndent()

        val schema = json.decodeFromString<FunctionCallingSchema>(jsonString)

        // Call without specifying Json instance (uses default)
        val jsonObject = schema.encodeToJsonObject()

        jsonObject["name"].shouldNotBeNull()
        jsonObject["parameters"].shouldNotBeNull()
    }
}
