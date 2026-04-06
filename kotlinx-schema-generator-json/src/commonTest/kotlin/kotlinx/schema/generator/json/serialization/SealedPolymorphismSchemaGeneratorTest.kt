@file:Suppress("RUNTIME_ANNOTATION_NOT_SUPPORTED")

package kotlinx.schema.generator.json.serialization

import io.kotest.assertions.json.shouldEqualJson
import kotlinx.schema.generator.json.SerialDescription
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import kotlin.test.Test

class SealedPolymorphismSchemaGeneratorTest {
    @Serializable
    @SerialName("Shape")
    @SerialDescription("A geometric shape")
    sealed class Shape {
        @Serializable
        @SerialName("Circle")
        @SerialDescription("A circle")
        data class Circle(
            @property:SerialDescription("Circle radius") val radius: Double,
        ) : Shape()

        @Serializable
        @SerialName("Rect")
        @SerialDescription("A rectangle")
        data class Rect(
            @property:SerialDescription("Rectangle width") val w: Double,
        ) : Shape()
    }

    @Test
    fun `custom classDiscriminator from Json config`() {
        val module =
            SerializersModule {
                polymorphic(Shape::class) {
                    subclass(Shape.Circle::class)
                    subclass(Shape.Rect::class)
                }
            }

        val generator =
            SerializationClassJsonSchemaGenerator(
                json =
                    Json {
                        serializersModule = module
                        classDiscriminator = "kind"
                    },
            )

        val schema = generator.generateSchemaString(Shape.serializer().descriptor)

        schema shouldEqualJson
            // language=JSON
            $$"""
            {
              "$schema": "https://json-schema.org/draft/2020-12/schema",
              "$id": "Shape",
              "description": "A geometric shape",
              "type": "object",
              "additionalProperties": false,
              "oneOf": [
                { "$ref": "#/$defs/Circle" },
                { "$ref": "#/$defs/Rect" }
              ],
              "$defs": {
                "Circle": {
                  "type": "object",
                  "description": "A circle",
                  "properties": {
                    "kind": { "type": "string", "const": "Circle" },
                    "radius": { "type": "number", "description": "Circle radius" }
                  },
                  "required": ["kind", "radius"],
                  "additionalProperties": false
                },
                "Rect": {
                  "type": "object",
                  "description": "A rectangle",
                  "properties": {
                    "kind": { "type": "string", "const": "Rect" },
                    "w": { "type": "number", "description": "Rectangle width" }
                  },
                  "required": ["kind", "w"],
                  "additionalProperties": false
                }
              }
            }
            """.trimIndent()
    }

    @Test
    fun `SerializersModule does not filter sealed subtypes - all come from SerialDescriptor`() {
        val partialModule =
            SerializersModule {
                polymorphic(Shape::class) {
                    subclass(Shape.Rect::class)
                }
            }

        val generator =
            SerializationClassJsonSchemaGenerator(
                json = Json { serializersModule = partialModule },
            )

        val schema = generator.generateSchemaString(Shape.serializer().descriptor)

        schema shouldEqualJson
            // language=JSON
            $$"""
            {
              "$schema": "https://json-schema.org/draft/2020-12/schema",
              "$id": "Shape",
              "description": "A geometric shape",
              "type": "object",
              "additionalProperties": false,
              "oneOf": [
                { "$ref": "#/$defs/Circle" },
                { "$ref": "#/$defs/Rect" }
              ],
              "$defs": {
                "Circle": {
                  "type": "object",
                  "description": "A circle",
                  "properties": {
                    "type": { "type": "string", "const": "Circle" },
                    "radius": { "type": "number", "description": "Circle radius" }
                  },
                  "required": ["type", "radius"],
                  "additionalProperties": false
                },
                "Rect": {
                  "type": "object",
                  "description": "A rectangle",
                  "properties": {
                    "type": { "type": "string", "const": "Rect" },
                    "w": { "type": "number", "description": "Rectangle width" }
                  },
                  "required": ["type", "w"],
                  "additionalProperties": false
                }
              }
            }
            """.trimIndent()
    }
}
