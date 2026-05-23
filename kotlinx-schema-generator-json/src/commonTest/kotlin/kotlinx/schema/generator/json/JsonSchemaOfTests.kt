package kotlinx.schema.generator.json

import io.kotest.assertions.json.shouldEqualJson
import kotlinx.schema.json.encodeToString
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.test.Test

class JsonSchemaOfTests {
    @Serializable
    @SerialName("TestClass")
    data class TestClass(
        val nested: String,
    )

    @Test
    fun `generates with default generator`() {
        val schema = jsonSchemaOf<TestClass>()
        val schemaJson = schema.encodeToString(json)

        schemaJson shouldEqualJson
            // language=JSON
            $$"""
            {
              "$schema": "https://json-schema.org/draft/2020-12/schema",
              "$id": "TestClass",
              "type": "object",
              "properties": {
                "nested": {
                  "type": "string",
                }
              },
              "additionalProperties": false,
              "required": [
                "nested"
              ]
            }
            """.trimIndent()
    }
}
