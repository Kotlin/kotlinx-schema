package kotlinx.schema.integration.type

import io.kotest.assertions.json.shouldEqualJson
import kotlin.test.Test

/**
 * Tests for Animal schema generation - sealed class polymorphism.
 */
class AnimalSchemaTest {
    @Test
    fun `generates polymorphic schema with oneOf composition`() {
        val schema = Animal::class.jsonSchemaString

        // language=json
        schema shouldEqualJson
            $$"""
            {
              "$id": "kotlinx.schema.integration.type.Animal",
              "$defs": {
                "kotlinx.schema.integration.type.Animal": {
                  "oneOf": [
                    { "$ref": "#/$defs/kotlinx.schema.integration.type.Animal.Cat" },
                    { "$ref": "#/$defs/kotlinx.schema.integration.type.Animal.Dog" }
                  ],
                  "description": "Multicellular eukaryotic organism of the kingdom Metazoa"
                },
                "kotlinx.schema.integration.type.Animal.Cat": {
                  "type": "object",
                  "properties": {
                    "name": {
                      "type": "string",
                      "description": "Animal's name"
                    },
                    "type": {
                      "const": "kotlinx.schema.integration.type.Animal.Cat"
                    }
                  },
                  "required": ["name", "type"],
                  "additionalProperties": false
                },
                "kotlinx.schema.integration.type.Animal.Dog": {
                  "type": "object",
                  "properties": {
                    "name": {
                      "type": "string",
                      "description": "Animal's name"
                    },
                    "type": {
                      "const": "kotlinx.schema.integration.type.Animal.Dog"
                    }
                  },
                  "required": ["name", "type"],
                  "additionalProperties": false
                }
              },
              "$ref": "#/$defs/kotlinx.schema.integration.type.Animal"
            }
            """.trimIndent()
    }
}
