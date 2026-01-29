package kotlinx.schema.integration.type

import io.kotest.assertions.json.shouldEqualJson
import kotlin.test.Test

/**
 * Tests for Order schema generation - complex nested structures.
 */
class OrderSchemaTest {
    @Suppress("LongMethod")
    @Test
    fun `generates complete nested schema with all types`() {
        val schema = Order::class.jsonSchemaString

        // language=json
        schema shouldEqualJson
            $$"""
            {
              "$schema": "https://json-schema.org/draft/2020-12/schema",
              "$id": "kotlinx.schema.integration.type.Order",
              "description": "An order placed by a customer containing multiple items.",
              "type": "object",
              "properties": {
                "id": {
                  "type": "string",
                  "description": "Unique order identifier"
                },
                "customer": {
                  "type": "object",
                  "description": "The customer who placed the order",
                  "properties": {
                    "firstName": {
                      "type": "string",
                      "description": "Given name of the person"
                    },
                    "lastName": {
                      "type": "string",
                      "description": "Family name of the person"
                    },
                    "age": {
                      "type": "integer",
                      "description": "Age of the person in years"
                    }
                  },
                  "required": ["firstName", "lastName", "age"],
                  "additionalProperties": false
                },
                "shippingAddress": {
                  "type": "object",
                  "description": "Destination address for shipment",
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
                  "required": ["street", "city", "zipCode", "country"],
                  "additionalProperties": false
                },
                "items": {
                  "type": "array",
                  "description": "List of items included in the order",
                  "items": {
                    "type": "object",
                    "description": "A purchasable product with pricing and inventory info.",
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
                },
                "status": {
                  "type": "string",
                  "description": "Current status of the order",
                  "enum": ["ACTIVE", "INACTIVE", "PENDING"]
                }
              },
              "required": ["id", "customer", "shippingAddress", "items", "status"],
              "additionalProperties": false
            }
            """.trimIndent()
    }
}
