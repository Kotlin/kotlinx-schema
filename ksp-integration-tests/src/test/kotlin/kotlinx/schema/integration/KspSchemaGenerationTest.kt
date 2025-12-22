@file:Suppress("JsonStandardCompliance")

package kotlinx.schema.integration

import io.kotest.assertions.json.shouldEqualJson
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
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
 * Comprehensive integration tests that verify KSP-generated schemas for all supported scenarios
 */
@Suppress("LongMethod")
class KspSchemaGenerationTest {
    // ========================================
    // Basic Data Classes
    // ========================================

    @Test
    fun `Should generate schema for simple data class with primitives`() {
        val schema = Person::class.jsonSchemaString

        // language=json
        schema shouldEqualJson
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
    }

    @Test
    fun `Should generate schema for data class with default values`() {
        val schema = Address::class.jsonSchemaString

        // Verify properties with defaults are excluded from required
        // language=json
        schema shouldEqualJson
            $$"""
            {
              "$id": "kotlinx.schema.integration.Address",
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
                }
              },
              "$ref": "#/$defs/kotlinx.schema.integration.Address"
            }
            """.trimIndent()
    }

    @Test
    fun `Should generate schema for data class with nullable properties`() {
        val schema = Product::class.jsonSchemaString

        // language=json
        schema shouldEqualJson
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

    // ========================================
    // Enums
    // ========================================

    @Test
    fun `Should generate schema for enum class`() {
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

    @Test
    fun `Enum schema should have all enum values`() {
        val schema = Status::class.jsonSchemaString

        // Verify enum contains all values
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

    // ========================================
    // Collections
    // ========================================

    @Test
    fun `Should generate schema for data class with List property`() {
        val schema = Product::class.jsonSchemaString

        // Verify List<String> property generates array type with string items
        // language=json
        schema shouldEqualJson
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
                    "tags": {
                      "type": "array",
                      "items": { "type": "string" },
                      "description": "List of tags for categorization and search"
                    }
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

    @Test
    fun `Should generate schema for data class with Map property`() {
        val schema = Container::class.jsonSchemaString

        // Verify Map<String, T> property generates object type with additionalProperties
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
                    "content": { "$ref": "#/$defs/kotlin.Any", "description": "The wrapped content value" },
                    "metadata": {
                      "type": "object",
                      "additionalProperties": { "$ref": "#/$defs/kotlin.Any" },
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

    // ========================================
    // Nested Objects
    // ========================================

    @Test
    fun `Should generate schema for nested objects with $defs`() {
        val schema = Order::class.jsonSchemaString

        // Verify nested types are in $defs and referenced via $ref
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

    @Test
    fun `Nested schema should not duplicate type definitions`() {
        val schema = Order::class.jsonSchemaString

        // Verify each type appears exactly once in $defs
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

    // ========================================
    // Generics
    // ========================================

    @Test
    fun `Should generate schema for generic class with type parameter`() {
        val schema = Container::class.jsonSchemaString

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
                    "content": { "$ref": "#/$defs/kotlin.Any", "description": "The wrapped content value" },
                    "metadata": { "type": "object", "additionalProperties": { "$ref": "#/$defs/kotlin.Any" }, "description": "Arbitrary metadata key-value pairs" }
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

    @Test
    fun `Generic type parameters should resolve to kotlin-Any`() {
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
                    "content": { "$ref": "#/$defs/kotlin.Any", "description": "The wrapped content value" },
                    "metadata": { "type": "object", "additionalProperties": { "$ref": "#/$defs/kotlin.Any" }, "description": "Arbitrary metadata key-value pairs" }
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

    // ========================================
    // Sealed Classes (Polymorphism)
    // ========================================

    @Test
    fun `Should generate polymorphic schema for sealed class with oneOf`() {
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

    @Test
    fun `Sealed class schema should have all subtype definitions in $defs`() {
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

    @Test
    fun `Sealed class subtypes should have type discriminator property`() {
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

    // ========================================
    // JsonObject Generation (withSchemaObject)
    // ========================================

    @Test
    fun `Should generate both jsonSchemaString and jsonSchema when withSchemaObject is true`() {
        Person::class.jsonSchemaString shouldNotBe null
        Person::class.jsonSchema shouldNotBe null
    }

    @Test
    fun `jsonSchema should parse jsonSchemaString correctly`() {
        val schemaString = Person::class.jsonSchemaString
        val schemaObject = Person::class.jsonSchema

        schemaObject shouldNotBeNull {
            this shouldBeEqual Json.decodeFromString<JsonObject>(schemaString)
        }
    }

    @Test
    fun `Order with withSchemaObject should have both string and object schemas`() {
        Order::class.jsonSchemaString shouldNotBe null
        Order::class.jsonSchema shouldNotBe null

        val schemaObject = Order::class.jsonSchema
        schemaObject shouldNotBeNull {
            val id = this["\$id"]?.toString()
            id shouldContain "kotlinx.schema.integration.Order"
        }
    }

    // ========================================
    // Schema Validation
    // ========================================

    @Test
    fun `Generated schemas should be valid JSON`() {
        val schemas =
            listOf(
                Person::class.jsonSchemaString,
                Address::class.jsonSchemaString,
                Product::class.jsonSchemaString,
                Status::class.jsonSchemaString,
                Container::class.jsonSchemaString,
                Order::class.jsonSchemaString,
                Animal::class.jsonSchemaString,
            )

        schemas.forEach { schema ->
            // Should be valid JSON
            val trimmed = schema.trim()
            trimmed.first() shouldBe '{'
            trimmed.last() shouldBe '}'

            // Should parse without errors
            val jsonObj = Json.decodeFromString<JsonObject>(schema)
            jsonObj shouldNotBe null
        }
    }

    @Test
    fun `All schemas should have required JSON Schema fields`() {
        val schemas =
            listOf(
                Person::class.jsonSchemaString,
                Address::class.jsonSchemaString,
                Product::class.jsonSchemaString,
                Order::class.jsonSchemaString,
            )

        schemas.forEach { schema ->
            schema shouldContain """"${'$'}id":"""
            schema shouldContain """"${'$'}defs":"""
            schema shouldContain """"${'$'}ref":"""
        }
    }

    @Test
    fun `Schemas should not contain Kotlin-specific syntax`() {
        val schemas =
            listOf(
                Person::class.jsonSchemaString,
                Address::class.jsonSchemaString,
                Product::class.jsonSchemaString,
                Order::class.jsonSchemaString,
            )

        schemas.forEach { schema ->
            schema shouldNotContain "data class"
            schema shouldNotContain "fun "
            schema shouldNotContain "val "
            schema shouldNotContain "var "
            schema shouldNotContain "sealed class"
        }
    }

    @Test
    fun `All annotated classes should have unique schema IDs`() {
        val personSchema = Person::class.jsonSchemaString
        val addressSchema = Address::class.jsonSchemaString
        val productSchema = Product::class.jsonSchemaString

        personSchema shouldContain "kotlinx.schema.integration.Person"
        addressSchema shouldContain "kotlinx.schema.integration.Address"
        productSchema shouldContain "kotlinx.schema.integration.Product"

        // Schemas should be distinct
        (personSchema == addressSchema) shouldBe false
        (personSchema == productSchema) shouldBe false
        (addressSchema == productSchema) shouldBe false
    }

    @Test
    fun `Schema sizes should be reasonable`() {
        val schemas =
            listOf(
                Person::class.jsonSchemaString,
                Address::class.jsonSchemaString,
                Product::class.jsonSchemaString,
            )

        schemas.forEach { schema ->
            schema.length shouldBeGreaterThan 50
        }
    }

    // ========================================
    // Descriptions and Annotations
    // ========================================

    @Test
    fun `Class descriptions should be included in schema`() {
        val personSchema = Person::class.jsonSchemaString
        personSchema shouldContain "A person with a first and last name and age."

        val productSchema = Product::class.jsonSchemaString
        productSchema shouldContain "A purchasable product with pricing and inventory info."
    }

    @Test
    fun `Property descriptions should be included in schema`() {
        val personSchema = Person::class.jsonSchemaString

        personSchema shouldContain "Given name of the person"
        personSchema shouldContain "Family name of the person"
        personSchema shouldContain "Age of the person in years"
    }

    @Test
    fun `Enum descriptions should be included in schema`() {
        val statusSchema = Status::class.jsonSchemaString
        statusSchema shouldContain "Current lifecycle status of an entity."
    }

    // ========================================
    // Non-Annotated Classes
    // ========================================

    @Test
    fun `NonAnnotatedClass should not have generated extensions`() {
        // This verifies that KSP correctly skips non-annotated classes
        // If extensions were generated, this would fail to compile
        val clazz = NonAnnotatedClass::class
        clazz shouldNotBe null
    }

    // ========================================
    // Edge Cases
    // ========================================

    @Test
    fun `Schema with all primitive types should be valid`() {
        val personSchema = Person::class.jsonSchemaString

        personSchema shouldContain "\"type\": \"string\"" // firstName, lastName
        personSchema shouldContain "\"type\": \"integer\"" // age
    }

    @Test
    fun `Schema with all numeric types should be valid`() {
        val productSchema = Product::class.jsonSchemaString

        productSchema shouldContain "\"type\": \"integer\"" // id (Long)
        productSchema shouldContain "\"type\": \"number\"" // price (Double)
    }

    @Test
    fun `Schema with boolean type should be valid`() {
        val productSchema = Product::class.jsonSchemaString
        productSchema shouldContain "\"type\": \"boolean\"" // inStock
    }

    @Test
    fun `Required array should contain required fields`() {
        // Container has one required field (content)
        val containerSchema = Container::class.jsonSchemaString
        containerSchema shouldContain "\"content\""
        containerSchema shouldContain "\"required\""
    }
}
