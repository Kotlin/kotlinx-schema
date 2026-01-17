package kotlinx.schema.json

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlin.test.Test

/**
 * Tests for typed enum serializers: ArrayEnumSerializer, BooleanEnumSerializer,
 * NumericEnumSerializer, ObjectEnumSerializer.
 */
class EnumSerializerTest {
    private val json = Json { prettyPrint = true }

    // ArrayEnumSerializer Tests

    @Test
    fun `ArrayEnumSerializer deserializes array of arrays`() {
        // language=json
        val jsonString =
            """
            {
              "type": "array",
              "enum": [
                [1, 2, 3],
                ["a", "b"],
                []
              ]
            }
            """.trimIndent()

        val decoded = json.decodeFromString<ArrayPropertyDefinition>(jsonString)

        decoded.enum shouldBe
            listOf(
                JsonArray(listOf(JsonPrimitive(1), JsonPrimitive(2), JsonPrimitive(3))),
                JsonArray(listOf(JsonPrimitive("a"), JsonPrimitive("b"))),
                JsonArray(emptyList()),
            )
    }

    @Test
    fun `ArrayEnumSerializer serializes array of arrays`() {
        // language=json
        val jsonString =
            """
            {
              "type": "array",
              "enum": [
                [1, 2],
                ["x", "y"]
              ]
            }
            """.trimIndent()

        deserializeAndSerialize<ArrayPropertyDefinition>(jsonString, json)
    }

    @Test
    fun `ArrayEnumSerializer rejects non-array values`() {
        // language=json
        val jsonString =
            """
            {
              "type": "array",
              "enum": [
                [1, 2],
                "not an array"
              ]
            }
            """.trimIndent()

        shouldThrow<SerializationException> {
            json.decodeFromString<ArrayPropertyDefinition>(jsonString)
        }
    }

    @Test
    fun `ArrayEnumSerializer serializes null enum`() {
        // language=json
        val jsonString =
            """
            {
              "type": "array"
            }
            """.trimIndent()

        deserializeAndSerialize<ArrayPropertyDefinition>(jsonString, json)
    }

    // BooleanEnumSerializer Tests

    @Test
    fun `BooleanEnumSerializer deserializes boolean values`() {
        // language=json
        val jsonString =
            """
            {
              "type": "boolean",
              "enum": [true, false]
            }
            """.trimIndent()

        val decoded = json.decodeFromString<BooleanPropertyDefinition>(jsonString)

        decoded.enum shouldBe listOf(true, false)
    }

    @Test
    fun `BooleanEnumSerializer deserializes string boolean values`() {
        // language=json
        val jsonString =
            """
            {
              "type": "boolean",
              "enum": ["true", "false"]
            }
            """.trimIndent()

        val decoded = json.decodeFromString<BooleanPropertyDefinition>(jsonString)

        decoded.enum shouldBe listOf(true, false)
    }

    @Test
    fun `BooleanEnumSerializer deserializes numeric boolean values`() {
        // language=json
        val jsonString =
            """
            {
              "type": "boolean",
              "enum": [1, 0, 42, 0.0]
            }
            """.trimIndent()

        val decoded = json.decodeFromString<BooleanPropertyDefinition>(jsonString)

        decoded.enum shouldBe listOf(true, false, true, false)
    }

    @Test
    fun `BooleanEnumSerializer serializes boolean values`() {
        // language=json
        val jsonString =
            """
            {
              "type": "boolean",
              "enum": [true, false, true]
            }
            """.trimIndent()

        deserializeAndSerialize<BooleanPropertyDefinition>(jsonString, json)
    }

    @Test
    fun `BooleanEnumSerializer rejects non-boolean convertible values`() {
        // language=json
        val jsonString =
            """
            {
              "type": "boolean",
              "enum": ["invalid"]
            }
            """.trimIndent()

        shouldThrow<SerializationException> {
            json.decodeFromString<BooleanPropertyDefinition>(jsonString)
        }
    }

    @Test
    fun `BooleanEnumSerializer rejects array values`() {
        // language=json
        val jsonString =
            """
            {
              "type": "boolean",
              "enum": [[true]]
            }
            """.trimIndent()

        shouldThrow<SerializationException> {
            json.decodeFromString<BooleanPropertyDefinition>(jsonString)
        }
    }

    @Test
    fun `BooleanEnumSerializer serializes null enum`() {
        // language=json
        val jsonString =
            """
            {
              "type": "boolean"
            }
            """.trimIndent()

        deserializeAndSerialize<BooleanPropertyDefinition>(jsonString, json)
    }

    // NumericEnumSerializer Tests

    @Test
    fun `NumericEnumSerializer deserializes integer values`() {
        // language=json
        val jsonString =
            """
            {
              "type": "integer",
              "enum": [1, 2, 3, 5, 8]
            }
            """.trimIndent()

        val decoded = json.decodeFromString<NumericPropertyDefinition>(jsonString)

        decoded.enum shouldBe listOf(1.0, 2.0, 3.0, 5.0, 8.0)
    }

    @Test
    fun `NumericEnumSerializer deserializes number values`() {
        // language=json
        val jsonString =
            """
            {
              "type": "number",
              "enum": [1.5, 2.7, 3.14, -0.5]
            }
            """.trimIndent()

        val decoded = json.decodeFromString<NumericPropertyDefinition>(jsonString)

        decoded.enum shouldBe listOf(1.5, 2.7, 3.14, -0.5)
    }

    @Test
    fun `NumericEnumSerializer deserializes string numeric values`() {
        // language=json
        val jsonString =
            """
            {
              "type": "number",
              "enum": ["42", "3.14"]
            }
            """.trimIndent()

        val decoded = json.decodeFromString<NumericPropertyDefinition>(jsonString)

        decoded.enum shouldBe listOf(42.0, 3.14)
    }

    @Test
    fun `NumericEnumSerializer serializes numeric values`() {
        // language=json
        val jsonString =
            """
            {
              "type": "number",
              "enum": [1.0, 2.5, 3.0]
            }
            """.trimIndent()

        deserializeAndSerialize<NumericPropertyDefinition>(jsonString, json)
    }

    @Test
    fun `NumericEnumSerializer rejects non-numeric values`() {
        // language=json
        val jsonString =
            """
            {
              "type": "number",
              "enum": ["not a number"]
            }
            """.trimIndent()

        shouldThrow<SerializationException> {
            json.decodeFromString<NumericPropertyDefinition>(jsonString)
        }
    }

    @Test
    fun `NumericEnumSerializer rejects array values`() {
        // language=json
        val jsonString =
            """
            {
              "type": "number",
              "enum": [[1, 2, 3]]
            }
            """.trimIndent()

        shouldThrow<SerializationException> {
            json.decodeFromString<NumericPropertyDefinition>(jsonString)
        }
    }

    @Test
    fun `NumericEnumSerializer serializes null enum`() {
        // language=json
        val jsonString =
            """
            {
              "type": "number"
            }
            """.trimIndent()

        deserializeAndSerialize<NumericPropertyDefinition>(jsonString, json)
    }

    // ObjectEnumSerializer Tests

    @Test
    fun `ObjectEnumSerializer deserializes object values`() {
        // language=json
        val jsonString =
            """
            {
              "type": "object",
              "enum": [
                {"mode": "read"},
                {"mode": "write", "timeout": 30}
              ]
            }
            """.trimIndent()

        val decoded = json.decodeFromString<ObjectPropertyDefinition>(jsonString)

        decoded.enum shouldBe
            listOf(
                JsonObject(mapOf("mode" to JsonPrimitive("read"))),
                JsonObject(
                    mapOf(
                        "mode" to JsonPrimitive("write"),
                        "timeout" to JsonPrimitive(30),
                    ),
                ),
            )
    }

    @Test
    fun `ObjectEnumSerializer deserializes empty objects`() {
        // language=json
        val jsonString =
            """
            {
              "type": "object",
              "enum": [
                {},
                {"key": "value"}
              ]
            }
            """.trimIndent()

        val decoded = json.decodeFromString<ObjectPropertyDefinition>(jsonString)

        decoded.enum shouldBe
            listOf(
                JsonObject(emptyMap()),
                JsonObject(mapOf("key" to JsonPrimitive("value"))),
            )
    }

    @Test
    fun `ObjectEnumSerializer serializes object values`() {
        // language=json
        val jsonString =
            """
            {
              "type": "object",
              "enum": [
                {"status": "active"},
                {"status": "inactive"}
              ]
            }
            """.trimIndent()

        deserializeAndSerialize<ObjectPropertyDefinition>(jsonString, json)
    }

    @Test
    fun `ObjectEnumSerializer rejects non-object values`() {
        // language=json
        val jsonString =
            """
            {
              "type": "object",
              "enum": [
                {"valid": "object"},
                "not an object"
              ]
            }
            """.trimIndent()

        shouldThrow<SerializationException> {
            json.decodeFromString<ObjectPropertyDefinition>(jsonString)
        }
    }

    @Test
    fun `ObjectEnumSerializer rejects array values`() {
        // language=json
        val jsonString =
            """
            {
              "type": "object",
              "enum": [
                [1, 2, 3]
              ]
            }
            """.trimIndent()

        shouldThrow<SerializationException> {
            json.decodeFromString<ObjectPropertyDefinition>(jsonString)
        }
    }

    @Test
    fun `ObjectEnumSerializer serializes null enum`() {
        // language=json
        val jsonString =
            """
            {
              "type": "object"
            }
            """.trimIndent()

        deserializeAndSerialize<ObjectPropertyDefinition>(jsonString, json)
    }

    // Extension Function Tests

    @Test
    fun `stringListToJsonElements converts strings to JsonPrimitives`() {
        val strings = listOf("one", "two", "three")
        val jsonElements = strings.toJsonElements()

        jsonElements shouldBe
            listOf(
                JsonPrimitive("one"),
                JsonPrimitive("two"),
                JsonPrimitive("three"),
            )
    }

    @Test
    fun `numberListToJsonElements converts numbers to JsonPrimitives`() {
        val numbers = listOf(1, 2.5, 3)
        val jsonElements = numbers.toJsonElements()

        jsonElements shouldBe
            listOf(
                JsonPrimitive(1),
                JsonPrimitive(2.5),
                JsonPrimitive(3),
            )
    }

    @Test
    fun `booleanListToJsonElements converts booleans to JsonPrimitives`() {
        val booleans = listOf(true, false, true)
        val jsonElements = booleans.toJsonElements()

        jsonElements shouldBe
            listOf(
                JsonPrimitive(true),
                JsonPrimitive(false),
                JsonPrimitive(true),
            )
    }
}
