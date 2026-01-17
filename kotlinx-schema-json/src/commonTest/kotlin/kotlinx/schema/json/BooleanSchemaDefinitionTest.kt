package kotlinx.schema.json

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToJsonElement
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull

class BooleanSchemaDefinitionTest {
    private val json = Json { prettyPrint = true }

    @Test
    fun `deserialize true boolean schema`() {
        val jsonElement = JsonPrimitive(true)
        val propertyDef = json.decodeFromJsonElement<PropertyDefinition>(jsonElement)

        assertIs<BooleanSchemaDefinition>(propertyDef)
        assertEquals(true, propertyDef.value)
    }

    @Test
    fun `deserialize false boolean schema`() {
        val jsonElement = JsonPrimitive(false)
        val propertyDef = json.decodeFromJsonElement<PropertyDefinition>(jsonElement)

        assertIs<BooleanSchemaDefinition>(propertyDef)
        assertEquals(false, propertyDef.value)
    }

    @Test
    fun `serialize true boolean schema`() {
        val booleanSchema = BooleanSchemaDefinition(value = true)
        val jsonElement = json.encodeToJsonElement<PropertyDefinition>(booleanSchema)

        assertIs<JsonPrimitive>(jsonElement)
        assertEquals(true, jsonElement.content.toBoolean())
    }

    @Test
    fun `serialize false boolean schema`() {
        val booleanSchema = BooleanSchemaDefinition(value = false)
        val jsonElement = json.encodeToJsonElement<PropertyDefinition>(booleanSchema)

        assertIs<JsonPrimitive>(jsonElement)
        assertEquals(false, jsonElement.content.toBoolean())
    }

    @Test
    fun `round-trip true boolean schema`() {
        val original = BooleanSchemaDefinition(value = true)
        val jsonElement = json.encodeToJsonElement<PropertyDefinition>(original)
        val decoded = json.decodeFromJsonElement<PropertyDefinition>(jsonElement)

        assertIs<BooleanSchemaDefinition>(decoded)
        assertEquals(original.value, decoded.value)
    }

    @Test
    fun `round-trip false boolean schema`() {
        val original = BooleanSchemaDefinition(value = false)
        val jsonElement = json.encodeToJsonElement<PropertyDefinition>(original)
        val decoded = json.decodeFromJsonElement<PropertyDefinition>(jsonElement)

        assertIs<BooleanSchemaDefinition>(decoded)
        assertEquals(original.value, decoded.value)
    }

    @Test
    fun `array with boolean items schema`() {
        val jsonString =
            """
            {
                "type": "array",
                "items": false
            }
            """.trimIndent()

        val arrayDef = json.decodeFromString<ArrayPropertyDefinition>(jsonString)

        assertNotNull(arrayDef.items)
        assertIs<BooleanSchemaDefinition>(arrayDef.items)
        assertEquals(false, arrayDef.items.value)
    }

    @Test
    fun `oneOf with boolean schemas`() {
        val jsonString =
            """
            {
                "oneOf": [true, false, {"type": "string"}]
            }
            """.trimIndent()

        val oneOfDef = json.decodeFromString<OneOfPropertyDefinition>(jsonString)

        assertEquals(3, oneOfDef.oneOf.size)
        assertIs<BooleanSchemaDefinition>(oneOfDef.oneOf[0])
        assertEquals(true, (oneOfDef.oneOf[0] as BooleanSchemaDefinition).value)
        assertIs<BooleanSchemaDefinition>(oneOfDef.oneOf[1])
        assertEquals(false, (oneOfDef.oneOf[1] as BooleanSchemaDefinition).value)
        assertIs<StringPropertyDefinition>(oneOfDef.oneOf[2])
    }

    @Test
    fun `anyOf with boolean schemas`() {
        val jsonString =
            """
            {
                "anyOf": [true, {"type": "number"}]
            }
            """.trimIndent()

        val anyOfDef = json.decodeFromString<AnyOfPropertyDefinition>(jsonString)

        assertEquals(2, anyOfDef.anyOf.size)
        assertIs<BooleanSchemaDefinition>(anyOfDef.anyOf[0])
        assertEquals(true, (anyOfDef.anyOf[0] as BooleanSchemaDefinition).value)
        assertIs<NumericPropertyDefinition>(anyOfDef.anyOf[1])
    }

    @Test
    fun `allOf with boolean schemas`() {
        val jsonString =
            """
            {
                "allOf": [true, {"type": "string", "minLength": 1}]
            }
            """.trimIndent()

        val allOfDef = json.decodeFromString<AllOfPropertyDefinition>(jsonString)

        assertEquals(2, allOfDef.allOf.size)
        assertIs<BooleanSchemaDefinition>(allOfDef.allOf[0])
        assertEquals(true, (allOfDef.allOf[0] as BooleanSchemaDefinition).value)
        assertIs<StringPropertyDefinition>(allOfDef.allOf[1])
    }

    @Test
    fun `object properties with boolean schema`() {
        val jsonString =
            """
            {
                "type": "object",
                "properties": {
                    "anyValue": true,
                    "neverValid": false
                }
            }
            """.trimIndent()

        val objectDef = json.decodeFromString<ObjectPropertyDefinition>(jsonString)

        assertNotNull(objectDef.properties)
        assertEquals(2, objectDef.properties.size)

        val anyValue = objectDef.properties["anyValue"]
        assertIs<BooleanSchemaDefinition>(anyValue)
        assertEquals(true, anyValue.value)

        val neverValid = objectDef.properties["neverValid"]
        assertIs<BooleanSchemaDefinition>(neverValid)
        assertEquals(false, neverValid.value)
    }

    @Test
    fun `booleanSchemaProperty helper method returns correct value`() {
        val objectDef =
            ObjectPropertyDefinition(
                properties =
                    mapOf(
                        "alwaysValid" to BooleanSchemaDefinition(true),
                        "regularString" to StringPropertyDefinition(),
                    ),
            )

        val booleanSchema = objectDef.booleanSchemaProperty("alwaysValid")
        assertNotNull(booleanSchema)
        assertEquals(true, booleanSchema.value)

        val notBoolean = objectDef.booleanSchemaProperty("regularString")
        assertEquals(null, notBoolean)

        val notExists = objectDef.booleanSchemaProperty("doesNotExist")
        assertEquals(null, notExists)
    }

    @Test
    fun `serialize and deserialize full object with boolean schema properties`() {
        val objectDef =
            ObjectPropertyDefinition(
                properties =
                    mapOf(
                        "acceptAll" to BooleanSchemaDefinition(true),
                        "rejectAll" to BooleanSchemaDefinition(false),
                        "normalString" to StringPropertyDefinition(),
                    ),
            )

        val jsonElement = json.encodeToJsonElement<ObjectPropertyDefinition>(objectDef)
        val decoded = json.decodeFromJsonElement<ObjectPropertyDefinition>(jsonElement)

        assertEquals(3, decoded.properties?.size)

        val acceptAll = decoded.properties?.get("acceptAll")
        assertIs<BooleanSchemaDefinition>(acceptAll)
        assertEquals(true, acceptAll.value)

        val rejectAll = decoded.properties?.get("rejectAll")
        assertIs<BooleanSchemaDefinition>(rejectAll)
        assertEquals(false, rejectAll.value)

        val normalString = decoded.properties?.get("normalString")
        assertIs<StringPropertyDefinition>(normalString)
    }
}
