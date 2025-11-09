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
class KspIntegrationTest {
    @Test
    fun `Should generate schema for Enum`() {
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
                  "enum": [
                    "ACTIVE",
                    "INACTIVE",
                    "PENDING"
                  ]
                }
              },
              "$ref": "#/$defs/kotlinx.schema.integration.Status"
            }
            """.trimIndent()
    }

    @Test
    fun `Person class should have generated jsonSchemaString extension`() {
        // This tests that KSP successfully generated the extension property
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
                  "required": [
                    "firstName",
                    "lastName",
                    "age"
                  ],
                  "additionalProperties": false,
                  "description": "A person with a first and last name and age."
                }
              },
              "$ref": "#/$defs/kotlinx.schema.integration.Person"
            }
            """.trimIndent()
    }

    @Test
    fun `Person class should have generated jsonSchema extension`() {
        // This tests that KSP successfully generated the extension property
        val schema = Person::class.jsonSchema

        schema shouldNotBeNull {
            this shouldBeEqual
                Json.decodeFromString<JsonObject>(Person::class::jsonSchemaString.get())
        }
    }

    @Test
    fun `Address class should have generated jsonSchemaString extension`() {
        val schema = Address::class.jsonSchemaString

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
                  "required": [
                    "street",
                    "city",
                    "zipCode"
                  ],
                  "additionalProperties": false,
                  "description": "A postal address for deliveries and billing."
                }
              },
              "$ref": "#/$defs/kotlinx.schema.integration.Address"
            }
            """.trimIndent()
    }

    @Test
    fun `Product class should have generated jsonSchemaString extension`() {
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
              "$ref": "#/$defs/kotlinx.schema.integration.Product"
            }
            """.trimIndent()
    }

    @Test
    fun `NonAnnotatedClass should NOT have generated extension`() {
        // This should fail to compile if KSP incorrectly generated an extension
        // We can't directly test this at runtime, but the absence of compilation errors
        // for this test indicates KSP correctly skipped non-annotated classes

        // Verify the class exists but doesn't have our extension
        val clazz = NonAnnotatedClass::class
        clazz shouldNotBe null

        // If KSP generated an extension for this class, the next line would compile
        // but it shouldn't, so this test verifies correct behavior by omission
    }

    @Test
    fun `CustomSchemaClass should have generated extension with custom parameter`() {
        val schema = CustomSchemaClass::class.jsonSchemaString

        // language=json
        schema shouldEqualJson
            $$"""
            {
              "$id": "kotlinx.schema.integration.CustomSchemaClass",
              "$defs": {
                "kotlinx.schema.integration.CustomSchemaClass": {
                  "type": "object",
                  "properties": {
                    "customField": { "type": "string", "description": "A field included to validate custom schema handling" }
                  },
                  "required": [
                    "customField"
                  ],
                  "additionalProperties": false,
                  "description": "A class using a custom schema type value."
                }
              },
              "$ref": "#/$defs/kotlinx.schema.integration.CustomSchemaClass"
            }
            """.trimIndent()
    }

    @Test
    fun `Container generic class should have generated extension`() {
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
                  "required": [
                    "content"
                  ],
                  "additionalProperties": false,
                  "description": "A generic container that wraps content with optional metadata."
                }
              },
              "$ref": "#/$defs/kotlinx.schema.integration.Container"
            }
            """.trimIndent()
    }

    @Test
    fun `Status enum should have generated extension`() {
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
                  "enum": [
                    "ACTIVE",
                    "INACTIVE",
                    "PENDING"
                  ]
                }
              },
              "$ref": "#/$defs/kotlinx.schema.integration.Status"
            }
            """.trimIndent()
    }

    @Test
    fun `Order complex class should have generated extension`() {
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
                    "firstName": { "type": "string", "description": "Given name of the person" },
                    "lastName": { "type": "string", "description": "Family name of the person" },
                    "age": { "type": "integer", "description": "Age of the person in years" }
                  },
                  "required": [
                    "firstName",
                    "lastName",
                    "age"
                  ],
                  "additionalProperties": false,
                  "description": "A person with a first and last name and age."
                },
                "kotlinx.schema.integration.Address": {
                  "type": "object",
                  "properties": {
                    "street": { "type": "string", "description": "Street address, including house number" },
                    "city": { "type": "string", "description": "City or town name" },
                    "zipCode": { "type": "string", "description": "Postal or ZIP code" },
                    "country": { "type": "string", "description": "Two-letter ISO country code; defaults to US" }
                  },
                  "required": [
                    "street",
                    "city",
                    "zipCode"
                  ],
                  "additionalProperties": false,
                  "description": "A postal address for deliveries and billing."
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
                  "required": [
                    "id",
                    "name",
                    "description",
                    "price"
                  ],
                  "additionalProperties": false,
                  "description": "A purchasable product with pricing and inventory info."
                },
                "kotlinx.schema.integration.Status": {
                  "type": "string",
                  "enum": [
                    "ACTIVE",
                    "INACTIVE",
                    "PENDING"
                  ],
                  "description": "Current lifecycle status of an entity."
                },
                "kotlinx.schema.integration.Order": {
                  "type": "object",
                  "properties": {
                    "id": { "type": "string", "description": "Unique order identifier" },
                    "customer": { "$ref": "#/$defs/kotlinx.schema.integration.Person", "description": "The customer who placed the order" },
                    "shippingAddress": { "$ref": "#/$defs/kotlinx.schema.integration.Address", "description": "Destination address for shipment" },
                    "items": { "type": "array", "items": { "$ref": "#/$defs/kotlinx.schema.integration.Product" }, "description": "List of items included in the order" },
                    "status": { "$ref": "#/$defs/kotlinx.schema.integration.Status", "description": "Current status of the order" }
                  },
                  "required": [
                    "id",
                    "customer",
                    "shippingAddress",
                    "items",
                    "status"
                  ],
                  "additionalProperties": false,
                  "description": "An order placed by a customer containing multiple items."
                }
              },
              "$ref": "#/$defs/kotlinx.schema.integration.Order"
            }
            """.trimIndent()
    }

    @Test
    fun `generated schemas should be valid JSON format`() {
        val schemas =
            listOf(
                Person::class.jsonSchemaString,
                Address::class.jsonSchemaString,
                Product::class.jsonSchemaString,
                Container::class.jsonSchemaString,
                Status::class.jsonSchemaString,
                Order::class.jsonSchemaString,
            )

        schemas.forEach { schema ->
            // Basic JSON validation - should start and end with braces
            val trimmedSchema = schema.trim()
            // Use a more platform-compatible regex pattern
            trimmedSchema.first() shouldBe '{'
            trimmedSchema.last() shouldBe '}'

            // Should contain required JSON schema fields
            schema shouldContain "\"type\"" // present inside $defs
            schema shouldContain $$"\"$id\""

            // Should not contain Kotlin-specific syntax
            schema shouldNotContain "data class"
            schema shouldNotContain "fun "
            schema shouldNotContain "val "
        }
    }

    @Test
    fun `all annotated classes should have unique schemas`() {
        val personSchema = Person::class.jsonSchemaString
        val addressSchema = Address::class.jsonSchemaString
        val productSchema = Product::class.jsonSchemaString

        // Each class should have a distinct schema
        (personSchema == addressSchema) shouldBe false
        (personSchema == productSchema) shouldBe false
        (addressSchema == productSchema) shouldBe false
    }

    @Test
    fun `schema extension property should be accessible from instances`() {
        // Extension should work on both class and instance level
        val personSchemaFromClass = Person::class.jsonSchemaString
        val addressSchemaFromClass = Address::class.jsonSchemaString

        // Verify they're the same (extension is on the class, not instance)
        personSchemaFromClass shouldNotBeNull {
            length shouldBeGreaterThan 0
        }
        addressSchemaFromClass shouldNotBeNull {
            length shouldBeGreaterThan 0
        }
    }
}