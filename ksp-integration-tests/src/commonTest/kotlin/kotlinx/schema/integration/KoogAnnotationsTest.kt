@file:Suppress("JsonStandardCompliance")

package kotlinx.schema.integration

import io.kotest.assertions.json.shouldEqualJson
import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlin.test.Test

/**
 * Integration tests that verify KSP-generated extension properties work correctly
 */
@Suppress("LongMethod")
class KoogAnnotationsTest {
    @Test
    fun `KoogModel class should have generated jsonSchemaString extension`() {
        val schema = KoogModel::class.jsonSchemaString

        // language=json
        schema shouldEqualJson
            $$"""
            {
              "$id": "kotlinx.schema.integration.KoogModel",
              "$defs": {
                "kotlinx.schema.integration.KoogModel": {
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
                  "description": "A purchasable product with pricing and inventory info."
                }
              },
              "$ref": "#/$defs/kotlinx.schema.integration.KoogModel"
            }
            """.trimIndent()
    }
}
