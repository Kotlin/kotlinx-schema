@file:Suppress("JsonStandardCompliance")

package kotlinx.schema.integration

import io.kotest.assertions.json.shouldEqualSpecifiedJson
import io.kotest.matchers.shouldNotBe
import kotlin.test.Test

/**
 * Integration tests that verify KSP-generated extension properties work correctly
 */
@Suppress("LongMethod")
class SealedHierarchyTest {
    @Test
    fun `Sealed hierarchy should have generated jsonSchemaString extension`() {
        Animal::class.jsonSchema shouldNotBe null
        val animalSchemaString = Animal::class.jsonSchemaString

        // language=json
        animalSchemaString shouldEqualSpecifiedJson
            $$"""
            {
              "$id": "kotlinx.schema.integration.Animal",
              "$defs": {
                "kotlinx.schema.integration.Animal": {
                  "type": "object",
                  "properties": {
                    "name": { "type": "string", "description": "Animal's name" }
                  },
                  "required": [
                    "name"
                  ],
                  "additionalProperties": false,
                  "description": "Multicellular eukaryotic organism of the kingdom Metazoa"
                }
              },
              "$ref": "#/$defs/kotlinx.schema.integration.Animal"
            }
            """.trimIndent()

        Animal.Cat::class.jsonSchema shouldNotBe null
        Animal.Dog::class.jsonSchema shouldNotBe null
    }
}
