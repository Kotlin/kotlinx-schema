@file:Suppress("JsonStandardCompliance")

package kotlinx.schema.integration

import io.kotest.assertions.json.shouldEqualJson
import kotlin.test.Test

/**
 * Advanced KSP schema generation test scenarios.
 * Tests complex cases including mixed types, nullable collections, and nested structures.
 */
@Suppress("LongMethod")
class KspAdvancedScenariosTest {
    /**
     * Tests objects with primitives, lists, maps, nullable types, default values, and descriptions
     */
    @Test
    fun `Should handle complex object with mixed property types`() {
        val schema = User::class.jsonSchemaString

        // language=json
        schema shouldEqualJson
            $$"""
            {
              "$id": "kotlinx.schema.integration.User",
              "$defs": {
                "kotlinx.schema.integration.User": {
                  "type": "object",
                  "properties": {
                    "name": {
                      "type": "string",
                      "description": "The name of the user"
                    },
                    "age": {
                      "type": ["integer", "null"]
                    },
                    "email": {
                      "type": "string"
                    },
                    "tags": {
                      "type": "array",
                      "items": {
                        "type": "string"
                      }
                    },
                    "attributes": {
                      "type": ["object", "null"],
                      "additionalProperties": {
                        "type": "integer"
                      }
                    }
                  },
                  "required": ["name", "age", "tags", "attributes"],
                  "additionalProperties": false,
                  "description": "A user model"
                }
              },
              "$ref": "#/$defs/kotlinx.schema.integration.User"
            }
            """.trimIndent()
    }

    /**
     * Tests enum schema generation with entries and descriptions
     */
    @Test
    fun `Should generate enum schema with all values`() {
        val schema = Status::class.jsonSchemaString

        // language=json
        schema shouldEqualJson
            $$"""
            {
              "$id": "kotlinx.schema.integration.Status",
              "$defs": {
                "kotlinx.schema.integration.Status": {
                  "description": "Current lifecycle status of an entity.",
                  "type": "string",
                  "enum": ["ACTIVE", "INACTIVE", "PENDING"]
                }
              },
              "$ref": "#/$defs/kotlinx.schema.integration.Status"
            }
            """.trimIndent()
    }

    /**
     * Tests sealed class schema generation with polymorphic nodes and subtypes
     */
    @Test
    fun `Should generate sealed class with oneOf and subtype definitions`() {
        val schema = Animal::class.jsonSchemaString

        // language=json
        schema shouldEqualJson
            $$"""
            {
              "$id": "kotlinx.schema.integration.Animal",
              "$defs": {
                "kotlinx.schema.integration.Animal": {
                  "oneOf": [
                    { "$ref": "#/$defs/kotlinx.schema.integration.Animal.Cat" },
                    { "$ref": "#/$defs/kotlinx.schema.integration.Animal.Dog" }
                  ],
                  "description": "Multicellular eukaryotic organism of the kingdom Metazoa"
                },
                "kotlinx.schema.integration.Animal.Cat": {
                  "type": "object",
                  "properties": {
                    "name": { "type": "string", "description": "Animal's name" },
                    "type": { "const": "kotlinx.schema.integration.Animal.Cat" }
                  },
                  "required": ["name", "type"],
                  "additionalProperties": false
                },
                "kotlinx.schema.integration.Animal.Dog": {
                  "type": "object",
                  "properties": {
                    "name": { "type": "string", "description": "Animal's name" },
                    "type": { "const": "kotlinx.schema.integration.Animal.Dog" }
                  },
                  "required": ["name", "type"],
                  "additionalProperties": false
                }
              },
              "$ref": "#/$defs/kotlinx.schema.integration.Animal"
            }
            """.trimIndent()
    }

    /**
     * Tests nullable collection handling
     */
    @Test
    fun `Should handle nullable collections correctly`() {
        val schema = User::class.jsonSchemaString

        // Verify nullable map property has both object and null types
        // language=json
        schema shouldEqualJson
            $$"""
            {
              "$id": "kotlinx.schema.integration.User",
              "$defs": {
                "kotlinx.schema.integration.User": {
                  "type": "object",
                  "properties": {
                    "name": { "type": "string", "description": "The name of the user" },
                    "age": { "type": ["integer", "null"] },
                    "email": { "type": "string" },
                    "tags": { "type": "array", "items": { "type": "string" } },
                    "attributes": {
                      "type": ["object", "null"],
                      "additionalProperties": {
                        "type": "integer"
                      }
                    }
                  },
                  "required": ["name", "age", "tags", "attributes"],
                  "additionalProperties": false,
                  "description": "A user model"
                }
              },
              "$ref": "#/$defs/kotlinx.schema.integration.User"
            }
            """.trimIndent()
    }

    /**
     * Tests nested property type resolution
     */
    @Test
    fun `Should resolve nested property types correctly`() {
        val schema = User::class.jsonSchemaString

        // Verify List<String> and Map<String, Int> are properly resolved
        // language=json
        schema shouldEqualJson
            $$"""
            {
              "$id": "kotlinx.schema.integration.User",
              "$defs": {
                "kotlinx.schema.integration.User": {
                  "type": "object",
                  "properties": {
                    "name": { "type": "string", "description": "The name of the user" },
                    "age": { "type": ["integer", "null"] },
                    "email": { "type": "string" },
                    "tags": {
                      "type": "array",
                      "items": {
                        "type": "string"
                      }
                    },
                    "attributes": {
                      "type": ["object", "null"],
                      "additionalProperties": {
                        "type": "integer"
                      }
                    }
                  },
                  "required": ["name", "age", "tags", "attributes"],
                  "additionalProperties": false,
                  "description": "A user model"
                }
              },
              "$ref": "#/$defs/kotlinx.schema.integration.User"
            }
            """.trimIndent()
    }

    /**
     * Tests required field logic for properties with and without defaults
     */
    @Test
    fun `Should correctly identify required vs optional fields`() {
        val schema = User::class.jsonSchemaString

        // Verify required includes properties without defaults, excludes properties with defaults
        // language=json
        schema shouldEqualJson
            $$"""
            {
              "$id": "kotlinx.schema.integration.User",
              "$defs": {
                "kotlinx.schema.integration.User": {
                  "type": "object",
                  "properties": {
                    "name": { "type": "string", "description": "The name of the user" },
                    "age": { "type": ["integer", "null"] },
                    "email": { "type": "string" },
                    "tags": { "type": "array", "items": { "type": "string" } },
                    "attributes": {
                      "type": ["object", "null"],
                      "additionalProperties": { "type": "integer" }
                    }
                  },
                  "required": ["name", "age", "tags", "attributes"],
                  "additionalProperties": false,
                  "description": "A user model"
                }
              },
              "$ref": "#/$defs/kotlinx.schema.integration.User"
            }
            """.trimIndent()
    }

    /**
     * Tests all primitive types are handled correctly
     */
    @Test
    fun `Should handle all primitive types`() {
        // Verify String, Int, Long, Double, Boolean map to correct JSON Schema types
        val personSchema = Person::class.jsonSchemaString
        // language=json
        personSchema shouldEqualJson
            $$"""
            {
              "$id": "kotlinx.schema.integration.Person",
              "$defs": {
                "kotlinx.schema.integration.Person": {
                  "type": "object",
                  "properties": {
                    "firstName": { "type": "string", "description": "Given name of the person" },
                    "lastName": { "type": "string", "description": "Family name of the person" },
                    "age": { "type": "integer", "description": "Age of the person in years" }
                  },
                  "required": ["firstName", "lastName", "age"],
                  "additionalProperties": false,
                  "description": "A person with a first and last name and age."
                }
              },
              "$ref": "#/$defs/kotlinx.schema.integration.Person"
            }
            """.trimIndent()

        val productSchema = Product::class.jsonSchemaString
        // language=json
        productSchema shouldEqualJson
            $$"""
            {
              "$id": "kotlinx.schema.integration.Product",
              "$defs": {
                "kotlinx.schema.integration.Product": {
                  "type": "object",
                  "properties": {
                    "id": { "type": "integer", "description": "Unique identifier for the product" },
                    "name": { "type": "string", "description": "Human-readable product name" },
                    "description": { "type": ["string", "null"], "description": "Optional detailed description of the product" },
                    "price": { "type": "number", "description": "Unit price expressed as a decimal number" },
                    "inStock": { "type": "boolean", "description": "Whether the product is currently in stock" },
                    "tags": { "type": "array", "items": { "type": "string" }, "description": "List of tags for categorization and search" }
                  },
                  "required": ["id", "name", "description", "price"],
                  "additionalProperties": false,
                  "description": "A purchasable product with pricing and inventory info."
                }
              },
              "$ref": "#/$defs/kotlinx.schema.integration.Product"
            }
            """.trimIndent()
    }

    /**
     * Tests complex nested structures with multiple levels
     */
    @Test
    fun `Should handle complex nested structures`() {
        val schema = Order::class.jsonSchemaString

        // Verify all nested types are in $defs and Order references them via $ref
        // language=json
        schema shouldEqualJson
            $$"""
            {
              "$id": "kotlinx.schema.integration.Order",
              "$defs": {
                "kotlinx.schema.integration.Address": {
                  "type": "object",
                  "properties": {
                    "street": { "type": "string", "description": "Street address, including house number" },
                    "city": { "type": "string", "description": "City or town name" },
                    "zipCode": { "type": "string", "description": "Postal or ZIP code" },
                    "country": { "type": "string", "description": "Two-letter ISO country code; defaults to US" }
                  },
                  "required": ["street", "city", "zipCode"],
                  "additionalProperties": false,
                  "description": "A postal address for deliveries and billing."
                },
                "kotlinx.schema.integration.Order": {
                  "type": "object",
                  "properties": {
                    "id": { "type": "string", "description": "Unique order identifier" },
                    "customer": { "$ref": "#/$defs/kotlinx.schema.integration.Person", "description": "The customer who placed the order" },
                    "shippingAddress": { "$ref": "#/$defs/kotlinx.schema.integration.Address", "description": "Destination address for shipment" },
                    "items": {
                      "type": "array",
                      "items": { "$ref": "#/$defs/kotlinx.schema.integration.Product" },
                      "description": "List of items included in the order"
                    },
                    "status": { "$ref": "#/$defs/kotlinx.schema.integration.Status", "description": "Current status of the order" }
                  },
                  "required": ["id", "customer", "shippingAddress", "items", "status"],
                  "additionalProperties": false,
                  "description": "An order placed by a customer containing multiple items."
                },
                "kotlinx.schema.integration.Person": {
                  "type": "object",
                  "properties": {
                    "firstName": { "type": "string", "description": "Given name of the person" },
                    "lastName": { "type": "string", "description": "Family name of the person" },
                    "age": { "type": "integer", "description": "Age of the person in years" }
                  },
                  "required": ["firstName", "lastName", "age"],
                  "additionalProperties": false,
                  "description": "A person with a first and last name and age."
                },
                "kotlinx.schema.integration.Product": {
                  "type": "object",
                  "properties": {
                    "id": { "type": "integer", "description": "Unique identifier for the product" },
                    "name": { "type": "string", "description": "Human-readable product name" },
                    "description": { "type": ["string", "null"], "description": "Optional detailed description of the product" },
                    "price": { "type": "number", "description": "Unit price expressed as a decimal number" },
                    "inStock": { "type": "boolean", "description": "Whether the product is currently in stock" },
                    "tags": { "type": "array", "items": { "type": "string" }, "description": "List of tags for categorization and search" }
                  },
                  "required": ["id", "name", "description", "price"],
                  "additionalProperties": false,
                  "description": "A purchasable product with pricing and inventory info."
                },
                "kotlinx.schema.integration.Status": {
                  "description": "Current lifecycle status of an entity.",
                  "type": "string",
                  "enum": ["ACTIVE", "INACTIVE", "PENDING"]
                }
              },
              "$ref": "#/$defs/kotlinx.schema.integration.Order"
            }
            """.trimIndent()
    }

    /**
     * Tests list properties within nested objects
     */
    @Test
    fun `Should handle list properties in nested objects`() {
        val schema = Order::class.jsonSchemaString

        // Verify List<Product> has array type with $ref to Product
        // language=json
        schema shouldEqualJson
            $$"""
            {
              "$id": "kotlinx.schema.integration.Order",
              "$defs": {
                "kotlinx.schema.integration.Address": {
                  "type": "object",
                  "properties": {
                    "street": { "type": "string", "description": "Street address, including house number" },
                    "city": { "type": "string", "description": "City or town name" },
                    "zipCode": { "type": "string", "description": "Postal or ZIP code" },
                    "country": { "type": "string", "description": "Two-letter ISO country code; defaults to US" }
                  },
                  "required": ["street", "city", "zipCode"],
                  "additionalProperties": false,
                  "description": "A postal address for deliveries and billing."
                },
                "kotlinx.schema.integration.Order": {
                  "type": "object",
                  "properties": {
                    "id": { "type": "string", "description": "Unique order identifier" },
                    "customer": { "$ref": "#/$defs/kotlinx.schema.integration.Person", "description": "The customer who placed the order" },
                    "shippingAddress": { "$ref": "#/$defs/kotlinx.schema.integration.Address", "description": "Destination address for shipment" },
                    "items": {
                      "type": "array",
                      "items": { "$ref": "#/$defs/kotlinx.schema.integration.Product" },
                      "description": "List of items included in the order"
                    },
                    "status": { "$ref": "#/$defs/kotlinx.schema.integration.Status", "description": "Current status of the order" }
                  },
                  "required": ["id", "customer", "shippingAddress", "items", "status"],
                  "additionalProperties": false,
                  "description": "An order placed by a customer containing multiple items."
                },
                "kotlinx.schema.integration.Person": {
                  "type": "object",
                  "properties": {
                    "firstName": { "type": "string", "description": "Given name of the person" },
                    "lastName": { "type": "string", "description": "Family name of the person" },
                    "age": { "type": "integer", "description": "Age of the person in years" }
                  },
                  "required": ["firstName", "lastName", "age"],
                  "additionalProperties": false,
                  "description": "A person with a first and last name and age."
                },
                "kotlinx.schema.integration.Product": {
                  "type": "object",
                  "properties": {
                    "id": { "type": "integer", "description": "Unique identifier for the product" },
                    "name": { "type": "string", "description": "Human-readable product name" },
                    "description": { "type": ["string", "null"], "description": "Optional detailed description of the product" },
                    "price": { "type": "number", "description": "Unit price expressed as a decimal number" },
                    "inStock": { "type": "boolean", "description": "Whether the product is currently in stock" },
                    "tags": { "type": "array", "items": { "type": "string" }, "description": "List of tags for categorization and search" }
                  },
                  "required": ["id", "name", "description", "price"],
                  "additionalProperties": false,
                  "description": "A purchasable product with pricing and inventory info."
                },
                "kotlinx.schema.integration.Status": {
                  "description": "Current lifecycle status of an entity.",
                  "type": "string",
                  "enum": ["ACTIVE", "INACTIVE", "PENDING"]
                }
              },
              "$ref": "#/$defs/kotlinx.schema.integration.Order"
            }
            """.trimIndent()
    }

    /**
     * Tests maps with object value types
     */
    @Test
    fun `Should handle maps with complex value types`() {
        val schema = User::class.jsonSchemaString

        // Verify Map<String, Int> has object type with integer additionalProperties
        // language=json
        schema shouldEqualJson
            $$"""
            {
              "$id": "kotlinx.schema.integration.User",
              "$defs": {
                "kotlinx.schema.integration.User": {
                  "type": "object",
                  "properties": {
                    "name": { "type": "string", "description": "The name of the user" },
                    "age": { "type": ["integer", "null"] },
                    "email": { "type": "string" },
                    "tags": { "type": "array", "items": { "type": "string" } },
                    "attributes": {
                      "type": ["object", "null"],
                      "additionalProperties": {
                        "type": "integer"
                      }
                    }
                  },
                  "required": ["name", "age", "tags", "attributes"],
                  "additionalProperties": false,
                  "description": "A user model"
                }
              },
              "$ref": "#/$defs/kotlinx.schema.integration.User"
            }
            """.trimIndent()
    }

    /**
     * Tests generic type parameter resolution
     */
    @Test
    fun `Should resolve generic type parameters`() {
        val schema = Container::class.jsonSchemaString

        // Verify generic type T resolves to kotlin.Any
        // language=json
        schema shouldEqualJson
            $$"""
            {
              "$id": "kotlinx.schema.integration.Container",
              "$defs": {
                "kotlin.Any": {
                  "type": "object",
                  "properties": {},
                  "required": [],
                  "additionalProperties": false
                },
                "kotlinx.schema.integration.Container": {
                  "type": "object",
                  "properties": {
                    "content": {
                      "$ref": "#/$defs/kotlin.Any",
                      "description": "The wrapped content value"
                    },
                    "metadata": {
                      "type": "object",
                      "additionalProperties": {
                        "$ref": "#/$defs/kotlin.Any"
                      },
                      "description": "Arbitrary metadata key-value pairs"
                    }
                  },
                  "required": ["content"],
                  "additionalProperties": false,
                  "description": "A generic container that wraps content with optional metadata."
                }
              },
              "$ref": "#/$defs/kotlinx.schema.integration.Container"
            }
            """.trimIndent()
    }
}
