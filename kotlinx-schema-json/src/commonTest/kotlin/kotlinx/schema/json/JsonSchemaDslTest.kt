package kotlinx.schema.json

import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.Json
import kotlin.test.Test

/**
 * Tests for JSON Schema round-trip serialization.
 *
 * These tests verify that schemas can be:
 * 1. Serialized to JSON strings
 * 2. Match expected JSON format (using shouldEqualJson)
 * 3. Deserialized back to equivalent objects
 */
@Suppress("LargeClass", "LongMethod")
internal class JsonSchemaDslTest {
    private val json = Json { prettyPrint = true }

    @Test
    fun `simple string schema serialization round-trip`() {
        val schema =
            jsonSchema {
                property("email") {
                    required = true
                    string {
                        description = "Email address"
                        format = "email"
                    }
                }
            }

        // language=json
        val expectedJson =
            """
            {
                "type": "object",
                "properties": {
                  "email": {
                    "type": "string",
                    "description": "Email address",
                    "format": "email"
                  }
                },
                "required": ["email"]
            }
            """.trimIndent()

        val deserialized = serializeAndDeserialize(schema, expectedJson)

        deserialized.required shouldBe schema.required
        deserialized.properties shouldBe schema.properties
    }

    @Test
    fun `schema with string enum serialization round-trip`() {
        val schema =
            jsonSchema {
                property("status") {
                    string {
                        description = "Current status"
                        enum = listOf("active", "inactive", "pending")
                    }
                }
            }

        // language=json
        val expectedJson =
            """
            {
                "type": "object",
                "properties": {
                  "status": {
                    "type": "string",
                    "description": "Current status",
                    "enum": ["active", "inactive", "pending"]
                  }
                }
            }
            """.trimIndent()

        serializeAndDeserialize(schema, expectedJson, json)
    }

    @Test
    fun `schema with numeric constraints serialization round-trip`() {
        val schema =
            jsonSchema {
                property("score") {
                    number {
                        description = "User score"
                        minimum = 0.0
                        maximum = 100.0
                        multipleOf = 0.5
                    }
                }
            }

        // language=json
        val expectedJson =
            """
            {
                "type": "object",
                "properties": {
                  "score": {
                    "type": "number",
                    "description": "User score",
                    "multipleOf": 0.5,
                    "minimum": 0.0,
                    "maximum": 100.0
                  }
                }
            }
            """.trimIndent()

        serializeAndDeserialize(schema, expectedJson, json)
    }

    @Test
    fun `schema with array property serialization round-trip`() {
        val schema =
            jsonSchema {
                description = "Tags"
                property("tags") {
                    array {
                        description = "List of tags"
                        minItems = 1
                        maxItems = 10
                        ofString()
                    }
                }
            }

        // language=json
        val expectedJson =
            """
            {
                "description": "Tags",
                "type": "object",
                "properties": {
                  "tags": {
                    "type": "array",
                    "description": "List of tags",
                    "items": {
                      "type": "string"
                    },
                    "minItems": 1,
                    "maxItems": 10
                  }
                }
            }
            """.trimIndent()

        val deserialized = serializeAndDeserialize(schema, expectedJson, json)
        val tagsProperty = deserialized.properties["tags"] as ArrayPropertyDefinition
        tagsProperty.minItems shouldBe 1
        tagsProperty.maxItems shouldBe 10
    }

    @Test
    fun `schema with nested object serialization round-trip`() {
        val schema =
            jsonSchema {
                property("metadata") {
                    obj {
                        description = "User metadata"
                        property("createdAt") {
                            required = true
                            string {
                                format = "date-time"
                            }
                        }
                        property("updatedAt") {
                            string {
                                format = "date-time"
                            }
                        }
                    }
                }
            }

        // language=json
        val expectedJson =
            """
            {
                "type": "object",
                "properties": {
                  "metadata": {
                    "type": "object",
                    "description": "User metadata",
                    "properties": {
                      "createdAt": {
                        "type": "string",
                        "format": "date-time"
                      },
                      "updatedAt": {
                        "type": "string",
                        "format": "date-time"
                      }
                    },
                    "required": ["createdAt"]
                  }
                }
            }
            """.trimIndent()

        serializeAndDeserialize(schema, expectedJson, json)
    }

    @Test
    fun `schema with reference property serialization round-trip`() {
        val schema =
            jsonSchema {
                property("profile") {
                    reference("#/definitions/Profile")
                }
            }

        // language=json
        val expectedJson =
            $$"""
            {
                "type": "object",
                "properties": {
                  "profile": {
                    "$ref": "#/definitions/Profile"
                  }
                }
            }
            """.trimIndent()

        val deserialized = serializeAndDeserialize(schema, expectedJson, json)

        val profileProperty = deserialized.properties["profile"] as ReferencePropertyDefinition
        profileProperty.ref shouldBe "#/definitions/Profile"
    }

    @Test
    fun `schema with id and schema fields serialization round-trip`() {
        val schema =
            jsonSchema {
                description = "Product"
                id = "https://example.com/schemas/product"
                schema = "https://json-schema.org/draft-07/schema"
                property("name") {
                    string()
                }
            }

        // language=json
        val expectedJson =
            $$"""
            {
              "description": "Product",
                "type": "object",
                "$id": "https://example.com/schemas/product",
                "$schema": "https://json-schema.org/draft-07/schema",
                "properties": {
                  "name": {
                    "type": "string"
                  }
                }
            }
            """.trimIndent()

        val deserialized = serializeAndDeserialize(schema, expectedJson, json)

        deserialized.id shouldBe "https://example.com/schemas/product"
        deserialized.schema shouldBe "https://json-schema.org/draft-07/schema"
    }

    @Test
    fun `schema with default values serialization round-trip`() {
        val schema =
            jsonSchema {
                property("enabled") {
                    boolean {
                        description = "Feature enabled"
                        default = true
                    }
                }
                property("name") {
                    string {
                        description = "Config name"
                        default = "default"
                    }
                }
                property("count") {
                    integer {
                        description = "Item count"
                        default = 10
                    }
                }
            }

        // language=json
        val expectedJson =
            """
            {
                "type": "object",
                "properties": {
                  "enabled": {
                    "type": "boolean",
                    "description": "Feature enabled",
                    "default": true
                  },
                  "name": {
                    "type": "string",
                    "description": "Config name",
                    "default": "default"
                  },
                  "count": {
                    "type": "integer",
                    "description": "Item count",
                    "default": 10
                  }
                }
            }
            """.trimIndent()

        serializeAndDeserialize(schema, expectedJson, json)
    }

    @Test
    fun `schema with nullable property serialization round-trip`() {
        val schema =
            jsonSchema {
                description = "OptionalField"
                property("optional") {
                    string {
                        description = "Optional string field"
                        nullable = true
                    }
                }
            }

        // language=json
        val expectedJson =
            """
            {
                "description": "OptionalField",
                "type": "object",
                "properties": {
                  "optional": {
                    "type": "string",
                    "description": "Optional string field",
                    "nullable": true
                  }
                }
            }
            """.trimIndent()

        serializeAndDeserialize(schema, expectedJson, json)
    }

    @Test
    fun `complex schema serialization round-trip`() {
        val schema =
            jsonSchema {
                description = "A complex schema with various field types"
                additionalProperties = false

                property("id") {
                    required = true
                    string {
                        format = "uuid"
                        description = "Unique identifier"
                    }
                }

                property("email") {
                    required = true
                    string {
                        format = "email"
                        description = "Email address"
                        minLength = 5
                        maxLength = 100
                    }
                }

                property("tags") {
                    array {
                        description = "List of tags"
                        ofString()
                    }
                }

                property("metadata") {
                    obj {
                        description = "Additional metadata"
                        property("version") {
                            integer()
                        }
                    }
                }
            }

        // language=json
        val expectedJson =
            """
            {
                "description": "A complex schema with various field types",
                "type": "object",
                "properties": {
                  "id": {
                    "type": "string",
                    "description": "Unique identifier",
                    "format": "uuid"
                  },
                  "email": {
                    "type": "string",
                    "description": "Email address",
                    "format": "email",
                    "minLength": 5,
                    "maxLength": 100
                  },
                  "tags": {
                    "type": "array",
                    "description": "List of tags",
                    "items": {
                      "type": "string"
                    }
                  },
                  "metadata": {
                    "type": "object",
                    "description": "Additional metadata",
                    "properties": {
                      "version": {
                        "type": "integer"
                      }
                    }
                  }
                },
                "required": ["id", "email"],
                "additionalProperties": false
            }
            """.trimIndent()

        val deserialized = serializeAndDeserialize(schema, expectedJson, json)

        deserialized.description shouldBe schema.description
        deserialized.required shouldBe schema.required
        deserialized.additionalProperties shouldBe schema.additionalProperties
    }

    @Test
    fun `schema with integer property serialization round-trip`() {
        val schema =
            jsonSchema {
                property("age") {
                    integer {
                        description = "Person's age"
                        minimum = 0.0
                        maximum = 150.0
                    }
                }
            }

        // language=json
        val expectedJson =
            """
            {
                "type": "object",
                "properties": {
                  "age": {
                    "type": "integer",
                    "description": "Person's age",
                    "minimum": 0.0,
                    "maximum": 150.0
                  }
                }
            }
            """.trimIndent()

        val deserialized = serializeAndDeserialize(schema, expectedJson, json)

        val ageProp = deserialized.properties["age"] as NumericPropertyDefinition
        ageProp.type shouldBe listOf("integer")
        ageProp.minimum shouldBe 0.0
        ageProp.maximum shouldBe 150.0
    }

    @Test
    fun `schema with array of objects serialization round-trip`() {
        val schema =
            jsonSchema {
                property("steps") {
                    array {
                        description = "Processing steps"
                        ofObject {
                            additionalProperties = kotlinx.serialization.json.JsonPrimitive(false)
                            property("explanation") {
                                required = true
                                string {
                                    description = "Step explanation"
                                }
                            }
                            property("output") {
                                required = true
                                string {
                                    description = "Step output"
                                }
                            }
                        }
                    }
                }
            }

        // language=json
        val expectedJson =
            """
            {
                "type": "object",
                "properties": {
                  "steps": {
                    "type": "array",
                    "description": "Processing steps",
                    "items": {
                      "type": "object",
                      "properties": {
                        "explanation": {
                          "type": "string",
                          "description": "Step explanation"
                        },
                        "output": {
                          "type": "string",
                          "description": "Step output"
                        }
                      },
                      "required": ["explanation", "output"],
                      "additionalProperties": false
                    }
                  }
                }
            }
            """.trimIndent()

        val deserialized = serializeAndDeserialize(schema, expectedJson, json)

        val stepsProp = deserialized.properties["steps"] as ArrayPropertyDefinition
        val itemsObj = stepsProp.items as ObjectPropertyDefinition
        itemsObj.required shouldBe listOf("explanation", "output")
        itemsObj.additionalProperties shouldBe kotlinx.serialization.json.JsonPrimitive(false)
    }

    @Test
    fun `schema with const value serialization round-trip`() {
        val schema =
            jsonSchema {
                description = "ApiVersion"
                property("version") {
                    string {
                        description = "API version"
                        constValue = "v1.0"
                    }
                }
                property("flag") {
                    boolean {
                        description = "Constant flag"
                        constValue = false
                    }
                }
            }

        // language=json
        val expectedJson =
            """
            {
                "description": "ApiVersion",
                "type": "object",
                "properties": {
                  "version": {
                    "type": "string",
                    "description": "API version",
                    "const": "v1.0"
                  },
                  "flag": {
                    "type": "boolean",
                    "description": "Constant flag",
                    "const": false
                  }
                }
            }
            """.trimIndent()

        serializeAndDeserialize(schema, expectedJson, json)
    }

    @Test
    fun `schema with exclusive bounds serialization round-trip`() {
        val schema =
            jsonSchema {
                property("precision") {
                    number {
                        description = "Precision value"
                        exclusiveMinimum = 0.0
                        exclusiveMaximum = 1.0
                        constValue = 0.5
                    }
                }
            }

        // language=json
        val expectedJson =
            """
            {
                "type": "object",
                "properties": {
                  "precision": {
                    "type": "number",
                    "description": "Precision value",
                    "exclusiveMinimum": 0.0,
                    "exclusiveMaximum": 1.0,
                    "const": 0.5
                  }
                }
            }
            """.trimIndent()

        val deserialized = serializeAndDeserialize(schema, expectedJson, json)

        val precisionProp = deserialized.properties["precision"] as NumericPropertyDefinition
        precisionProp.exclusiveMinimum shouldBe 0.0
        precisionProp.exclusiveMaximum shouldBe 1.0
    }
}
