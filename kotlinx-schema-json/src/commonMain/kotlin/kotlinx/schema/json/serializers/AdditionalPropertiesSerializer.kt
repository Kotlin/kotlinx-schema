@file:OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)

package kotlinx.schema.json.serializers

import kotlinx.schema.json.AdditionalPropertiesConstraint
import kotlinx.schema.json.AdditionalPropertiesSchema
import kotlinx.schema.json.AllowAdditionalProperties
import kotlinx.schema.json.DenyAdditionalProperties
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.jsonPrimitive

/**
 * Custom serializer for [kotlinx.schema.json.AdditionalPropertiesConstraint].
 *
 * Handles the three forms of additionalProperties in JSON Schema:
 * - Boolean `true` → [kotlinx.schema.json.AllowAdditionalProperties]
 * - Boolean `false` → [kotlinx.schema.json.DenyAdditionalProperties]
 * - Schema object → [kotlinx.schema.json.AdditionalPropertiesSchema]
 */

internal object AdditionalPropertiesSerializer : KSerializer<AdditionalPropertiesConstraint> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("AdditionalPropertiesConstraint", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): AdditionalPropertiesConstraint {
        require(decoder is JsonDecoder) { "AdditionalPropertiesSerializer can only be used with JSON" }

        val element = decoder.decodeJsonElement()

        // Handle boolean values
        if (element is JsonPrimitive && !element.isString) {
            return when (element.booleanOrNull) {
                true -> AllowAdditionalProperties
                false -> DenyAdditionalProperties
                null -> error("Expected boolean or schema for additionalProperties, got: $element")
            }
        }

        // Handle schema object
        val schema = decoder.json.decodeFromJsonElement(PropertyDefinitionSerializer(), element)
        return AdditionalPropertiesSchema(schema)
    }

    override fun serialize(
        encoder: Encoder,
        value: AdditionalPropertiesConstraint,
    ) {
        val jsonEncoder =
            encoder as? JsonEncoder
                ?: error("AdditionalPropertiesSerializer can only be used with JSON")

        when (value) {
            AllowAdditionalProperties -> jsonEncoder.encodeBoolean(true)
            DenyAdditionalProperties -> jsonEncoder.encodeBoolean(false)
            is AdditionalPropertiesSchema -> {
                jsonEncoder.encodeSerializableValue(
                    PropertyDefinitionSerializer(),
                    value.schema,
                )
            }
        }
    }
}

internal object NumberToNullableIntSerializer : KSerializer<Int?> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("FlexibleInt", PrimitiveKind.INT)

    override fun deserialize(decoder: Decoder): Int? {
        val element = (decoder as? JsonDecoder)?.decodeJsonElement()
        return if (element == null || element is JsonNull) {
            null
        } else {
            element.jsonPrimitive.content
                .toDouble()
                .toInt()
        }
    }

    override fun serialize(
        encoder: Encoder,
        value: Int?,
    ) {
        if (value == null) encoder.encodeNull() else encoder.encodeInt(value)
    }
}
