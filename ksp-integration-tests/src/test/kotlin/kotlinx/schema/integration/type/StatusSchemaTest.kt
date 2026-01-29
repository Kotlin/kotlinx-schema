package kotlinx.schema.integration.type

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
              "$id": "kotlinx.schema.integration.type.Status",
              "$schema": "https://json-schema.org/draft/2020-12/schema",
              "type": "string",
              "enum": ["ACTIVE", "INACTIVE", "PENDING"],
              "description": "Current lifecycle status of an entity."
            }
            """.trimIndent()
    }
}
