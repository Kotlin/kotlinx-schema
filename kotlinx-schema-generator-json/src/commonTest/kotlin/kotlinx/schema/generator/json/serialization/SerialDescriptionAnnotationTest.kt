package kotlinx.schema.generator.json.serialization

import io.kotest.assertions.json.shouldEqualJson
import kotlinx.schema.SerialDescription
import kotlinx.serialization.Serializable
import kotlin.test.Test

class SerialDescriptionAnnotationTest {
    @Serializable
    @SerialDescription("A described class")
    data class DescribedClass(
        @property:SerialDescription("A described property")
        val name: String,
        val undescribed: Int,
    )

    @Serializable
    @SerialDescription("Outer class")
    data class OuterClass(
        @property:SerialDescription("Nested reference")
        val nested: DescribedClass,
    )

    val generator = SerializationClassJsonSchemaGenerator.Default

    @Test
    fun `@SerialDescription on class and property appears in schema without custom extractor`() {
        val schema = generator.generateSchemaString(DescribedClass.serializer().descriptor)

        schema shouldEqualJson
            // language=JSON
            $$"""
            {
              "$schema": "https://json-schema.org/draft/2020-12/schema",
              "$id": "kotlinx.schema.generator.json.serialization.SerialDescriptionAnnotationTest.DescribedClass",
              "description": "A described class",
              "type": "object",
              "properties": {
                "name": {
                  "type": "string",
                  "description": "A described property"
                },
                "undescribed": {
                  "type": "integer"
                }
              },
              "required": ["name", "undescribed"],
              "additionalProperties": false
            }
            """.trimIndent()
    }

    @Test
    fun `@SerialDescription propagates to defs entry`() {
        val schema = generator.generateSchemaString(OuterClass.serializer().descriptor)

        schema shouldEqualJson
            // language=JSON
            $$"""
            {
              "$schema": "https://json-schema.org/draft/2020-12/schema",
              "$id": "kotlinx.schema.generator.json.serialization.SerialDescriptionAnnotationTest.OuterClass",
              "description": "Outer class",
              "type": "object",
              "properties": {
                "nested": {
                  "$ref": "#/$defs/kotlinx.schema.generator.json.serialization.SerialDescriptionAnnotationTest.DescribedClass",
                  "description": "Nested reference"
                }
              },
              "required": ["nested"],
              "additionalProperties": false,
              "$defs": {
                "kotlinx.schema.generator.json.serialization.SerialDescriptionAnnotationTest.DescribedClass": {
                  "description": "A described class",
                  "type": "object",
                  "properties": {
                    "name": {
                      "type": "string",
                      "description": "A described property"
                    },
                    "undescribed": {
                      "type": "integer"
                    }
                  },
                  "required": ["name", "undescribed"],
                  "additionalProperties": false
                }
              }
            }
            """.trimIndent()
    }
}
