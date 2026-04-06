package kotlinx.schema.integration.type

import io.kotest.assertions.json.shouldEqualJson
import kotlin.test.Test

class SerialDescriptionKspTest {
    @Test
    fun `KSP extracts class and property descriptions from @SerialDescription`() {
        val schema = SerialDescribedProduct::class.jsonSchemaString

        // language=json
        schema shouldEqualJson
            $$"""
            {
              "$schema": "https://json-schema.org/draft/2020-12/schema",
              "$id": "kotlinx.schema.integration.type.SerialDescribedProduct",
              "description": "A product described with @SerialDescription",
              "type": "object",
              "properties": {
                "id": { "type": "integer", "description": "Unique product identifier" },
                "name": { "type": "string", "description": "Human-readable product name" },
                "price": { "type": "number" }
              },
              "required": ["id", "name", "price"],
              "additionalProperties": false
            }
            """.trimIndent()
    }
}
