@file:OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)

package kotlinx.schema.json.serializers

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
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
internal class PolymorphicEnumSerializer : KSerializer<List<JsonElement>?> by TypedEnumSerializer(
    descriptorName = "PolymorphicEnum",
    elementConverter = { it }, // Identity conversion - preserve JsonElement as-is
    elementSerializer = { it }, // Identity conversion - preserve JsonElement as-is
)

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
public class ArrayEnumSerializer : KSerializer<List<JsonArray>?> by TypedEnumSerializer(
    descriptorName = "ArrayEnum",
    elementConverter = { element ->
        when (element) {
            is JsonArray -> element
            else -> throw SerializationException(
                "Array enum must contain only array values, got ${element::class.simpleName}",
            )
        }
    },
    elementSerializer = { it }, // JsonArray is already a JsonElement
)

/**
 * Custom serializer for object enum fields that ensures all values are JsonObject during deserialization.
 *
 * During deserialization:
 * - JsonObject values → preserved as-is
 * - Other types → error
 *
 * During serialization, converts List<JsonObject> back to JsonArray.
 */
public class ObjectEnumSerializer : KSerializer<List<JsonObject>?> by TypedEnumSerializer(
    descriptorName = "ObjectEnum",
    elementConverter = { element ->
        when (element) {
            is JsonObject -> element
            else -> throw SerializationException(
                "Object enum must contain only object values, got ${element::class.simpleName}",
            )
        }
    },
    elementSerializer = { it }, // JsonObject is already a JsonElement
)

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
public class StringEnumSerializer : KSerializer<List<String>?> by TypedEnumSerializer(
    descriptorName = "StringEnum",
    elementConverter = { element ->
        when (element) {
            is JsonPrimitive -> element.content
            else -> element.toString()
        }
    },
    elementSerializer = { JsonPrimitive(it) },
)

/**
 * Custom serializer for numeric enum fields that converts JsonElement values to Double during deserialization.
 *
 * During deserialization:
 * - JsonPrimitive numbers → converted to Double
 * - JsonPrimitive strings → parsed to Double if possible, otherwise error
 * - Other types → error
 *
 * During serialization, converts List<Double> back to JsonArray of JsonPrimitive numbers.
 */
public class NumericEnumSerializer : KSerializer<List<Double>?> by TypedEnumSerializer(
    descriptorName = "NumericEnum",
    elementConverter = { element ->
        if (element !is JsonPrimitive) {
            throw SerializationException(
                "Numeric enum must contain only number values, got ${element::class.simpleName}",
            )
        }
        element.content.toDoubleOrNull()
            ?: throw SerializationException(
                "Cannot convert '${element.content}' to number in enum",
            )
    },
    elementSerializer = { JsonPrimitive(it) },
)

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
public class BooleanEnumSerializer : KSerializer<List<Boolean>?> by TypedEnumSerializer(
    descriptorName = "BooleanEnum",
    elementConverter = { element ->
        if (element !is JsonPrimitive) {
            throw SerializationException(
                "Boolean enum must contain only boolean values, got ${element::class.simpleName}",
            )
        }

        // Try direct boolean value
        element.booleanOrNull?.let { return@TypedEnumSerializer it }

        // Try string "true"/"false" or number (non-zero = true)
        when {
            element.isString -> {
                when (element.content.lowercase()) {
                    "true" -> true
                    "false" -> false
                    else -> throw SerializationException(
                        "Cannot convert '${element.content}' to boolean in enum",
                    )
                }
            }

            else -> {
                element.content.toDoubleOrNull()?.let { it != 0.0 }
                    ?: throw SerializationException(
                        "Cannot convert '${element.content}' to boolean in enum",
                    )
            }
        }
    },
    elementSerializer = { JsonPrimitive(it) },
)
