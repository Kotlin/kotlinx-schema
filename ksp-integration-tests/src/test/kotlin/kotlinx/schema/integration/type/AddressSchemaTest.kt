package kotlinx.schema.integration.type

import io.kotest.assertions.json.shouldEqualJson
import kotlin.test.Test

/**
 * Tests for Address schema generation - default value handling.
 */
class AddressSchemaTest {
    @Test
    fun `generates schema with default value properties excluded from required`() {
        val schema = Address::class.jsonSchemaString

        // language=json
        schema shouldEqualJson
            $$"""
            {
              "$id": "kotlinx.schema.integration.type.Address",
              "$defs": {
                "kotlinx.schema.integration.type.Address": {
                  "type": "object",
                  "properties": {
                    "street": {
                      "type": "string",
                      "description": "Street address, including house number"
                    },
                    "city": {
                      "type": "string",
                      "description": "City or town name"
                    },
                    "zipCode": {
                      "type": "string",
                      "description": "Postal or ZIP code"
                    },
                    "country": {
                      "type": "string",
                      "description": "Two-letter ISO country code; defaults to US"
                    }
                  },
                  "required": ["street", "city", "zipCode"],
                  "additionalProperties": false,
                  "description": "A postal address for deliveries and billing."
                }
              },
              "$ref": "#/$defs/kotlinx.schema.integration.type.Address"
            }
            """.trimIndent()
    }
}
