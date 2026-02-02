package kotlinx.schema.json

import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
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
        val constraint: AdditionalPropertiesConstraint = AllowAdditionalProperties
        val encoded = json.encodeToJsonElement(AdditionalPropertiesSerializer, constraint)

        encoded shouldBe JsonPrimitive(true)
    }

    @Test
    fun `serialize DenyAdditionalProperties as false`() {
        val constraint: AdditionalPropertiesConstraint = DenyAdditionalProperties
        val encoded = json.encodeToJsonElement(AdditionalPropertiesSerializer, constraint)

        encoded shouldBe JsonPrimitive(false)
    }

    @Test
    fun `serialize AdditionalPropertiesSchema as schema object`() {
        val schema = StringPropertyDefinition(description = "Must be a string")
        val constraint: AdditionalPropertiesConstraint = AdditionalPropertiesSchema(schema)

        val encoded = json.encodeToJsonElement(AdditionalPropertiesSerializer, constraint)

        encoded.shouldBeInstanceOf<JsonObject>()
        encoded["type"]?.jsonPrimitive?.content shouldBe "string"
        encoded["description"]?.jsonPrimitive?.content shouldBe "Must be a string"
    }

    @Test
    fun `deserialize true as AllowAdditionalProperties`() {
        val element = JsonPrimitive(true)
        val constraint = json.decodeFromJsonElement(AdditionalPropertiesSerializer, element)

        constraint shouldBe AllowAdditionalProperties
    }

    @Test
    fun `deserialize false as DenyAdditionalProperties`() {
        val element = JsonPrimitive(false)
        val constraint = json.decodeFromJsonElement(AdditionalPropertiesSerializer, element)

        constraint shouldBe DenyAdditionalProperties
    }

    @Test
    fun `deserialize schema object as AdditionalPropertiesSchema`() {
        val element =
            buildJsonObject {
                put("type", "number")
                put("minimum", 0)
            }

        val constraint = json.decodeFromJsonElement(AdditionalPropertiesSerializer, element)

        constraint.shouldBeInstanceOf<AdditionalPropertiesSchema>()
        val schema = constraint.schema.shouldBeInstanceOf<NumericPropertyDefinition>()
        schema.minimum shouldBe 0.0
    }

    @Test
    fun `round-trip serialization - allow`() {
        val original = AdditionalPropertiesConstraint.allow()

        val encoded = json.encodeToJsonElement(AdditionalPropertiesSerializer, original)
        val decoded = json.decodeFromJsonElement(AdditionalPropertiesSerializer, encoded)

        decoded shouldBe original
    }

    @Test
    fun `round-trip serialization - deny`() {
        val original = AdditionalPropertiesConstraint.deny()

        val encoded = json.encodeToJsonElement(AdditionalPropertiesSerializer, original)
        val decoded = json.decodeFromJsonElement(AdditionalPropertiesSerializer, encoded)

        decoded shouldBe original
    }

    @Test
    fun `round-trip serialization - schema`() {
        val original =
            AdditionalPropertiesConstraint.schema(
                StringPropertyDefinition(
                    minLength = 1,
                    maxLength = 100,
                ),
            )

        val encoded = json.encodeToJsonElement(AdditionalPropertiesSerializer, original)
        val decoded = json.decodeFromJsonElement(AdditionalPropertiesSerializer, encoded)

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
        val encoded = schema.encodeToJsonObject(json)
        val decoded = json.decodeFromJsonElement<JsonSchema>(encoded)

        decoded.additionalProperties shouldBe DenyAdditionalProperties
        encoded["additionalProperties"] shouldBe JsonPrimitive(false)
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
        val encoded = schema.encodeToJsonObject(json)
        val decoded = json.decodeFromJsonElement<JsonSchema>(encoded)

        decoded.additionalProperties shouldBe AllowAdditionalProperties
        encoded["additionalProperties"] shouldBe JsonPrimitive(true)
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

        schemaWithAdditionalProps.additionalProperties.shouldBeInstanceOf<AdditionalPropertiesSchema>()
        val additionalSchema =
            schemaWithAdditionalProps.additionalProperties
                .schema
                .shouldBeInstanceOf<ObjectPropertyDefinition>()
        additionalSchema.properties
            ?.get("dynamic")
            .shouldBeInstanceOf<StringPropertyDefinition>()

        // Round-trip
        val encoded = schemaWithAdditionalProps.encodeToJsonObject(json)
        val decoded = json.decodeFromJsonElement<JsonSchema>(encoded)

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
        val encoded = schema.encodeToJsonObject(json)
        val decoded = json.decodeFromJsonElement<JsonSchema>(encoded)

        decoded.additionalProperties.shouldBeNull()
        encoded.containsKey("additionalProperties") shouldBe false
    }

    @Test
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

        // Verify structure
        val strictObj = schema.objectProperty("strict")!!
        strictObj.additionalProperties shouldBe DenyAdditionalProperties

        val flexibleObj = schema.objectProperty("flexible")!!
        flexibleObj.additionalProperties shouldBe AllowAdditionalProperties

        val typedObj = schema.objectProperty("typed")!!
        typedObj.additionalProperties.shouldBeInstanceOf<AdditionalPropertiesSchema>()
        val typedSchema = typedObj.additionalProperties.schema
        typedSchema.shouldBeInstanceOf<StringPropertyDefinition>()
        typedSchema.pattern shouldBe "^[a-z]+$"

        // Round-trip entire schema
        val encoded = schema.encodeToJsonObject(json)
        val decoded = json.decodeFromJsonElement<JsonSchema>(encoded)

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

        val parsed = json.decodeFromString<JsonSchema>(jsonSchemaText)

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

        val parsed = json.decodeFromString<JsonSchema>(jsonSchemaText)

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

        val parsed = json.decodeFromString<JsonSchema>(jsonSchemaText)

        parsed.additionalProperties.shouldBeInstanceOf<AdditionalPropertiesSchema>()
        val schema = parsed.additionalProperties.schema
        schema.shouldBeInstanceOf<NumericPropertyDefinition>()
        schema.minimum shouldBe 0.0
    }
}
