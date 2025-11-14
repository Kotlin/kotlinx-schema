@file:Suppress("JsonStandardCompliance")

package kotlinx.schema.integration

import io.kotest.assertions.json.shouldEqualJson
import kotlin.test.Test

/**
 * Integration tests that verify KSP-generated extension properties work correctly
 * with Jackson @JsonClassDescription and @JsonPropertyDescription annotations
 */
@Suppress("LongMethod")
class JacksonAnnotationsTest {
    @Test
    fun `Should generate jsonSchema form  JacksonModel`() {
        val schema = JacksonModel::class.jsonSchemaString

        // language=json
        schema shouldEqualJson
            $$"""
            {
              "$id": "kotlinx.schema.integration.JacksonModel",
              "$defs": {
                "kotlinx.schema.integration.JacksonModel": {
                  "type": "object",
                  "properties": {
                    "id": { "type": "integer", "description": "Unique identifier for the product" },
                    "name": { "type": "string", "description": "Human-readable product name" },
                    "description": { "type": ["string", "null"], "description": "Optional detailed description of the product" },
                    "price": { "type": "number", "description": "Unit price expressed as a decimal number" },
                    "inStock": { "type": "boolean", "description": "Whether the product is currently in stock" },
                    "tags": { "type": "array", "items": { "type": "string" }, "description": "List of tags for categorization and search" }
                  },
                  "required": [
                    "id",
                    "name",
                    "description",
                    "price"
                  ],
                  "additionalProperties": false,
                  "description": "A purchasable product using Jackson annotations."
                }
              },
              "$ref": "#/$defs/kotlinx.schema.integration.JacksonModel"
            }
            """.trimIndent()
    }
}
