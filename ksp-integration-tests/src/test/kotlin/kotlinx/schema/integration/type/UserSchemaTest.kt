package kotlinx.schema.integration.type

import io.kotest.assertions.json.shouldEqualJson
import kotlin.test.Test

/**
 * Tests for User schema generation - complex mixed types with nullable collections and maps.
 */
class UserSchemaTest {
    @Test
    fun `generates schema with mixed complex types and nullable Map`() {
        val schema = User::class.jsonSchemaString

        schema shouldEqualJson
            $$"""
            {
              "$id": "kotlinx.schema.integration.type.User",
              "$defs": {
                "kotlinx.schema.integration.type.User": {
                  "type": "object",
                  "properties": {
                    "name": {
                      "type": "string",
                      "description": "The name of the user"
                    },
                    "age": {
                      "type": ["integer", "null"]
                    },
                    "email": {
                      "type": "string"
                    },
                    "tags": {
                      "type": "array",
                      "items": {
                        "type": "string"
                      }
                    },
                    "attributes": {
                      "type": ["object", "null"],
                      "additionalProperties": {
                        "type": "integer"
                      }
                    }
                  },
                  "required": ["name", "age", "tags", "attributes"],
                  "additionalProperties": false,
                  "description": "A user model"
                }
              },
              "$ref": "#/$defs/kotlinx.schema.integration.type.User"
            }
            """.trimIndent()
    }
}
