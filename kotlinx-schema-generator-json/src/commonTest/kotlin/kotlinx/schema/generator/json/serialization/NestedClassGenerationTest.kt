package kotlinx.schema.generator.json.serialization

import io.kotest.assertions.json.shouldEqualJson
import kotlinx.schema.json.encodeToString
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.test.Test

class NestedClassGenerationTest {
    val generator =
        SerializationClassJsonSchemaGenerator()

    @Serializable
    @SerialName("Nested")
    data class Nested(
        val foo: String,
        val bar: Int,
    )

    @Serializable
    @SerialName("Testing")
    data class Testing(
        val nested: Nested,
        val nestedNullable: Nested?,
    )

    @Test
    fun generateTestingSchema() {
        val schema = generator.generateSchema(Testing.serializer().descriptor)

        val schemaJson = schema.encodeToString(Json)
        schemaJson shouldEqualJson
            $$"""
            {
              "$schema": "https://json-schema.org/draft/2020-12/schema",
              "$id": "Testing",
              "type": "object",
              "properties": {
                "nested": {
                  "$ref": "#/$defs/Nested"
                },
                "nestedNullable": {
                  "oneOf": [
                    {
                      "type": "null"
                    },
                    {
                      "$ref": "#/$defs/Nested"
                    }
                  ]
                }
              },
              "additionalProperties": false,
              "required": [
                "nested",
                "nestedNullable"
              ],
              "$defs": {
                "Nested": {
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
                }
              }
            }
            """.trimIndent()
    }

    @Serializable
    data class Orders(
        val items: List<String>,
    )

    @Serializable
    data class Feedback(
        val orders: Orders?,
        val initiallyGeneratedOrders: Orders?,
    )

    @Test
    fun generateFeedbackSchema() {
        val schema = generator.generateSchema(Feedback.serializer().descriptor)

        val schemaJson = schema.encodeToString(Json)
        schemaJson shouldEqualJson
            $$"""
            {
              "$schema": "https://json-schema.org/draft/2020-12/schema",
              "$id": "kotlinx.schema.generator.json.serialization.NestedClassGenerationTest.Feedback",
              "type": "object",
              "properties": {
                "orders": {
                  "oneOf": [
                    {
                      "type": "null"
                    },
                    {
                      "$ref": "#/$defs/kotlinx.schema.generator.json.serialization.NestedClassGenerationTest.Orders"
                    }
                  ]
                },
                "initiallyGeneratedOrders": {
                  "oneOf": [
                    {
                      "type": "null"
                    },
                    {
                      "$ref": "#/$defs/kotlinx.schema.generator.json.serialization.NestedClassGenerationTest.Orders"
                    }
                  ]
                }
              },
              "additionalProperties": false,
              "required": [
                "orders",
                "initiallyGeneratedOrders"
              ],
              "$defs": {
                "kotlinx.schema.generator.json.serialization.NestedClassGenerationTest.Orders": {
                  "type": "object",
                  "properties": {
                    "items": {
                      "type": "array",
                      "items": {
                        "type": "string"
                      }
                    }
                  },
                  "required": [
                    "items"
                  ],
                  "additionalProperties": false
                }
              }
            }
            """.trimIndent()
    }
}
