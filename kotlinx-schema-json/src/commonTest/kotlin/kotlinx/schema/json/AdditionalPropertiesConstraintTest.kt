package kotlinx.schema.json

import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.serialization.json.Json
import kotlin.test.Test

/**
 * Tests for [AdditionalPropertiesConstraint] and its serialization.
 *
 * Verifies that additionalProperties correctly handles the three JSON Schema forms:
 * - Boolean `true`: Allow any additional properties
 * - Boolean `false`: Disallow additional properties
 * - Schema object: Additional properties must match schema
 */
class AdditionalPropertiesConstraintTest {
    private val json = Json { ignoreUnknownKeys = false }

    @Test
    fun `serialize AllowAdditionalProperties as true`() {
        serializeAndDeserialize<AdditionalPropertiesConstraint>(AllowAdditionalProperties, "true", json)
    }

    @Test
    fun `serialize DenyAdditionalProperties as false`() {
        serializeAndDeserialize<AdditionalPropertiesConstraint>(DenyAdditionalProperties, "false", json)
    }

    @Test
    fun `serialize AdditionalPropertiesSchema as schema object`() {
        val schema = StringPropertyDefinition(description = "Must be a string")
        val constraint: AdditionalPropertiesConstraint = AdditionalPropertiesSchema(schema)

        serializeAndDeserialize<AdditionalPropertiesConstraint>(
            constraint,
            """
            {
              "type": "string",
              "description": "Must be a string"
            }
            """.trimIndent(),
            json,
        )
    }

    @Test
    fun `deserialize true as AllowAdditionalProperties`() {
        deserializeAndSerialize<AdditionalPropertiesConstraint>("true", json) shouldBe AllowAdditionalProperties
    }

    @Test
    fun `deserialize false as DenyAdditionalProperties`() {
        deserializeAndSerialize<AdditionalPropertiesConstraint>("false", json) shouldBe DenyAdditionalProperties
    }

    @Test
    fun `deserialize schema object as AdditionalPropertiesSchema`() {
        val jsonString =
            """
            {
              "type": "number",
              "minimum": 0
            }
            """.trimIndent()

        val constraint = deserializeAndSerialize<AdditionalPropertiesConstraint>(jsonString, json)

        constraint.shouldBeInstanceOf<AdditionalPropertiesSchema>()
        val schema = constraint.schema.shouldBeInstanceOf<NumericPropertyDefinition>()
        schema.minimum shouldBe 0.0
    }

    @Test
    fun `round-trip serialization - allow`() {
        deserializeAndSerialize<AdditionalPropertiesConstraint>("true", json) shouldBe AllowAdditionalProperties
    }

    @Test
    fun `round-trip serialization - deny`() {
        deserializeAndSerialize<AdditionalPropertiesConstraint>("false", json) shouldBe DenyAdditionalProperties
    }

    @Test
    fun `round-trip serialization - schema`() {
        val jsonString =
            """
            {
              "type": "string",
              "minLength": 1,
              "maxLength": 100
            }
            """.trimIndent()

        val decoded = deserializeAndSerialize<AdditionalPropertiesConstraint>(jsonString, json)

        decoded.shouldBeInstanceOf<AdditionalPropertiesSchema>()
        val schema = decoded.schema.shouldBeInstanceOf<StringPropertyDefinition>()
        schema.minLength shouldBe 1
        schema.maxLength shouldBe 100
    }

    @Test
    fun `ObjectPropertyDefinition with additionalProperties false`() {
        val schema =
            jsonSchema {
                property("name") { string() }
                additionalProperties = false
            }

        schema.additionalProperties shouldBe DenyAdditionalProperties

        // Round-trip
        serializeAndDeserialize(
            schema,
            """
            {
              "type": "object",
              "properties": {
                "name": {
                  "type": "string"
                }
              },
              "additionalProperties": false
            }
            """.trimIndent(),
            json,
        )
    }

    @Test
    fun `ObjectPropertyDefinition with additionalProperties true`() {
        val schema =
            jsonSchema {
                property("name") { string() }
                additionalProperties = true
            }

        schema.additionalProperties shouldBe AllowAdditionalProperties

        // Round-trip
        serializeAndDeserialize(
            schema,
            """
            {
              "type": "object",
              "properties": {
                "name": {
                  "type": "string"
                }
              },
              "additionalProperties": true
            }
            """.trimIndent(),
            json,
        )
    }

    @Test
    fun `ObjectPropertyDefinition with additionalProperties schema`() {
        // Create a schema with additionalProperties set to an object schema using companion function
        val schemaWithAdditionalProps =
            JsonSchema(
                properties =
                    mapOf(
                        "name" to StringPropertyDefinition(),
                    ),
                additionalProperties =
                    AdditionalPropertiesConstraint.schema(
                        ObjectPropertyDefinition(
                            properties =
                                mapOf(
                                    "dynamic" to StringPropertyDefinition(),
                                ),
                        ),
                    ),
            )

        // Round-trip
        val decoded =
            serializeAndDeserialize(
                schemaWithAdditionalProps,
                """
                {
                  "type": "object",
                  "properties": {
                    "name": {
                      "type": "string"
                    }
                  },
                  "additionalProperties": {
                    "type": "object",
                    "properties": {
                      "dynamic": {
                        "type": "string"
                      }
                    }
                  }
                }
                """.trimIndent(),
                json,
            )

        decoded.additionalProperties.shouldBeInstanceOf<AdditionalPropertiesSchema>()
        val roundTripSchema =
            decoded.additionalProperties
                .schema
                .shouldBeInstanceOf<ObjectPropertyDefinition>()
        roundTripSchema.properties
            ?.get("dynamic")
            .shouldBeInstanceOf<StringPropertyDefinition>()
    }

    @Test
    fun `ObjectPropertyDefinition with no additionalProperties constraint`() {
        val schema =
            jsonSchema {
                property("name") { string() }
                // No additionalProperties set
            }

        schema.additionalProperties.shouldBeNull()

        // Round-trip - null should not be serialized
        serializeAndDeserialize(
            schema,
            """
            {
              "type": "object",
              "properties": {
                "name": {
                  "type": "string"
                }
              }
            }
            """.trimIndent(),
            json,
        )
    }

    @Test
    @Suppress("LongMethod")
    fun `nested object with different additionalProperties constraints`() {
        val schema =
            jsonSchema {
                property("strict") {
                    obj {
                        property("name") { string() }
                        additionalProperties = false // Strict
                    }
                }
                property("flexible") {
                    obj {
                        property("name") { string() }
                        additionalProperties = true // Flexible
                    }
                }
                property("typed") {
                    obj {
                        property("name") { string() }
                        additionalProperties =
                            StringPropertyDefinition(
                                pattern = "^[a-z]+$",
                            ) // Type constraint
                    }
                }
            }

        // Round-trip entire schema
        val decoded =
            serializeAndDeserialize(
                schema,
                """
                {
                  "type": "object",
                  "properties": {
                    "strict": {
                      "type": "object",
                      "properties": {
                        "name": {
                          "type": "string"
                        }
                      },
                      "additionalProperties": false
                    },
                    "flexible": {
                      "type": "object",
                      "properties": {
                        "name": {
                          "type": "string"
                        }
                      },
                      "additionalProperties": true
                    },
                    "typed": {
                      "type": "object",
                      "properties": {
                        "name": {
                          "type": "string"
                        }
                      },
                      "additionalProperties": {
                        "type": "string",
                        "pattern": "^[a-z]+$"
                      }
                    }
                  }
                }
                """.trimIndent(),
                json,
            )

        // Verify after round-trip
        val decodedStrict = decoded.objectProperty("strict")!!
        decodedStrict.additionalProperties shouldBe DenyAdditionalProperties

        val decodedFlexible = decoded.objectProperty("flexible")!!
        decodedFlexible.additionalProperties shouldBe AllowAdditionalProperties

        val decodedTyped = decoded.objectProperty("typed")!!
        decodedTyped.additionalProperties.shouldBeInstanceOf<AdditionalPropertiesSchema>()
    }

    @Test
    fun `parse real JSON Schema with additionalProperties false`() {
        val jsonSchemaText =
            """
            {
              "type": "object",
              "properties": {
                "name": { "type": "string" }
              },
              "additionalProperties": false
            }
            """.trimIndent()

        val parsed = deserializeAndSerialize<JsonSchema>(jsonSchemaText, json)

        parsed.additionalProperties shouldBe DenyAdditionalProperties
    }

    @Test
    fun `parse real JSON Schema with additionalProperties true`() {
        val jsonSchemaText =
            """
            {
              "type": "object",
              "properties": {
                "name": { "type": "string" }
              },
              "additionalProperties": true
            }
            """.trimIndent()

        val parsed = deserializeAndSerialize<JsonSchema>(jsonSchemaText, json)

        parsed.additionalProperties shouldBe AllowAdditionalProperties
    }

    @Test
    fun `parse real JSON Schema with additionalProperties schema`() {
        val jsonSchemaText =
            """
            {
              "type": "object",
              "properties": {
                "name": { "type": "string" }
              },
              "additionalProperties": {
                "type": "number",
                "minimum": 0
              }
            }
            """.trimIndent()

        val parsed = deserializeAndSerialize<JsonSchema>(jsonSchemaText, json)

        parsed.additionalProperties.shouldBeInstanceOf<AdditionalPropertiesSchema>()
        val schema = parsed.additionalProperties.schema
        schema.shouldBeInstanceOf<NumericPropertyDefinition>()
        schema.minimum shouldBe 0.0
    }
}
