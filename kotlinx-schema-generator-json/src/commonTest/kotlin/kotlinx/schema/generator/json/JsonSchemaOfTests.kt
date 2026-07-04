package kotlinx.schema.generator.json

import io.kotest.assertions.json.shouldEqualJson
import kotlinx.schema.generator.json.serialization.CustomDescription
import kotlinx.schema.generator.json.serialization.SerializationClassJsonSchemaGenerator
import kotlinx.schema.generator.json.serialization.SerializationClassSchemaIntrospector
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

    @Serializable
    @SerialName("Described")
    @CustomDescription("A described class")
    data class Described(
        val value: String,
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
                  "type": "string"
                }
              },
              "additionalProperties": false,
              "required": [
                "nested"
              ]
            }
            """.trimIndent()
    }

    @Test
    fun `threads the supplied generator`() {
        val generator =
            SerializationClassJsonSchemaGenerator(
                introspectorConfig =
                    SerializationClassSchemaIntrospector.Config(
                        descriptionExtractor = { annotations ->
                            annotations.filterIsInstance<CustomDescription>().firstOrNull()?.value
                        },
                    ),
            )

        val schemaJson = jsonSchemaOf<Described>(generator).encodeToString(json)

        // The default generator would not pick up @CustomDescription, so a passing "description"
        // proves the supplied generator (not Default) is the one that produced the schema.
        schemaJson shouldEqualJson
            // language=JSON
            $$"""
            {
              "$schema": "https://json-schema.org/draft/2020-12/schema",
              "$id": "Described",
              "description": "A described class",
              "type": "object",
              "properties": {
                "value": {
                  "type": "string"
                }
              },
              "additionalProperties": false,
              "required": [
                "value"
              ]
            }
            """.trimIndent()
    }
}
