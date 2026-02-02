package kotlinx.schema.json

import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.serialization.json.Json
import kotlin.test.Test

class BooleanSchemaDefinitionTest {
    private val json = Json { prettyPrint = true }

    @Test
    fun `deserialize true boolean schema`() {
        val propertyDef = deserializeAndSerialize<PropertyDefinition>("true", json)

        propertyDef.shouldBeInstanceOf<BooleanSchemaDefinition>()
        propertyDef.value shouldBe true
    }

    @Test
    fun `deserialize false boolean schema`() {
        val propertyDef = deserializeAndSerialize<PropertyDefinition>("false", json)

        propertyDef.shouldBeInstanceOf<BooleanSchemaDefinition>()
        propertyDef.value shouldBe false
    }

    @Test
    fun `serialize true boolean schema`() {
        val booleanSchema = BooleanSchemaDefinition(value = true)
        serializeAndDeserialize<PropertyDefinition>(booleanSchema, "true", json)
    }

    @Test
    fun `serialize false boolean schema`() {
        val booleanSchema = BooleanSchemaDefinition(value = false)
        serializeAndDeserialize<PropertyDefinition>(booleanSchema, "false", json)
    }

    @Test
    fun `round-trip true boolean schema`() {
        val original = BooleanSchemaDefinition(value = true)
        serializeAndDeserialize<PropertyDefinition>(original, "true", json)
    }

    @Test
    fun `round-trip false boolean schema`() {
        val original = BooleanSchemaDefinition(value = false)
        serializeAndDeserialize<PropertyDefinition>(original, "false", json)
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

        val arrayDef = deserializeAndSerialize<ArrayPropertyDefinition>(jsonString, json)

        arrayDef.items.shouldNotBeNull()
        arrayDef.items.shouldBeInstanceOf<BooleanSchemaDefinition>()
        arrayDef.items.value shouldBe false
    }

    @Test
    fun `oneOf with boolean schemas`() {
        val jsonString =
            """
            {
                "oneOf": [
                  true,
                  false,
                  {
                    "type": "string"
                  }
                ]
            }
            """.trimIndent()

        val oneOfDef = deserializeAndSerialize<OneOfPropertyDefinition>(jsonString, json)

        oneOfDef.oneOf.size shouldBe 3
        oneOfDef.oneOf[0].shouldBeInstanceOf<BooleanSchemaDefinition>()
        (oneOfDef.oneOf[0] as BooleanSchemaDefinition).value shouldBe true
        oneOfDef.oneOf[1].shouldBeInstanceOf<BooleanSchemaDefinition>()
        (oneOfDef.oneOf[1] as BooleanSchemaDefinition).value shouldBe false
        oneOfDef.oneOf[2].shouldBeInstanceOf<StringPropertyDefinition>()
    }

    @Test
    fun `anyOf with boolean schemas`() {
        val jsonString =
            """
            {
                "anyOf": [
                  true,
                  {
                    "type": "number"
                  }
                ]
            }
            """.trimIndent()

        val anyOfDef = deserializeAndSerialize<AnyOfPropertyDefinition>(jsonString, json)

        anyOfDef.anyOf.size shouldBe 2
        anyOfDef.anyOf[0].shouldBeInstanceOf<BooleanSchemaDefinition>()
        (anyOfDef.anyOf[0] as BooleanSchemaDefinition).value shouldBe true
        anyOfDef.anyOf[1].shouldBeInstanceOf<NumericPropertyDefinition>()
    }

    @Test
    fun `allOf with boolean schemas`() {
        val jsonString =
            """
            {
                "allOf": [
                  true,
                  {
                    "type": "string",
                    "minLength": 1
                  }
                ]
            }
            """.trimIndent()

        val allOfDef = deserializeAndSerialize<AllOfPropertyDefinition>(jsonString, json)

        allOfDef.allOf.size shouldBe 2
        allOfDef.allOf[0].shouldBeInstanceOf<BooleanSchemaDefinition>()
        (allOfDef.allOf[0] as BooleanSchemaDefinition).value shouldBe true
        allOfDef.allOf[1].shouldBeInstanceOf<StringPropertyDefinition>()
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

        val objectDef = deserializeAndSerialize<ObjectPropertyDefinition>(jsonString, json)

        objectDef.properties.shouldNotBeNull()
        objectDef.properties.size shouldBe 2

        val anyValue = objectDef.properties["anyValue"]
        anyValue.shouldBeInstanceOf<BooleanSchemaDefinition>()
        anyValue.value shouldBe true

        val neverValid = objectDef.properties["neverValid"]
        neverValid.shouldBeInstanceOf<BooleanSchemaDefinition>()
        neverValid.value shouldBe false
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
        booleanSchema.shouldNotBeNull()
        booleanSchema.value shouldBe true

        val notBoolean = objectDef.booleanSchemaProperty("regularString")
        notBoolean shouldBe null

        val notExists = objectDef.booleanSchemaProperty("doesNotExist")
        notExists shouldBe null
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

        val decoded =
            serializeAndDeserialize(
                objectDef,
                """
                {
                  "type": "object",
                  "properties": {
                    "acceptAll": true,
                    "rejectAll": false,
                    "normalString": {
                      "type": "string"
                    }
                  }
                }
                """.trimIndent(),
                json,
            )

        decoded.properties.shouldNotBeNull()
        decoded.properties.size shouldBe 3

        val acceptAll = decoded.properties["acceptAll"]
        acceptAll.shouldBeInstanceOf<BooleanSchemaDefinition>()
        acceptAll.value shouldBe true

        val rejectAll = decoded.properties["rejectAll"]
        rejectAll.shouldBeInstanceOf<BooleanSchemaDefinition>()
        rejectAll.value shouldBe false

        val normalString = decoded.properties["normalString"]
        normalString.shouldBeInstanceOf<StringPropertyDefinition>()
    }
}
