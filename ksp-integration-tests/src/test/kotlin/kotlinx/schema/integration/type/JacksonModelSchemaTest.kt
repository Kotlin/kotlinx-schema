package kotlinx.schema.integration.type

import io.kotest.assertions.json.shouldEqualJson
import kotlinx.serialization.json.Json
import kotlin.test.Test

/**
 * Tests for JacksonModel schema generation - Jackson annotation extraction.
 */
class JacksonModelSchemaTest {
    @Test
    fun `extracts descriptions from Jackson annotations`() {
        val schema = JacksonModel::class.jsonSchemaString

        // language=json
        schema shouldEqualJson
            $$"""
            {
              "$schema": "https://json-schema.org/draft/2020-12/schema",
              "$id": "kotlinx.schema.integration.type.JacksonModel",
              "description": "A purchasable product using Jackson annotations.",
              "type": "object",
              "properties": {
                "id": {
                  "type": "integer",
                  "description": "Unique identifier for the product"
                },
                "name": {
                  "type": "string",
                  "description": "Human-readable product name"
                },
                "description": {
                  "type": ["string", "null"],
                  "description": "Optional detailed description of the product"
                },
                "price": {
                  "type": "number",
                  "description": "Unit price expressed as a decimal number"
                },
                "inStock": {
                  "type": "boolean",
                  "description": "Whether the product is currently in stock"
                },
                "tags": {
                  "type": "array",
                  "description": "List of tags for categorization and search",
                  "items": {
                    "type": "string"
                  }
                }
              },
              "required": ["id", "name", "description", "price", "inStock", "tags"],
              "additionalProperties": false
            }
            """.trimIndent()
    }

    @Test
    fun `extracts input schema from function`() {
        val schema = createJacksonModelJsonSchema()
        val schemaString = createJacksonModelJsonSchemaString()

        schemaString shouldEqualJson Json.encodeToString(schema)
        println(schemaString)
    }

    @Test
    fun `extracts output schema from function`() {
        val schema = createJacksonModelJsonSchema()
        val schemaString = createJacksonModelJsonSchemaString()

        schemaString shouldEqualJson Json.encodeToString(schema)
        println(schemaString)
    }
}
