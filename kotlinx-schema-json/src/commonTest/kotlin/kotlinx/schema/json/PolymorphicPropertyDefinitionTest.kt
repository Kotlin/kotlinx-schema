package kotlinx.schema.json

import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlin.test.Test

/**
 * Tests for polymorphic property definitions (oneOf, anyOf, allOf) serialization and deserialization.
 */
internal class PolymorphicPropertyDefinitionTest {
    private val json = Json { prettyPrint = true }

    @Test
    fun `oneOf with simple options serialization round-trip`() {
        val definition =
            OneOfPropertyDefinition(
                oneOf =
                    listOf(
                        StringPropertyDefinition(description = "String option"),
                        NumericPropertyDefinition(type = listOf("integer"), description = "Integer option"),
                    ),
                description = "String or integer",
            )

        // language=json
        val expectedJson =
            """
            {
              "oneOf": [
                {
                  "type": "string",
                  "description": "String option"
                },
                {
                  "type": "integer",
                  "description": "Integer option"
                }
              ],
              "description": "String or integer"
            }
            """.trimIndent()

        val deserialized =
            serializeAndDeserialize(
                definition,
                expectedJson,
                json,
            )

        deserialized.oneOf.size shouldBe 2
        deserialized.description shouldBe "String or integer"
        deserialized.discriminator shouldBe null
    }

    @Test
    fun `oneOf with discriminator serialization round-trip`() {
        val definition =
            OneOfPropertyDefinition(
                oneOf =
                    listOf(
                        ReferencePropertyDefinition(ref = "#/definitions/Dog"),
                        ReferencePropertyDefinition(ref = "#/definitions/Cat"),
                    ),
                discriminator =
                    Discriminator(
                        propertyName = "petType",
                        mapping =
                            mapOf(
                                "dog" to "#/definitions/Dog",
                                "cat" to "#/definitions/Cat",
                            ),
                    ),
                description = "A pet animal",
            )

        // language=json
        val expectedJson =
            $$"""
            {
              "oneOf": [
                {
                  "$ref": "#/definitions/Dog"
                },
                {
                  "$ref": "#/definitions/Cat"
                }
              ],
              "discriminator": {
                "propertyName": "petType",
                "mapping": {
                  "dog": "#/definitions/Dog",
                  "cat": "#/definitions/Cat"
                }
              },
              "description": "A pet animal"
            }
            """.trimIndent()

        val deserialized =
            serializeAndDeserialize(
                definition,
                expectedJson,
                json,
            )

        deserialized.oneOf.size shouldBe 2
        deserialized.discriminator?.propertyName shouldBe "petType"
        deserialized.discriminator?.mapping?.size shouldBe 2
    }

    @Test
    @Suppress("LongMethod")
    fun `oneOf with inline object options serialization round-trip`() {
        val definition =
            OneOfPropertyDefinition(
                oneOf =
                    listOf(
                        ObjectPropertyDefinition(
                            properties =
                                mapOf(
                                    "type" to StringPropertyDefinition(constValue = JsonPrimitive("credit_card")),
                                    "cardNumber" to StringPropertyDefinition(description = "Card number"),
                                ),
                            required = listOf("type", "cardNumber"),
                        ),
                        ObjectPropertyDefinition(
                            properties =
                                mapOf(
                                    "type" to StringPropertyDefinition(constValue = JsonPrimitive("paypal")),
                                    "email" to StringPropertyDefinition(format = "email", description = "PayPal email"),
                                ),
                            required = listOf("type", "email"),
                        ),
                    ),
                description = "Payment method",
            )

        // language=json
        val expectedJson =
            """
            {
              "oneOf": [
                {
                  "type": "object",
                  "properties": {
                    "type": {
                      "type": "string",
                      "const": "credit_card"
                    },
                    "cardNumber": {
                      "type": "string",
                      "description": "Card number"
                    }
                  },
                  "required": ["type", "cardNumber"]
                },
                {
                  "type": "object",
                  "properties": {
                    "type": {
                      "type": "string",
                      "const": "paypal"
                    },
                    "email": {
                      "type": "string",
                      "format": "email",
                      "description": "PayPal email"
                    }
                  },
                  "required": ["type", "email"]
                }
              ],
              "description": "Payment method"
            }
            """.trimIndent()

        serializeAndDeserialize(definition, expectedJson, json)
    }

    @Test
    fun `anyOf with mixed types serialization round-trip`() {
        val definition =
            AnyOfPropertyDefinition(
                anyOf =
                    listOf(
                        StringPropertyDefinition(format = "uuid", description = "UUID identifier"),
                        NumericPropertyDefinition(
                            type = listOf("integer"),
                            minimum = 1.0,
                            description = "Numeric identifier",
                        ),
                    ),
                description = "UUID or integer ID",
            )

        // language=json
        val expectedJson =
            """
            {
              "anyOf": [
                {
                  "type": "string",
                  "format": "uuid",
                  "description": "UUID identifier"
                },
                {
                  "type": "integer",
                  "minimum": 1.0,
                  "description": "Numeric identifier"
                }
              ],
              "description": "UUID or integer ID"
            }
            """.trimIndent()

        val deserialized =
            serializeAndDeserialize(
                definition,
                expectedJson,
                json,
            )

        deserialized.anyOf.size shouldBe 2
        deserialized.description shouldBe "UUID or integer ID"
    }

    @Test
    @Suppress("LongMethod")
    fun `allOf with composition serialization round-trip`() {
        val definition =
            AllOfPropertyDefinition(
                allOf =
                    listOf(
                        ReferencePropertyDefinition(ref = "#/definitions/BaseUser"),
                        ObjectPropertyDefinition(
                            properties =
                                mapOf(
                                    "role" to
                                        StringPropertyDefinition(
                                            enum = listOf("admin", "superadmin"),
                                            description = "Admin role",
                                        ),
                                    "permissions" to
                                        ArrayPropertyDefinition(
                                            items = StringPropertyDefinition(),
                                            description = "List of permissions",
                                        ),
                                ),
                            required = listOf("role", "permissions"),
                        ),
                    ),
                description = "Admin user extends base user",
            )

        // language=json
        val expectedJson =
            $$"""
            {
              "allOf": [
                {
                  "$ref": "#/definitions/BaseUser"
                },
                {
                  "type": "object",
                  "properties": {
                    "role": {
                      "type": "string",
                      "enum": ["admin", "superadmin"],
                      "description": "Admin role"
                    },
                    "permissions": {
                      "type": "array",
                      "items": {
                        "type": "string"
                      },
                      "description": "List of permissions"
                    }
                  },
                  "required": ["role", "permissions"]
                }
              ],
              "description": "Admin user extends base user"
            }
            """.trimIndent()

        val deserialized =
            serializeAndDeserialize(
                definition,
                expectedJson,
                json,
            )

        deserialized.allOf.size shouldBe 2
        deserialized.description shouldBe "Admin user extends base user"
    }

    @Test
    fun `oneOf nested in property definition serialization round-trip`() {
        val schema =
            jsonSchema {
                name = "FlexibleType"
                schema {
                    property("value") {
                        required = true
                        oneOf {
                            description = "String or number value"
                            string { minLength = 1 }
                            number { minimum = 0.0 }
                        }
                    }
                }
            }

        // language=json
        val expectedJson =
            """
            {
              "name": "FlexibleType",
              "strict": false,
              "schema": {
                "type": "object",
                "properties": {
                  "value": {
                    "oneOf": [
                      {
                        "type": "string",
                        "minLength": 1
                      },
                      {
                        "type": "number",
                        "minimum": 0.0
                      }
                    ],
                    "description": "String or number value"
                  }
                },
                "required": ["value"]
              }
            }
            """.trimIndent()

        serializeAndDeserialize(schema, expectedJson, json)
    }

    @Test
    fun `allOf with single option serialization round-trip`() {
        val definition =
            AllOfPropertyDefinition(
                allOf = listOf(ReferencePropertyDefinition(ref = "#/definitions/Base")),
                description = "Single ref allOf",
            )

        // language=json
        val expectedJson =
            $$"""
            {
              "allOf": [
                {
                  "$ref": "#/definitions/Base"
                }
              ],
              "description": "Single ref allOf"
            }
            """.trimIndent()

        serializeAndDeserialize(definition, expectedJson, json)
    }

    @Test
    fun `nested polymorphism oneOf inside allOf serialization round-trip`() {
        val definition =
            AllOfPropertyDefinition(
                allOf =
                    listOf(
                        ReferencePropertyDefinition(ref = "#/definitions/Base"),
                        OneOfPropertyDefinition(
                            oneOf =
                                listOf(
                                    StringPropertyDefinition(description = "String variant"),
                                    NumericPropertyDefinition(
                                        type = listOf("integer"),
                                        description = "Integer variant",
                                    ),
                                ),
                        ),
                    ),
                description = "Complex nested polymorphism",
            )

        // language=json
        val expectedJson =
            $$"""
            {
              "allOf": [
                {
                  "$ref": "#/definitions/Base"
                },
                {
                  "oneOf": [
                    {
                      "type": "string",
                      "description": "String variant"
                    },
                    {
                      "type": "integer",
                      "description": "Integer variant"
                    }
                  ]
                }
              ],
              "description": "Complex nested polymorphism"
            }
            """.trimIndent()

        serializeAndDeserialize(definition, expectedJson, json)
    }

    @Test
    fun `discriminator without mapping serialization round-trip`() {
        val definition =
            OneOfPropertyDefinition(
                oneOf =
                    listOf(
                        ReferencePropertyDefinition(ref = "#/definitions/TypeA"),
                        ReferencePropertyDefinition(ref = "#/definitions/TypeB"),
                    ),
                discriminator = Discriminator(),
                description = "Discriminator with implicit mapping",
            )

        // language=json
        val expectedJson =
            $$"""
            {
              "oneOf": [
                {
                  "$ref": "#/definitions/TypeA"
                },
                {
                  "$ref": "#/definitions/TypeB"
                }
              ],
              "discriminator": {
                "propertyName": "type"
              },
              "description": "Discriminator with implicit mapping"
            }
            """.trimIndent()

        val deserialized =
            serializeAndDeserialize(
                definition,
                expectedJson,
                json,
            )

        deserialized.discriminator?.propertyName shouldBe "type"
        deserialized.discriminator?.mapping shouldBe null
    }

    @Test
    fun `deserialize oneOf from JSON`() {
        // language=json
        val inputJson =
            """
            {
              "oneOf": [
                {
                  "type": "string"
                },
                {
                  "type": "integer"
                }
              ]
            }
            """.trimIndent()

        val result = deserializeAndSerialize<OneOfPropertyDefinition>(inputJson, json)

        result.oneOf.size shouldBe 2
    }

    @Test
    fun `deserialize anyOf from JSON`() {
        // language=json
        val inputJson =
            """
            {
              "anyOf": [
                {
                  "type": "string"
                },
                {
                  "type": "number"
                }
              ]
            }
            """.trimIndent()

        val result = deserializeAndSerialize<AnyOfPropertyDefinition>(inputJson, json)

        result.anyOf.size shouldBe 2
    }

    @Test
    fun `deserialize allOf from JSON`() {
        // language=json
        val inputJson =
            $$"""
            {
              "allOf": [
                {
                  "$ref": "#/definitions/Base"
                },
                {
                  "type": "object",
                  "properties": {
                    "extra": {
                      "type": "string"
                    }
                  }
                }
              ]
            }
            """.trimIndent()

        val result = deserializeAndSerialize<AllOfPropertyDefinition>(inputJson, json)

        result.allOf.size shouldBe 2
    }
}
