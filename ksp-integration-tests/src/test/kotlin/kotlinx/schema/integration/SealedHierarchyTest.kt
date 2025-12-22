@file:Suppress("JsonStandardCompliance")

package kotlinx.schema.integration

import io.kotest.assertions.json.shouldEqualJson
import io.kotest.matchers.shouldNotBe
import kotlin.test.Test

/**
 * Integration tests that verify KSP-generated extension properties work correctly for sealed classes
 */
@Suppress("LongMethod")
class SealedHierarchyTest {
    @Test
    fun `Sealed hierarchy should generate polymorphic schema with discriminator`() {
        Animal::class.jsonSchema shouldNotBe null
        val animalSchemaString = Animal::class.jsonSchemaString

        // language=json
        animalSchemaString shouldEqualJson
            $$"""
            {
              "$id": "kotlinx.schema.integration.Animal",
              "$defs": {
                "kotlinx.schema.integration.Animal": {
                  "oneOf": [
                    { "$ref": "#/$defs/kotlinx.schema.integration.Animal.Cat" },
                    { "$ref": "#/$defs/kotlinx.schema.integration.Animal.Dog" }
                  ],
                  "description": "Multicellular eukaryotic organism of the kingdom Metazoa"
                },
                "kotlinx.schema.integration.Animal.Cat": {
                  "type": "object",
                  "properties": {
                    "name": { "type": "string" },
                    "type": { "const": "kotlinx.schema.integration.Animal.Cat" }
                  },
                  "required": [ "name", "type" ],
                  "additionalProperties": false
                },
                "kotlinx.schema.integration.Animal.Dog": {
                  "type": "object",
                  "properties": {
                    "name": { "type": "string" },
                    "type": { "const": "kotlinx.schema.integration.Animal.Dog" }
                  },
                  "required": [ "name", "type" ],
                  "additionalProperties": false
                }
              },
              "$ref": "#/$defs/kotlinx.schema.integration.Animal"
            }
            """.trimIndent()

        Animal.Cat::class.jsonSchema shouldNotBe null
        Animal.Dog::class.jsonSchema shouldNotBe null
    }
}
