package kotlinx.schema.json

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlin.test.Test

class StringOrListSerializerTest {
    @Test
    fun `deserialize single string to list`() {
        val jsonInput = "\"singleValue\""
        val expected = listOf("singleValue")

        val result = Json.decodeFromString(StringOrListSerializer(), jsonInput)

        result shouldBe expected
    }

    @Test
    fun `deserialize json array to list`() {
        val jsonInput = "[\"value1\", \"value2\"]"
        val expected = listOf("value1", "value2")

        val result = Json.decodeFromString(StringOrListSerializer(), jsonInput)

        result shouldBe expected
    }

    @Test
    fun `deserialize empty json array to empty list`() {
        val jsonInput = "[]"
        val expected = emptyList<String>()

        val result = Json.decodeFromString(StringOrListSerializer(), jsonInput)

        result shouldBe expected
    }

    @Test
    fun `deserialize null content treats as empty string`() {
        // Based on the logic: (it as? JsonPrimitive)?.contentOrNull ?: ""
        val jsonInput = "[null]"
        val expected = listOf("")

        val result = Json.decodeFromString(StringOrListSerializer(), jsonInput)

        result shouldBe expected
    }

    @Test
    fun `deserialize throws exception on invalid json type`() {
        val jsonInput = "{}" // Object is not supported

        shouldThrow<SerializationException> { Json.decodeFromString(StringOrListSerializer(), jsonInput) }
    }

    @Test
    fun `serialize single element list to string`() {
        val input = listOf("singleValue")
        val expectedJson = "\"singleValue\""

        val result = Json.encodeToString(StringOrListSerializer(), input)

        result shouldBe expectedJson
    }

    @Test
    fun `serialize multiple elements list to json array`() {
        val input = listOf("value1", "value2")
        val expectedJson = "[\"value1\",\"value2\"]"

        val result = Json.encodeToString(StringOrListSerializer(), input)

        result shouldBe expectedJson
    }

    @Test
    fun `serialize empty list to empty json array`() {
        val input = emptyList<String>()
        val expectedJson = "[]"

        val result = Json.encodeToString(StringOrListSerializer(), input)

        result shouldBe expectedJson
    }
}
