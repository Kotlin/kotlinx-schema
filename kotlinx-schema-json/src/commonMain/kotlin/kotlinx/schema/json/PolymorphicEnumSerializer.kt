@file:OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)

package kotlinx.schema.json

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
import kotlinx.serialization.json.JsonPrimitive
import kotlin.jvm.JvmName

/**
 * Custom serializer for enum fields that supports both List<String> (backward compatibility)
 * and List<JsonElement> (full JSON Schema spec compliance).
 *
 * During deserialization, it reads JSON and returns List<JsonElement>.
 * During serialization, it writes JsonElement as-is.
 *
 * This allows StringPropertyDefinition(enum = listOf("a", "b")) to work while
 * internally storing List<JsonElement>.
 */
public class PolymorphicEnumSerializer : KSerializer<List<JsonElement>?> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("PolymorphicEnum")

    override fun deserialize(decoder: Decoder): List<JsonElement>? {
        val jsonDecoder =
            decoder as? JsonDecoder
                ?: error("PolymorphicEnumSerializer can only be used with JSON")

        val element = jsonDecoder.decodeJsonElement()

        return when (element) {
            is JsonArray -> element.toList()
            else -> throw SerializationException("Expected JsonArray for enum, got ${element::class.simpleName}")
        }
    }

    override fun serialize(
        encoder: Encoder,
        value: List<JsonElement>?,
    ) {
        val jsonEncoder =
            encoder as? JsonEncoder
                ?: error("PolymorphicEnumSerializer can only be used with JSON")

        if (value == null) {
            encoder.encodeNull()
        } else {
            jsonEncoder.encodeJsonElement(JsonArray(value))
        }
    }
}

/**
 * Extension function to convert List<String> to List<JsonElement> for enum values.
 * Used for backward compatibility in constructors.
 */
@JvmName("stringListToJsonElements")
public fun List<String>.toJsonElements(): List<JsonElement> = map { JsonPrimitive(it) }

/**
 * Extension function to convert List<Number> to List<JsonElement> for enum values.
 * Used for backward compatibility in NumericPropertyDefinition constructors.
 */
@JvmName("numberListToJsonElements")
public fun List<Number>.toJsonElements(): List<JsonElement> = map { JsonPrimitive(it) }

/**
 * Extension function to convert List<Boolean> to List<JsonElement> for enum values.
 * Used for backward compatibility in BooleanPropertyDefinition constructors.
 */
@JvmName("booleanListToJsonElements")
public fun List<Boolean>.toJsonElements(): List<JsonElement> = map { JsonPrimitive(it) }
