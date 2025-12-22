package kotlinx.schema.integration

import io.kotest.assertions.json.shouldEqualJson
import kotlin.test.Test

/**
 * Tests for Status schema generation - enum handling.
 */
class StatusSchemaTest {
    @Test
    fun `generates enum schema with all values`() {
        val schema = Status::class.jsonSchemaString

        // language=json
        schema shouldEqualJson
            $$"""
            {
              "$id": "kotlinx.schema.integration.Status",
              "$defs": {
                "kotlinx.schema.integration.Status": {
                  "type": "string",
                  "enum": ["ACTIVE", "INACTIVE", "PENDING"],
                  "description": "Current lifecycle status of an entity."
                }
              },
              "$ref": "#/$defs/kotlinx.schema.integration.Status"
            }
            """.trimIndent()
    }
}
