package kotlinx.schema.integration

import io.kotest.assertions.json.shouldEqualJson
import kotlin.test.Test

/**
 * Tests for Container schema generation - generic type parameters.
 */
class ContainerSchemaTest {
    @Test
    fun `generates schema with generic type parameter resolution`() {
        val schema = Container::class.jsonSchemaString

        // language=json
        schema shouldEqualJson
            $$"""
            {
              "$id": "kotlinx.schema.integration.Container",
              "$defs": {
                "kotlin.Any": {
                  "type": "object",
                  "properties": {},
                  "required": [],
                  "additionalProperties": false
                },
                "kotlinx.schema.integration.Container": {
                  "type": "object",
                  "properties": {
                    "content": {
                      "$ref": "#/$defs/kotlin.Any",
                      "description": "The wrapped content value"
                    },
                    "metadata": {
                      "type": "object",
                      "additionalProperties": {
                        "$ref": "#/$defs/kotlin.Any"
                      },
                      "description": "Arbitrary metadata key-value pairs"
                    }
                  },
                  "required": ["content"],
                  "additionalProperties": false,
                  "description": "A generic container that wraps content with optional metadata."
                }
              },
              "$ref": "#/$defs/kotlinx.schema.integration.Container"
            }
            """.trimIndent()
    }
}
