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
              "$schema": "https://json-schema.org/draft/2020-12/schema",
              "$id": "kotlinx.schema.integration.type.Container",
              "description": "A generic container that wraps content with optional metadata.",
              "type": "object",
              "properties": {
                "content": {
                  "type": "object",
                  "description": "The wrapped content value",
                  "properties": {},
                  "required": [],
                  "additionalProperties": false
                },
                "metadata": {
                  "type": "object",
                  "description": "Arbitrary metadata key-value pairs",
                  "additionalProperties": {
                    "type": "object",
                    "properties": {},
                    "required": [],
                    "additionalProperties": false
                  }
                }
              },
              "required": ["content", "metadata"],
              "additionalProperties": false
            }
            """.trimIndent()
    }
}
