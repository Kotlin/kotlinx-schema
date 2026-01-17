package kotlinx.schema.json

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/**
 * Serializer for [PropertyDefinition] that handles polymorphic serialization.
 *
 * @author Konstantin Pavlov
 */
public class PropertyDefinitionSerializer : KSerializer<PropertyDefinition> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("PropertyDefinition")

    override fun deserialize(decoder: Decoder): PropertyDefinition {
        require(decoder is JsonDecoder) { "This serializer can only be used with JSON" }

        val jsonElement = decoder.decodeJsonElement()

        // Handle boolean schemas (true/false)
        if (jsonElement is JsonPrimitive) {
            val content = jsonElement.content
            if (content == "true" || content == "false") {
                return BooleanSchemaDefinition(value = content.toBoolean())
            }
        }

        require(jsonElement is JsonObject) {
            "Expected JSON object or boolean for PropertyDefinition, got ${jsonElement::class.simpleName}"
        }

        return decodePolymorphicOrNull(decoder, jsonElement)
            ?: decodeTypedProperty(decoder, jsonElement)
    }

    private fun decodePolymorphicOrNull(
        decoder: JsonDecoder,
        jsonElement: JsonObject,
    ): PropertyDefinition? {
        val json = decoder.json
        return when {
            jsonElement.containsKey("oneOf") -> {
                json.decodeFromJsonElement(
                    OneOfPropertyDefinition.serializer(),
                    jsonElement,
                )
            }

            jsonElement.containsKey("anyOf") -> {
                json.decodeFromJsonElement(
                    AnyOfPropertyDefinition.serializer(),
                    jsonElement,
                )
            }

            jsonElement.containsKey("allOf") -> {
                json.decodeFromJsonElement(
                    AllOfPropertyDefinition.serializer(),
                    jsonElement,
                )
            }

            jsonElement.containsKey("\$ref") -> {
                json.decodeFromJsonElement(
                    ReferencePropertyDefinition.serializer(),
                    jsonElement,
                )
            }

            else -> {
                null
            }
        }
    }

    private fun decodeTypedProperty(
        decoder: JsonDecoder,
        jsonElement: JsonObject,
    ): PropertyDefinition {
        val json = decoder.json
        val types = determineTypes(json, jsonElement)

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
                decodeByTypes(json, jsonElement, types)
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

    private fun determineTypes(
        json: Json,
        jsonElement: JsonObject,
    ): List<String>? =
        when (val typeElement = jsonElement["type"]) {
            null -> null
            else -> {
                val typeSerializer = StringOrListSerializer()
                json.decodeFromJsonElement(typeSerializer, typeElement)
            }
        }

    private fun decodeByTypes(
        json: Json,
        jsonElement: JsonObject,
        types: List<String>,
    ): PropertyDefinition =
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

    override fun serialize(
        encoder: Encoder,
        value: PropertyDefinition,
    ) {
        val jsonEncoder =
            encoder as? JsonEncoder
                ?: throw SerializationException("This serializer can only be used with JSON")

        encodePropertyDefinition(jsonEncoder, value)
    }

    private fun encodePropertyDefinition(
        encoder: JsonEncoder,
        value: PropertyDefinition,
    ) {
        when (value) {
            is BooleanSchemaDefinition -> {
                // Serialize as primitive boolean, not object
                encoder.encodeJsonElement(JsonPrimitive(value.value))
            }

            is StringPropertyDefinition -> {
                encoder.encodeSerializableValue(
                    StringPropertyDefinition.serializer(),
                    value,
                )
            }

            is NumericPropertyDefinition -> {
                encoder.encodeSerializableValue(
                    NumericPropertyDefinition.serializer(),
                    value,
                )
            }

            is ArrayPropertyDefinition -> {
                encoder.encodeSerializableValue(
                    ArrayPropertyDefinition.serializer(),
                    value,
                )
            }

            is ObjectPropertyDefinition -> {
                encoder.encodeSerializableValue(
                    ObjectPropertyDefinition.serializer(),
                    value,
                )
            }

            is ReferencePropertyDefinition -> {
                encoder.encodeSerializableValue(
                    ReferencePropertyDefinition.serializer(),
                    value,
                )
            }

            is BooleanPropertyDefinition -> {
                encoder.encodeSerializableValue(
                    BooleanPropertyDefinition.serializer(),
                    value,
                )
            }

            is OneOfPropertyDefinition -> {
                encoder.encodeSerializableValue(
                    OneOfPropertyDefinition.serializer(),
                    value,
                )
            }

            is AnyOfPropertyDefinition -> {
                encoder.encodeSerializableValue(
                    AnyOfPropertyDefinition.serializer(),
                    value,
                )
            }

            is AllOfPropertyDefinition -> {
                encoder.encodeSerializableValue(
                    AllOfPropertyDefinition.serializer(),
                    value,
                )
            }
        }
    }
}
