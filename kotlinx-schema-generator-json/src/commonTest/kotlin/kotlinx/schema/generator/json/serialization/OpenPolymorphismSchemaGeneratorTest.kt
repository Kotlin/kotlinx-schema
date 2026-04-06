@file:Suppress("RUNTIME_ANNOTATION_NOT_SUPPORTED")

package kotlinx.schema.generator.json.serialization

import io.kotest.assertions.json.shouldEqualJson
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.string.shouldContain
import kotlinx.schema.generator.json.SerialDescription
import kotlinx.serialization.PolymorphicSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import kotlin.test.Test

@Suppress("ANNOTATION_TARGETS_IN_JS")
class OpenPolymorphismSchemaGeneratorTest {
    @Suppress("AbstractClassCanBeInterface")
    @Serializable
    @SerialName("Flying")
    @SerialDescription("Something that flies")
    abstract class Flying {
        abstract val name: String
    }

    @Serializable
    @SerialName("Bird")
    @SerialDescription("A flying bird")
    data class Bird(
        @property:SerialDescription("Bird name") override val name: String,
        @property:SerialDescription("Wingspan in meters") val wingspan: Double,
    ) : Flying()

    @Serializable
    @SerialName("Kite")
    @SerialDescription("A man-made kite")
    data class Kite(
        @property:SerialDescription("Kite name") override val name: String,
        @property:SerialDescription("Material used") val material: String,
    ) : Flying()

    @Test
    fun `subtypes from SerializersModule`() {
        val module =
            SerializersModule {
                polymorphic(Flying::class) {
                    subclass(Bird::class)
                    subclass(Kite::class)
                }
            }

        val generator =
            SerializationClassJsonSchemaGenerator(
                json = Json { serializersModule = module },
            )

        val schema = generator.generateSchemaString(PolymorphicSerializer(Flying::class).descriptor)

        schema shouldEqualJson
            // language=JSON
            $$"""
            {
              "$schema": "https://json-schema.org/draft/2020-12/schema",
              "$id": "Flying",
              "type": "object",
              "additionalProperties": false,
              "oneOf": [
                { "$ref": "#/$defs/Bird" },
                { "$ref": "#/$defs/Kite" }
              ],
              "$defs": {
                "Bird": {
                  "type": "object",
                  "description": "A flying bird",
                  "properties": {
                    "type": { "type": "string", "const": "Bird" },
                    "name": { "type": "string", "description": "Bird name" },
                    "wingspan": { "type": "number", "description": "Wingspan in meters" }
                  },
                  "required": ["type", "name", "wingspan"],
                  "additionalProperties": false
                },
                "Kite": {
                  "type": "object",
                  "description": "A man-made kite",
                  "properties": {
                    "type": { "type": "string", "const": "Kite" },
                    "name": { "type": "string", "description": "Kite name" },
                    "material": { "type": "string", "description": "Material used" }
                  },
                  "required": ["type", "name", "material"],
                  "additionalProperties": false
                }
              }
            }
            """.trimIndent()
    }

    @Test
    fun `partial module includes only registered subtypes`() {
        val module =
            SerializersModule {
                polymorphic(Flying::class) {
                    subclass(Kite::class)
                }
            }

        val generator =
            SerializationClassJsonSchemaGenerator(
                json = Json { serializersModule = module },
            )

        val schema = generator.generateSchemaString(PolymorphicSerializer(Flying::class).descriptor)

        schema shouldEqualJson
            // language=JSON
            $$"""
            {
              "$schema": "https://json-schema.org/draft/2020-12/schema",
              "$id": "Flying",
              "type": "object",
              "additionalProperties": false,
              "oneOf": [
                { "$ref": "#/$defs/Kite" }
              ],
              "$defs": {
                "Kite": {
                  "type": "object",
                  "description": "A man-made kite",
                  "properties": {
                    "type": { "type": "string", "const": "Kite" },
                    "name": { "type": "string", "description": "Kite name" },
                    "material": { "type": "string", "description": "Material used" }
                  },
                  "required": ["type", "name", "material"],
                  "additionalProperties": false
                }
              }
            }
            """.trimIndent()
    }

    @Test
    fun `empty module fails with descriptive message`() {
        val generator = SerializationClassJsonSchemaGenerator()

        val exception =
            shouldThrow<IllegalStateException> {
                generator.generateSchemaString(PolymorphicSerializer(Flying::class).descriptor)
            }

        exception.message shouldContain "No subtypes registered in SerializersModule"
        exception.message shouldContain "Flying"
    }
}
