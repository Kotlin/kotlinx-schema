package kotlinx.schema.json

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Tests for unknown keywords as annotations feature.
 *
 * Per JSON Schema spec: "Unknown keywords SHOULD be treated as annotations"
 */
class AnnotationsTest {
    private val json =
        Json {
            prettyPrint = false
            ignoreUnknownKeys = true // Required for annotations to work - unknown keys are extracted separately
        }

    @Test
    fun `parse schema with unknown keywords as annotations`() {
        val schemaJson =
            """
            {
                "type": "object",
                "properties": {
                    "name": { "type": "string" }
                },
                "x-custom": "custom value",
                "x-vendor-extension": { "key": "value" }
            }
            """.trimIndent()

        val schema = json.decodeFromString<PropertyDefinition>(schemaJson) as ObjectPropertyDefinition

        assertNotNull(schema.annotations, "annotations should not be null")
        assertEquals(JsonPrimitive("custom value"), schema.annotations["x-custom"])
        assertTrue(schema.annotations["x-vendor-extension"] is JsonObject)
        val vendorExt = schema.annotations["x-vendor-extension"] as JsonObject
        assertEquals(JsonPrimitive("value"), vendorExt["key"])
    }

    @Test
    fun `annotations preserved in round-trip serialization`() {
        val schemaJson =
            """
            {
                "type": "object",
                "properties": {
                    "name": { "type": "string" }
                },
                "x-custom": "value",
                "x-metadata": { "key": "value" }
            }
            """.trimIndent()

        val decoded = json.decodeFromString<PropertyDefinition>(schemaJson) as ObjectPropertyDefinition
        val encoded = json.encodeToString<PropertyDefinition>(decoded)
        val reDecoded = json.decodeFromString<PropertyDefinition>(encoded) as ObjectPropertyDefinition

        assertEquals(decoded.annotations, reDecoded.annotations)
        assertNotNull(reDecoded.annotations)
        assertEquals(JsonPrimitive("value"), reDecoded.annotations["x-custom"])
    }

    @Test
    fun `nested properties preserve annotations`() {
        val schemaJson =
            """
            {
                "type": "object",
                "properties": {
                    "email": {
                        "type": "string",
                        "format": "email",
                        "x-validation-level": "strict",
                        "x-ui-hint": "primary-email"
                    }
                }
            }
            """.trimIndent()

        val schema = json.decodeFromString<PropertyDefinition>(schemaJson) as ObjectPropertyDefinition
        val emailProp = schema.stringProperty("email")

        assertNotNull(emailProp, "email property should exist")
        assertNotNull(emailProp.annotations, "email property should have annotations")
        assertEquals(JsonPrimitive("strict"), emailProp.annotations["x-validation-level"])
        assertEquals(JsonPrimitive("primary-email"), emailProp.annotations["x-ui-hint"])
    }

    @Test
    fun `schemas without unknown keywords have null annotations`() {
        val schemaJson =
            """
            {
                "type": "object",
                "properties": {
                    "name": { "type": "string" }
                }
            }
            """.trimIndent()

        val schema = json.decodeFromString<PropertyDefinition>(schemaJson) as ObjectPropertyDefinition

        assertNull(schema.annotations, "annotations should be null when no unknown keywords present")
    }

    @Test
    fun `annotations support complex JSON structures`() {
        val schemaJson =
            """
            {
                "type": "string",
                "x-custom-config": {
                    "validators": ["email", "required"],
                    "transformers": {
                        "before": "trim",
                        "after": "lowercase"
                    }
                }
            }
            """.trimIndent()

        val schema = json.decodeFromString<PropertyDefinition>(schemaJson)

        assertNotNull(schema.annotations)
        assertTrue(schema.annotations!!["x-custom-config"] is JsonObject)
        // Could add more detailed structure verification here
    }

    @Test
    fun `multiple unknown keywords at root level`() {
        val schemaJson =
            """
            {
                "type": "string",
                "x-key1": "value1",
                "x-key2": "value2",
                "x-key3": "value3"
            }
            """.trimIndent()

        val schema = json.decodeFromString<PropertyDefinition>(schemaJson)

        assertNotNull(schema.annotations)
        assertEquals(3, schema.annotations!!.size)
        assertEquals(JsonPrimitive("value1"), schema.annotations!!["x-key1"])
        assertEquals(JsonPrimitive("value2"), schema.annotations!!["x-key2"])
        assertEquals(JsonPrimitive("value3"), schema.annotations!!["x-key3"])
    }

    @Test
    fun `annotations in array property definitions`() {
        val schemaJson =
            """
            {
                "type": "array",
                "items": { "type": "string" },
                "x-array-metadata": "custom-array-info"
            }
            """.trimIndent()

        val schema = json.decodeFromString<PropertyDefinition>(schemaJson)

        assertNotNull(schema.annotations)
        assertEquals(JsonPrimitive("custom-array-info"), schema.annotations!!["x-array-metadata"])
    }

    @Test
    fun `annotations in numeric property definitions`() {
        val schemaJson =
            """
            {
                "type": "number",
                "minimum": 0,
                "maximum": 100,
                "x-unit": "percentage"
            }
            """.trimIndent()

        val schema = json.decodeFromString<PropertyDefinition>(schemaJson)

        assertNotNull(schema.annotations)
        assertEquals(JsonPrimitive("percentage"), schema.annotations!!["x-unit"])
    }

    @Test
    fun `annotations in oneOf composition`() {
        val schemaJson =
            """
            {
                "oneOf": [
                    { "type": "string" },
                    { "type": "number" }
                ],
                "x-polymorphic-type": "string-or-number"
            }
            """.trimIndent()

        val schema = json.decodeFromString<PropertyDefinition>(schemaJson)

        assertNotNull(schema.annotations)
        assertEquals(JsonPrimitive("string-or-number"), schema.annotations!!["x-polymorphic-type"])
    }

    @Test
    fun `annotations in anyOf composition`() {
        val schemaJson =
            """
            {
                "anyOf": [
                    { "type": "string" },
                    { "type": "null" }
                ],
                "x-nullable-string": true
            }
            """.trimIndent()

        val schema = json.decodeFromString<PropertyDefinition>(schemaJson)

        assertNotNull(schema.annotations)
        assertEquals(JsonPrimitive(true), schema.annotations!!["x-nullable-string"])
    }

    @Test
    fun `annotations in allOf composition`() {
        val schemaJson =
            """
            {
                "allOf": [
                    { "type": "object" },
                    { "properties": { "name": { "type": "string" } } }
                ],
                "x-composed": "merged-schema"
            }
            """.trimIndent()

        val schema = json.decodeFromString<PropertyDefinition>(schemaJson)

        assertNotNull(schema.annotations)
        assertEquals(JsonPrimitive("merged-schema"), schema.annotations!!["x-composed"])
    }

    @Test
    fun `annotations with OpenAPI vendor extensions`() {
        val schemaJson =
            """
            {
                "type": "string",
                "format": "email",
                "x-go-name": "EmailAddress",
                "x-nullable": true,
                "x-omitempty": false
            }
            """.trimIndent()

        val schema = json.decodeFromString<PropertyDefinition>(schemaJson)

        assertNotNull(schema.annotations)
        assertEquals(JsonPrimitive("EmailAddress"), schema.annotations!!["x-go-name"])
        assertEquals(JsonPrimitive(true), schema.annotations!!["x-nullable"])
        assertEquals(JsonPrimitive(false), schema.annotations!!["x-omitempty"])
    }

    @Test
    fun `annotations do not interfere with known keywords`() {
        val schemaJson =
            """
            {
                "type": "string",
                "minLength": 5,
                "maxLength": 50,
                "pattern": "^[a-z]+$",
                "x-custom": "value"
            }
            """.trimIndent()

        val schema = json.decodeFromString<PropertyDefinition>(schemaJson) as StringPropertyDefinition

        // Verify known keywords are parsed correctly
        assertEquals(5, schema.minLength)
        assertEquals(50, schema.maxLength)
        assertEquals("^[a-z]+$", schema.pattern)

        // Verify annotations are captured
        assertNotNull(schema.annotations)
        assertEquals(JsonPrimitive("value"), schema.annotations["x-custom"])
    }

    @Test
    fun `empty object has no annotations`() {
        val schemaJson = "{}"

        val schema = json.decodeFromString<PropertyDefinition>(schemaJson)

        assertNull(schema.annotations)
    }

    @Test
    fun `annotations serialization produces flat structure`() {
        val schemaJson =
            """
            {
                "type": "string",
                "x-custom": "value"
            }
            """.trimIndent()

        val schema = json.decodeFromString<PropertyDefinition>(schemaJson)
        val encoded = json.encodeToString<PropertyDefinition>(schema)

        // Parse as JsonObject to verify structure
        val jsonObject = Json.parseToJsonElement(encoded) as JsonObject

        // Annotations should be at root level, not nested
        assertTrue(jsonObject.containsKey("x-custom"))
        assertEquals(JsonPrimitive("value"), jsonObject["x-custom"])

        // Should NOT have an "annotations" field in the output
        assertTrue(!jsonObject.containsKey("annotations"))
    }

    @Test
    fun `reference property with annotations`() {
        val schemaJson =
            $$"""
            {
                "$ref": "#/definitions/User",
                "x-ref-metadata": "user-reference"
            }
            """.trimIndent()

        val schema = json.decodeFromString<PropertyDefinition>(schemaJson) as ReferencePropertyDefinition

        assertNotNull(schema.annotations)
        assertEquals(JsonPrimitive("user-reference"), schema.annotations["x-ref-metadata"])
        assertEquals("#/definitions/User", schema.ref)
    }

    @Test
    fun `generic property with annotations`() {
        val schemaJson =
            """
            {
                "x-custom-field": "custom-value"
            }
            """.trimIndent()

        val schema = json.decodeFromString<PropertyDefinition>(schemaJson)

        assertNotNull(schema.annotations)
        assertEquals(JsonPrimitive("custom-value"), schema.annotations!!["x-custom-field"])
    }
}
