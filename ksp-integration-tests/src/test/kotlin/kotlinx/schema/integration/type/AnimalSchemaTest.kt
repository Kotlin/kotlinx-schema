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
              "$schema": "https://json-schema.org/draft/2020-12/schema",
              "$id": "kotlinx.schema.integration.type.Animal",
              "description": "Multicellular eukaryotic organism of the kingdom Metazoa",
              "type": "object",
              "additionalProperties": false,
              "oneOf": [
                { "$ref": "#/$defs/kotlinx.schema.integration.type.Animal.Cat" },
                { "$ref": "#/$defs/kotlinx.schema.integration.type.Animal.Dog" }
              ],
              "$defs": {
                "kotlinx.schema.integration.type.Animal.Cat": {
                  "type": "object",
                  "properties": {
                    "name": {
                      "type": "string",
                      "description": "Animal's name"
                    }
                  },
                  "required": ["name"],
                  "additionalProperties": false
                },
                "kotlinx.schema.integration.type.Animal.Dog": {
                  "type": "object",
                  "properties": {
                    "name": {
                      "type": "string",
                      "description": "Animal's name"
                    }
                  },
                  "required": ["name"],
                  "additionalProperties": false
                }
              }
            }
            """.trimIndent()
    }
}
