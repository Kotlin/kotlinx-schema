package kotlinx.schema.integration.type

import io.kotest.assertions.json.shouldEqualJson
import kotlin.test.Test

/**
 * Tests for Container schema generation - generic type parameters.
 */
class ContainerSchemaTest {
    @Test
    fun `generates schema with generic type parameter resolution`() {
        val schemaString = Container::class.jsonSchemaString

        // language=json
        schemaString shouldEqualJson
            $$"""
            {
              "$id": "kotlinx.schema.integration.type.Container",
              "$schema": "https://json-schema.org/draft/2020-12/schema",
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
              "description": "A generic container that wraps content with optional metadata.",
              "$defs": {
                "kotlin.Any": {
                  "type": "object",
                  "properties": {},
                  "required": [],
                  "additionalProperties": false
                }
              }
            }
            """.trimIndent()
    }
}
