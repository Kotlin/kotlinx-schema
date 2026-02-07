package kotlinx.schema.integration.type

import io.kotest.assertions.json.shouldEqualJson
import kotlin.test.Test

/**
 * Integration tests verifying that Jakarta Validation annotations (like @Min)
 * are correctly extracted and mapped to JSON Schema constraints via KSP.
 */
class JakartaValidationTest {
    @Test
    fun `Should generate jsonSchema with min validation from Jakarta annotation`() {
        val schema = JakartaValidationModel::class.jsonSchemaString

        // language=json
        schema shouldEqualJson
            """
            {
              "${"$"}schema": "https://json-schema.org/draft/2020-12/schema",
              "${"$"}id": "kotlinx.schema.integration.type.JakartaValidationModel",
              "description": "Test model for verifying validation constraint extraction via KSP.",
              "type": "object",
              "properties": {
                "age": { 
                    "type": "integer", 
                    "description": null,
                    "minimum": 5.0 
                }
              },
              "required": [
                "age"
              ],
              "additionalProperties": false
            }
            """.trimIndent()
    }
}
