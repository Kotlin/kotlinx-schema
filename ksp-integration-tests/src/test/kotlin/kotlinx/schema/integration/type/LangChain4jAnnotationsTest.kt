@file:Suppress("JsonStandardCompliance")

package kotlinx.schema.integration.type

import io.kotest.assertions.json.shouldEqualJson
import kotlin.test.Test

/**
 * Integration tests that verify KSP-generated extension properties work correctly
 * with LangChain4j @P annotations
 */
@Suppress("LongMethod")
class LangChain4jAnnotationsTest {
    @Test
    fun `Should generate jsonSchema form LangChain4jModel`() {
        val schema = LangChain4jModel::class.jsonSchemaString

        // language=json
        schema shouldEqualJson
            $$"""
            {
              "$id": "kotlinx.schema.integration.type.LangChain4jModel",
              "$schema": "https://json-schema.org/draft/2020-12/schema",
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
              "description": "A purchasable product using LangChain4j annotations."
            }
            """.trimIndent()
    }
}
