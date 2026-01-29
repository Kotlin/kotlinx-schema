package kotlinx.schema.integration.type

import io.kotest.assertions.json.shouldEqualJson
import kotlin.test.Test

/**
 * Tests for Product schema generation - all primitive types and collections.
 */
class ProductSchemaTest {
    @Test
    fun `generates schema with all property types and nullable handling`() {
        val schema = Product::class.jsonSchemaString

        // language=json
        schema shouldEqualJson
            $$"""
            {
              "$id": "kotlinx.schema.integration.type.Product",
              "$schema": "https://json-schema.org/draft/2020-12/schema",
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
                  "items": {
                    "type": "string"
                  },
                  "description": "List of tags for categorization and search"
                }
              },
              "required": ["id", "name", "description", "price"],
              "additionalProperties": false,
              "description": "A purchasable product with pricing and inventory info."
            }
            """.trimIndent()
    }
}
