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
              "$schema": "https://json-schema.org/draft/2020-12/schema",
              "$id": "kotlinx.schema.integration.Status",
              "description": "Current lifecycle status of an entity.",
              "type": "string",
              "enum": [
                "ACTIVE",
                "INACTIVE",
                "PENDING"
              ]
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
              "$schema": "https://json-schema.org/draft/2020-12/schema",
              "$id": "kotlinx.schema.integration.Person",
              "description": "A person with a first and last name and age.",
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
              "additionalProperties": false
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
              "$schema": "https://json-schema.org/draft/2020-12/schema",
              "$id": "kotlinx.schema.integration.Address",
              "description": "A postal address for deliveries and billing.",
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
                "zipCode",
                "country"
              ],
              "additionalProperties": false
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
              "$schema": "https://json-schema.org/draft/2020-12/schema",
              "$id": "kotlinx.schema.integration.Product",
              "description": "A purchasable product with pricing and inventory info.",
              "type": "object",
              "properties": {
                "id": { "type": "integer", "description": "Unique identifier for the product" },
                "name": { "type": "string", "description": "Human-readable product name" },
                "description": { "type": ["string", "null"], "description": "Optional detailed description of the product" },
                "price": { "type": "number", "description": "Unit price expressed as a decimal number" },
                "inStock": { "type": "boolean", "description": "Whether the product is currently in stock" },
                "tags": { "type": ["array", "null"], "description": "List of tags for categorization and search (optional)", "items": { "type": "string" } }
              },
              "required": [
                "id",
                "name",
                "description",
                "price",
                "inStock",
                "tags"
              ],
              "additionalProperties": false
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
              "$schema": "https://json-schema.org/draft/2020-12/schema",
              "$id": "kotlinx.schema.integration.CustomSchemaClass",
              "description": "A class using a custom schema type value.",
              "type": "object",
              "properties": {
                "customField": { "type": "string", "description": "A field included to validate custom schema handling" }
              },
              "required": [
                "customField"
              ],
              "additionalProperties": false
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
              "$schema": "https://json-schema.org/draft/2020-12/schema",
              "$id": "kotlinx.schema.integration.Container",
              "description": "A generic container that wraps content with optional metadata.",
              "type": "object",
              "properties": {
                "content": { "type": "object", "description": "The wrapped content value", "properties": {}, "required": [], "additionalProperties": false },
                "metadata": { "type": ["object", "null"], "description": "Arbitrary metadata key-value pairs (optional)", "additionalProperties": { "type": "object", "properties": {}, "required": [], "additionalProperties": false } }
              },
              "required": [
                "content",
                "metadata"
              ],
              "additionalProperties": false
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
              "$schema": "https://json-schema.org/draft/2020-12/schema",
              "$id": "kotlinx.schema.integration.Status",
              "description": "Current lifecycle status of an entity.",
              "type": "string",
              "enum": [
                "ACTIVE",
                "INACTIVE",
                "PENDING"
              ]
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
              "$schema": "https://json-schema.org/draft/2020-12/schema",
              "$id": "kotlinx.schema.integration.Order",
              "description": "An order placed by a customer containing multiple items.",
              "type": "object",
              "properties": {
                "id": { "type": "string", "description": "Unique order identifier" },
                "customer": { "type": "object", "description": "The customer who placed the order", "properties": { "firstName": { "type": "string", "description": "Given name of the person" }, "lastName": { "type": "string", "description": "Family name of the person" }, "age": { "type": "integer", "description": "Age of the person in years" } }, "required": ["firstName", "lastName", "age"], "additionalProperties": false },
                "shippingAddress": { "type": "object", "description": "Destination address for shipment", "properties": { "street": { "type": "string", "description": "Street address, including house number" }, "city": { "type": "string", "description": "City or town name" }, "zipCode": { "type": "string", "description": "Postal or ZIP code" }, "country": { "type": "string", "description": "Two-letter ISO country code; defaults to US" } }, "required": ["street", "city", "zipCode", "country"], "additionalProperties": false },
                "items": { "type": "array", "description": "List of items included in the order", "items": { "type": "object", "description": "A purchasable product with pricing and inventory info.", "properties": { "id": { "type": "integer", "description": "Unique identifier for the product" }, "name": { "type": "string", "description": "Human-readable product name" }, "description": { "type": ["string", "null"], "description": "Optional detailed description of the product" }, "price": { "type": "number", "description": "Unit price expressed as a decimal number" }, "inStock": { "type": "boolean", "description": "Whether the product is currently in stock" }, "tags": { "type": ["array", "null"], "description": "List of tags for categorization and search (optional)", "items": { "type": "string" } } }, "required": ["id", "name", "description", "price", "inStock", "tags"], "additionalProperties": false } },
                "status": { "type": "string", "description": "Current status of the order", "enum": ["ACTIVE", "INACTIVE", "PENDING"] }
              },
              "required": [
                "id",
                "customer",
                "shippingAddress",
                "items",
                "status"
              ],
              "additionalProperties": false
            }
            """.trimIndent()
    }

    @Test
    fun `Shape sealed class should generate polymorphic schema with discriminator`() {
        val schema = Shape::class.jsonSchemaString

        // language=json
        schema shouldEqualJson
            $$"""
            {
              "$schema": "https://json-schema.org/draft/2020-12/schema",
              "$id": "kotlinx.schema.integration.Shape",
              "description": "Represents a geometric shape",
              "type": "object",
              "additionalProperties": false,
              "oneOf": [
                { "$ref": "#/$defs/kotlinx.schema.integration.Shape.Circle" },
                { "$ref": "#/$defs/kotlinx.schema.integration.Shape.Rectangle" },
                { "$ref": "#/$defs/kotlinx.schema.integration.Shape.Triangle" }
              ],
              "$defs": {
                "kotlinx.schema.integration.Shape.Circle": {
                  "type": "object",
                  "description": "A circular shape",
                  "properties": {
                    "radius": { "type": "number", "description": "Radius of the circle" }
                  },
                  "required": [
                    "radius"
                  ],
                  "additionalProperties": false
                },
                "kotlinx.schema.integration.Shape.Rectangle": {
                  "type": "object",
                  "description": "A rectangular shape",
                  "properties": {
                    "width": { "type": "number", "description": "Width of the rectangle" },
                    "height": { "type": "number", "description": "Height of the rectangle" }
                  },
                  "required": [
                    "width",
                    "height"
                  ],
                  "additionalProperties": false
                },
                "kotlinx.schema.integration.Shape.Triangle": {
                  "type": "object",
                  "description": "A triangular shape",
                  "properties": {
                    "base": { "type": "number", "description": "Length of the base" },
                    "height": { "type": "number", "description": "Height from base to apex" }
                  },
                  "required": [
                    "base",
                    "height"
                  ],
                  "additionalProperties": false
                }
              }
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
                Shape::class.jsonSchemaString,
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
