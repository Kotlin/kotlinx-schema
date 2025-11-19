package kotlinx.schema.json

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonObject

/**
 * Serializer for [PropertyDefinition] that handles polymorphic serialization.
 *
 * @author Konstantin Pavlov
 */
public class PropertyDefinitionSerializer : KSerializer<PropertyDefinition> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("PropertyDefinition")

    @Suppress("LongMethod", "CyclomaticComplexMethod")
    override fun deserialize(decoder: Decoder): PropertyDefinition {
        require(decoder is JsonDecoder) { "This serializer can only be used with JSON" }

        val jsonElement = decoder.decodeJsonElement()
        require(jsonElement is JsonObject) { "Expected JSON object for PropertyDefinition" }

        val json = decoder.json

        // Check if it's a reference
        if (jsonElement.containsKey($$"$ref")) {
            return json.decodeFromJsonElement(
                ReferencePropertyDefinition.serializer(),
                jsonElement,
            )
        }

        // Determine the type
        val types =
            when (val typeElement = jsonElement["type"]) {
                null -> null
                is JsonObject -> listOf(typeElement.toString())
                else -> {
                    val typeSerializer = StringOrListSerializer()
                    json.decodeFromJsonElement(typeSerializer, typeElement)
                }
            }

        return when {
            // If it has items, it's an array
            jsonElement.containsKey("items") -> {
                json.decodeFromJsonElement(
                    ArrayPropertyDefinition.serializer(),
                    jsonElement,
                )
            }
            // If it has properties, it's an object
            jsonElement.containsKey("properties") -> {
                json.decodeFromJsonElement(
                    ObjectPropertyDefinition.serializer(),
                    jsonElement,
                )
            }
            // Check type-specific properties
            types != null -> {
                when {
                    types.contains("string") -> {
                        json.decodeFromJsonElement(
                            StringPropertyDefinition.serializer(),
                            jsonElement,
                        )
                    }

                    types.contains("integer") || types.contains("number") -> {
                        json.decodeFromJsonElement(
                            NumericPropertyDefinition.serializer(),
                            jsonElement,
                        )
                    }

                    types.contains("boolean") -> {
                        json.decodeFromJsonElement(
                            BooleanPropertyDefinition.serializer(),
                            jsonElement,
                        )
                    }

                    types.contains("array") -> {
                        json.decodeFromJsonElement(
                            ArrayPropertyDefinition.serializer(),
                            jsonElement,
                        )
                    }

                    types.contains("object") -> {
                        json.decodeFromJsonElement(
                            ObjectPropertyDefinition.serializer(),
                            jsonElement,
                        )
                    }

                    else -> {
                        // Default to string for unknown types
                        json.decodeFromJsonElement(
                            StringPropertyDefinition.serializer(),
                            jsonElement,
                        )
                    }
                }
            }

            else -> {
                // If no type is specified, default to string
                json.decodeFromJsonElement(
                    StringPropertyDefinition.serializer(),
                    jsonElement,
                )
            }
        }
    }

    override fun serialize(
        encoder: Encoder,
        value: PropertyDefinition,
    ) {
        val jsonEncoder =
            encoder as? JsonEncoder
                ?: throw IllegalArgumentException("This serializer can only be used with JSON")

        when (value) {
            is StringPropertyDefinition ->
                jsonEncoder.encodeSerializableValue(
                    StringPropertyDefinition.serializer(),
                    value,
                )

            is NumericPropertyDefinition ->
                jsonEncoder.encodeSerializableValue(
                    NumericPropertyDefinition.serializer(),
                    value,
                )

            is ArrayPropertyDefinition ->
                jsonEncoder.encodeSerializableValue(
                    ArrayPropertyDefinition.serializer(),
                    value,
                )

            is ObjectPropertyDefinition ->
                jsonEncoder.encodeSerializableValue(
                    ObjectPropertyDefinition.serializer(),
                    value,
                )

            is ReferencePropertyDefinition ->
                jsonEncoder.encodeSerializableValue(
                    ReferencePropertyDefinition.serializer(),
                    value,
                )

            is BooleanPropertyDefinition ->
                jsonEncoder.encodeSerializableValue(
                    BooleanPropertyDefinition.serializer(),
                    value,
                )
        }
    }
}
