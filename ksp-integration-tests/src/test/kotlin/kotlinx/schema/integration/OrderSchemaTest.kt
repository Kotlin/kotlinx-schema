package kotlinx.schema.integration

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
              "$id": "kotlinx.schema.integration.Order",
              "$defs": {
                "kotlinx.schema.integration.Person": {
                  "type": "object",
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
                  "additionalProperties": false,
                  "description": "A person with a first and last name and age."
                },
                "kotlinx.schema.integration.Address": {
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
                },
                "kotlinx.schema.integration.Product": {
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
                },
                "kotlinx.schema.integration.Status": {
                  "type": "string",
                  "enum": ["ACTIVE", "INACTIVE", "PENDING"],
                  "description": "Current lifecycle status of an entity."
                },
                "kotlinx.schema.integration.Order": {
                  "type": "object",
                  "properties": {
                    "id": {
                      "type": "string",
                      "description": "Unique order identifier"
                    },
                    "customer": {
                      "$ref": "#/$defs/kotlinx.schema.integration.Person",
                      "description": "The customer who placed the order"
                    },
                    "shippingAddress": {
                      "$ref": "#/$defs/kotlinx.schema.integration.Address",
                      "description": "Destination address for shipment"
                    },
                    "items": {
                      "type": "array",
                      "items": {
                        "$ref": "#/$defs/kotlinx.schema.integration.Product"
                      },
                      "description": "List of items included in the order"
                    },
                    "status": {
                      "$ref": "#/$defs/kotlinx.schema.integration.Status",
                      "description": "Current status of the order"
                    }
                  },
                  "required": ["id", "customer", "shippingAddress", "items", "status"],
                  "additionalProperties": false,
                  "description": "An order placed by a customer containing multiple items."
                }
              },
              "$ref": "#/$defs/kotlinx.schema.integration.Order"
            }
            """.trimIndent()
    }
}
