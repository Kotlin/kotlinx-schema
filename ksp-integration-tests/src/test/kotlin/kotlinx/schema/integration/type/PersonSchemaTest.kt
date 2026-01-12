package kotlinx.schema.integration.type

import io.kotest.assertions.json.shouldEqualJson
import kotlin.test.Test

/**
 * Tests for Person schema generation - simple data class with primitives.
 */
class PersonSchemaTest {
    @Test
    fun `generates complete schema with all required fields`() {
        val schema = Person::class.jsonSchemaString

        // language=json
        schema shouldEqualJson
            $$"""
            {
              "$id": "kotlinx.schema.integration.type.Person",
              "$defs": {
                "kotlinx.schema.integration.type.Person": {
                  "type": "object",
                  "properties": {
                    "firstName": {
                      "type": "string",
                      "description": "Given name of the person"
                    },
                    "lastName": {
                      "type": "string",
                      "description": "Family name of the person"
                    },
                    "age": {
                      "type": "integer",
                      "description": "Age of the person in years"
                    }
                  },
                  "required": ["firstName", "lastName", "age"],
                  "additionalProperties": false,
                  "description": "A person with a first and last name and age."
                }
              },
              "$ref": "#/$defs/kotlinx.schema.integration.type.Person"
            }
            """.trimIndent()
    }
}
