package kotlinx.schema.integration.type

import io.kotest.assertions.json.shouldEqualJson
import kotlin.test.Test

/** KSP end-to-end coverage for #338. */
class BuiltInJsonTypesSchemaTest {
    @Test
    fun `generates schema for built-in kotlinx serialization JSON types`() {
        val schemaString = BuiltInJsonTypes::class.jsonSchemaString

        // language=json
        schemaString shouldEqualJson
            $$"""
            {
              "$schema": "https://json-schema.org/draft/2020-12/schema",
              "$id": "kotlinx.schema.integration.type.BuiltInJsonTypes",
              "type": "object",
              "properties": {
                "element": {},
                "obj": {
                  "type": "object",
                  "additionalProperties": {}
                },
                "arr": {
                  "type": "array",
                  "items": {}
                },
                "prim": {},
                "nullableElement": {}
              },
              "required": ["element", "obj", "arr", "prim", "nullableElement"],
              "additionalProperties": false
            }
            """.trimIndent()
    }
}
