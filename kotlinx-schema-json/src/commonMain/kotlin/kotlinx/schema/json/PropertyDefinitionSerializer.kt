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

            jsonElement.containsKey($$"$ref") -> {
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
                // If no type is specified, use GenericPropertyDefinition for maximum flexibility
                json.decodeFromJsonElement(
                    GenericPropertyDefinition.serializer(),
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
            null -> {
                null
            }

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

    /**
     * Encodes a [PropertyDefinition] to JSON.
     *
     * Uses a type-safe dispatch mechanism to encode each property definition type.
     * The when expression is exhaustive, ensuring compile-time safety when new
     * PropertyDefinition types are added.
     *
     * Special handling:
     * - [BooleanSchemaDefinition]: Encoded as primitive boolean (true/false)
     * - All other types: Encoded using their respective serializers
     */
    private fun encodePropertyDefinition(
        encoder: JsonEncoder,
        value: PropertyDefinition,
    ) {
        when (value) {
            // Special case: boolean schemas serialize as primitives, not objects
            is BooleanSchemaDefinition -> encodeBooleanSchema(encoder, value)

            // Value-based property definitions
            is StringPropertyDefinition -> encodeTyped(encoder, value)

            is NumericPropertyDefinition -> encodeTyped(encoder, value)

            is BooleanPropertyDefinition -> encodeTyped(encoder, value)

            is ArrayPropertyDefinition -> encodeTyped(encoder, value)

            is ObjectPropertyDefinition -> encodeTyped(encoder, value)

            is GenericPropertyDefinition -> encodeTyped(encoder, value)

            // Composition-based property definitions
            is OneOfPropertyDefinition -> encodeTyped(encoder, value)

            is AnyOfPropertyDefinition -> encodeTyped(encoder, value)

            is AllOfPropertyDefinition -> encodeTyped(encoder, value)

            // Reference property definition
            is ReferencePropertyDefinition -> encodeTyped(encoder, value)
        }
    }

    /**
     * Encodes a boolean schema as a primitive JSON boolean.
     *
     * Boolean schemas (true/false) are special in JSON Schema - they represent
     * schemas that always accept (true) or always reject (false) values.
     */
    private fun encodeBooleanSchema(
        encoder: JsonEncoder,
        value: BooleanSchemaDefinition,
    ) {
        encoder.encodeJsonElement(JsonPrimitive(value.value))
    }

    /**
     * Generic encoding function for typed property definitions.
     *
     * Uses reified type parameters to obtain the correct serializer at compile time,
     * eliminating boilerplate while maintaining type safety.
     *
     * This approach follows the Open/Closed Principle - the encoding logic is
     * centralized, but each PropertyDefinition type provides its own serializer.
     */
    private inline fun <reified T : PropertyDefinition> encodeTyped(
        encoder: JsonEncoder,
        value: T,
    ) {
        encoder.encodeSerializableValue(kotlinx.serialization.serializer<T>(), value)
    }
}
