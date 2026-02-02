@file:OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)

package kotlinx.schema.json.serializers

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonEncoder

/**
 * Generic serializer for typed enum lists in JSON Schema.
 *
 * This serializer handles the common pattern of deserializing JSON arrays into typed lists
 * and serializing them back. It reduces code duplication across type-specific enum serializers.
 *
 * @param T The target type for enum values (e.g., String, Double, Boolean, JsonArray, JsonObject)
 * @param descriptorName The name used for the serial descriptor
 * @param elementConverter Converts a JsonElement to T during deserialization
 * @param elementSerializer Converts T to JsonElement during serialization
 */
internal class TypedEnumSerializer<T>(
    descriptorName: String,
    private val elementConverter: (JsonElement) -> T,
    private val elementSerializer: (T) -> JsonElement,
) : KSerializer<List<T>?> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor(descriptorName)

    override fun deserialize(decoder: Decoder): List<T>? {
        val jsonDecoder =
            decoder as? JsonDecoder
                ?: throw SerializationException("${descriptor.serialName} can only be used with JSON")

        return when (val element = jsonDecoder.decodeJsonElement()) {
            is JsonArray -> element.map(elementConverter)
            else -> throw SerializationException(
                "Expected JsonArray for enum, got ${element::class.simpleName}",
            )
        }
    }

    override fun serialize(
        encoder: Encoder,
        value: List<T>?,
    ) {
        val jsonEncoder =
            encoder as? JsonEncoder
                ?: throw SerializationException("${descriptor.serialName} can only be used with JSON")

        if (value == null) {
            jsonEncoder.encodeNull()
        } else {
            jsonEncoder.encodeJsonElement(JsonArray(value.map(elementSerializer)))
        }
    }
}
