package kotlinx.schema.json

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

/**
 * A custom serializer for handling JSON values that can either be a single string
 * or a list of strings. This serializer ensures that both formats are correctly
 * deserialized into a `List<String>` and serialized back into their appropriate
 * JSON representation.
 *
 * This is particularly useful in cases where a JSON API may respond with a single
 * string if there is only one value, or a list of strings if there are multiple values.
 *
 * This serializer's behavior:
 * - During deserialization:
 *   - If the value is a string, it converts it into a single-element list.
 *   - If the value is an array of strings, it converts it into a list of strings.
 * - During serialization:
 *   - If the list contains exactly one element, it serializes it as a single string.
 *   - If the list size is not equal to 1 (including empty lists), it serializes it as a JSON array.
 *     Empty lists become empty JSON arrays [].
 *
 * This serializer is compatible only with JSON encoding and decoding.
 *
 * Throws:
 * - `SerializationException` if the serializer is used with a format other than JSON.
 * - `SerializationException` if the input during deserialization is neither a string
 *   nor an array of strings.
 *
 * @author Konstantin Pavlov
 */
public class StringOrListSerializer : KSerializer<List<String>> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("StringOrList")

    override fun deserialize(decoder: Decoder): List<String> {
        val jsonDecoder =
            decoder as? JsonDecoder
                ?: throw SerializationException("This serializer can only be used with JSON")

        return when (val element = jsonDecoder.decodeJsonElement()) {
            is JsonPrimitive -> listOf(element.contentOrNull ?: "")
            is JsonArray ->
                element.map {
                    (it as? JsonPrimitive)?.contentOrNull ?: ""
                }

            else -> throw SerializationException("Expected string or array of strings")
        }
    }

    override fun serialize(
        encoder: Encoder,
        value: List<String>,
    ) {
        val jsonEncoder =
            encoder as? JsonEncoder
                ?: throw SerializationException("This serializer can only be used with JSON")

        // If there's only one element, serialize as a string
        // Otherwise serialize as an array
        val element =
            if (value.size == 1) {
                JsonPrimitive(value.first())
            } else {
                JsonArray(value.map { JsonPrimitive(it) })
            }

        jsonEncoder.encodeJsonElement(element)
    }
}
