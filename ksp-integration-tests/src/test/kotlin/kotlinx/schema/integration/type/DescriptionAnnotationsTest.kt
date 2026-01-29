@file:Suppress("JsonStandardCompliance")

package kotlinx.schema.integration.type

import io.kotest.assertions.json.shouldEqualJson
import kotlin.test.Test

/**
 * Integration tests that verify KSP-generated extension properties work correctly
 */
@Suppress("LongMethod")
class DescriptionAnnotationsTest {
    @Test
    fun `Product class should have generated jsonSchemaString extension`() {
        val schema = Product::class.jsonSchemaString

        // language=json
        schema shouldEqualJson
            $$"""
            {
              "$schema": "https://json-schema.org/draft/2020-12/schema",
              "$id": "kotlinx.schema.integration.type.Product",
              "description": "A purchasable product with pricing and inventory info.",
              "type": "object",
              "properties": {
                "id": { "type": "integer", "description": "Unique identifier for the product" },
                "name": { "type": "string", "description": "Human-readable product name" },
                "description": { "type": ["string", "null"], "description": "Optional detailed description of the product" },
                "price": { "type": "number", "description": "Unit price expressed as a decimal number" },
                "inStock": { "type": "boolean", "description": "Whether the product is currently in stock" },
                "tags": { "type": "array", "description": "List of tags for categorization and search", "items": { "type": "string" } }
              },
              "required": [
                "id",
                "name",
                "description",
                "price",
                "inStock",
                "tags"
              ],
              "additionalProperties": false
            }
            """.trimIndent()
    }
}
