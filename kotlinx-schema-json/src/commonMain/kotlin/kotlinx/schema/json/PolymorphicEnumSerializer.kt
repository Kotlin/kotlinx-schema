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
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlin.jvm.JvmName

/**
 * Custom serializer for enum fields that preserves heterogeneous JSON types.
 *
 * During deserialization, it reads JSON array and returns List<JsonElement>.
 * During serialization, it writes JsonElements as-is in a JSON array.
 *
 * Used for property types that need to support mixed-type enums (e.g., NumericPropertyDefinition,
 * ArrayPropertyDefinition, ObjectPropertyDefinition).
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

/**
 * Custom serializer for array enum fields that ensures all values are JsonArray during deserialization.
 *
 * During deserialization:
 * - JsonArray values → preserved as-is
 * - Other types → error
 *
 * During serialization, converts List<JsonArray> back to JsonArray.
 */
public class ArrayEnumSerializer : KSerializer<List<JsonArray>?> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("ArrayEnum")

    override fun deserialize(decoder: Decoder): List<JsonArray>? {
        val jsonDecoder =
            decoder as? JsonDecoder
                ?: error("ArrayEnumSerializer can only be used with JSON")

        val element = jsonDecoder.decodeJsonElement()

        return when (element) {
            is JsonArray -> {
                element.map { jsonElement ->
                    when (jsonElement) {
                        is JsonArray -> {
                            jsonElement
                        }

                        else -> {
                            throw SerializationException(
                                "Array enum must contain only array values, got ${jsonElement::class.simpleName}",
                            )
                        }
                    }
                }
            }

            else -> {
                throw SerializationException("Expected JsonArray for enum, got ${element::class.simpleName}")
            }
        }
    }

    override fun serialize(
        encoder: Encoder,
        value: List<JsonArray>?,
    ) {
        val jsonEncoder =
            encoder as? JsonEncoder
                ?: error("ArrayEnumSerializer can only be used with JSON")

        if (value == null) {
            encoder.encodeNull()
        } else {
            jsonEncoder.encodeJsonElement(JsonArray(value))
        }
    }
}

/**
 * Custom serializer for object enum fields that ensures all values are JsonObject during deserialization.
 *
 * During deserialization:
 * - JsonObject values → preserved as-is
 * - Other types → error
 *
 * During serialization, converts List<JsonObject> back to JsonArray.
 */
public class ObjectEnumSerializer : KSerializer<List<JsonObject>?> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("ObjectEnum")

    override fun deserialize(decoder: Decoder): List<JsonObject>? {
        val jsonDecoder =
            decoder as? JsonDecoder
                ?: error("ObjectEnumSerializer can only be used with JSON")

        val element = jsonDecoder.decodeJsonElement()

        return when (element) {
            is JsonArray -> {
                element.map { jsonElement ->
                    when (jsonElement) {
                        is JsonObject -> {
                            jsonElement
                        }

                        else -> {
                            throw SerializationException(
                                "Object enum must contain only object values, got ${jsonElement::class.simpleName}",
                            )
                        }
                    }
                }
            }

            else -> {
                throw SerializationException("Expected JsonArray for enum, got ${element::class.simpleName}")
            }
        }
    }

    override fun serialize(
        encoder: Encoder,
        value: List<JsonObject>?,
    ) {
        val jsonEncoder =
            encoder as? JsonEncoder
                ?: error("ObjectEnumSerializer can only be used with JSON")

        if (value == null) {
            encoder.encodeNull()
        } else {
            jsonEncoder.encodeJsonElement(JsonArray(value))
        }
    }
}

/**
 * Custom serializer for string enum fields that converts JsonElement values to String during deserialization.
 *
 * During deserialization:
 * - JsonPrimitive strings → content directly
 * - JsonPrimitive numbers/booleans → string representation
 * - JsonNull → "null"
 * - JsonObject/JsonArray → JSON string representation
 *
 * During serialization, converts List<String> back to JsonArray of JsonPrimitive strings.
 */
public class StringEnumSerializer : KSerializer<List<String>?> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("StringEnum")

    override fun deserialize(decoder: Decoder): List<String>? {
        val jsonDecoder =
            decoder as? JsonDecoder
                ?: error("StringEnumSerializer can only be used with JSON")

        return when (val element = jsonDecoder.decodeJsonElement()) {
            is JsonArray -> {
                element.map { jsonElement ->
                    when (jsonElement) {
                        is JsonPrimitive -> jsonElement.content
                        else -> jsonElement.toString()
                    }
                }
            }

            else -> {
                throw SerializationException("Expected JsonArray for enum, got ${element::class.simpleName}")
            }
        }
    }

    override fun serialize(
        encoder: Encoder,
        value: List<String>?,
    ) {
        val jsonEncoder =
            encoder as? JsonEncoder
                ?: error("StringEnumSerializer can only be used with JSON")

        if (value == null) {
            encoder.encodeNull()
        } else {
            jsonEncoder.encodeJsonElement(JsonArray(value.map { JsonPrimitive(it) }))
        }
    }
}

/**
 * Custom serializer for numeric enum fields that converts JsonElement values to Double during deserialization.
 *
 * During deserialization:
 * - JsonPrimitive numbers → converted to Double
 * - JsonPrimitive strings → parsed to Double if possible, otherwise error
 * - JsonPrimitive booleans → 1.0 for true, 0.0 for false
 * - Other types → error
 *
 * During serialization, converts List<Double> back to JsonArray of JsonPrimitive numbers.
 */
public class NumericEnumSerializer : KSerializer<List<Double>?> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("NumericEnum")

    private fun convertToDouble(jsonElement: JsonElement): Double {
        if (jsonElement !is JsonPrimitive) {
            throw SerializationException(
                "Numeric enum must contain only number values, got ${jsonElement::class.simpleName}",
            )
        }

        return jsonElement.content.toDoubleOrNull()
            ?: throw SerializationException(
                "Cannot convert '${jsonElement.content}' to number in enum",
            )
    }

    override fun deserialize(decoder: Decoder): List<Double>? {
        val jsonDecoder =
            decoder as? JsonDecoder
                ?: error("NumericEnumSerializer can only be used with JSON")

        return when (val element = jsonDecoder.decodeJsonElement()) {
            is JsonArray -> element.map(::convertToDouble)
            else -> throw SerializationException("Expected JsonArray for enum, got ${element::class.simpleName}")
        }
    }

    override fun serialize(
        encoder: Encoder,
        value: List<Double>?,
    ) {
        val jsonEncoder =
            encoder as? JsonEncoder
                ?: error("NumericEnumSerializer can only be used with JSON")

        if (value == null) {
            encoder.encodeNull()
        } else {
            jsonEncoder.encodeJsonElement(JsonArray(value.map { JsonPrimitive(it) }))
        }
    }
}

/**
 * Custom serializer for boolean enum fields that converts JsonElement values to Boolean during deserialization.
 *
 * During deserialization:
 * - JsonPrimitive booleans → value directly
 * - JsonPrimitive strings → parsed to Boolean ("true"/"false")
 * - JsonPrimitive numbers → non-zero = true, zero = false
 * - Other types → error
 *
 * During serialization, converts List<Boolean> back to JsonArray of JsonPrimitive booleans.
 */
public class BooleanEnumSerializer : KSerializer<List<Boolean>?> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("BooleanEnum")

    private fun convertToBoolean(jsonElement: JsonElement): Boolean {
        if (jsonElement !is JsonPrimitive) {
            throw SerializationException(
                "Boolean enum must contain only boolean values, got ${jsonElement::class.simpleName}",
            )
        }

        // Try direct boolean value
        jsonElement.booleanOrNull?.let { return it }

        // Try string "true"/"false" or number (non-zero = true)
        val result =
            when {
                jsonElement.isString ->
                    when (jsonElement.content.lowercase()) {
                        "true" -> true
                        "false" -> false
                        else -> null
                    }
                else -> jsonElement.content.toDoubleOrNull()?.let { it != 0.0 }
            }

        return result
            ?: throw SerializationException(
                "Cannot convert '${jsonElement.content}' to boolean in enum",
            )
    }

    override fun deserialize(decoder: Decoder): List<Boolean>? {
        val jsonDecoder =
            decoder as? JsonDecoder
                ?: error("BooleanEnumSerializer can only be used with JSON")

        return when (val element = jsonDecoder.decodeJsonElement()) {
            is JsonArray -> element.map(::convertToBoolean)
            else -> throw SerializationException("Expected JsonArray for enum, got ${element::class.simpleName}")
        }
    }

    override fun serialize(
        encoder: Encoder,
        value: List<Boolean>?,
    ) {
        val jsonEncoder =
            encoder as? JsonEncoder
                ?: error("BooleanEnumSerializer can only be used with JSON")

        if (value == null) {
            encoder.encodeNull()
        } else {
            jsonEncoder.encodeJsonElement(JsonArray(value.map { JsonPrimitive(it) }))
        }
    }
}
