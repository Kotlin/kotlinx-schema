package kotlinx.schema.json

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.encodeToJsonElement

/**
 * Serializer implementation for the [PropertyDefinition] interface.
 *
 * This serializer handles both serialization and deserialization of various
 * property definition types derived from the [PropertyDefinition] interface.
 * It supports polymorphic and type-safe encoding/decoding mechanisms in
 * accordance with the JSON Schema standard.
 *
 * Main responsibilities:
 * - Deserialize JSON objects into their respective [PropertyDefinition] types.
 * - Encode [PropertyDefinition] types into JSON following type-specific rules.
 * - Handle annotations (unknown keywords) as specified by the JSON Schema standard.
 *
 * Structure:
 * - Deserialization: Determines the correct [PropertyDefinition] type based on the JSON structure and content.
 * - Serialization: Properly encodes each [PropertyDefinition] type while applying type-safe serialization mechanisms.
 * - Annotation Handling: Extracts unknown keywords during deserialization and re-inserts them during serialization.
 *
 * The implementation adheres to JSON Schema 2020-12 specifications and supports
 * handling of true/false schemas, typed schemas, and annotation-based metadata.
 *
 * @property descriptor The associated SerialDescriptor for the [PropertyDefinition] type.
 *                       Utilized for schema generation and serialization metadata.
 */
@Suppress("TooManyFunctions")
internal class PropertyDefinitionSerializer : KSerializer<PropertyDefinition> {
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

        // Extract unknown keywords as annotations
        val annotations = extractAnnotations(jsonElement)

        // Create a cleaned JSON object with only known keywords
        // This ensures deserialization works even with ignoreUnknownKeys = false
        val cleanedJson =
            if (annotations != null) {
                buildJsonObject {
                    jsonElement.forEach { (key, value) ->
                        if (key !in annotations) {
                            put(key, value)
                        }
                    }
                }
            } else {
                jsonElement
            }

        // Deserialize the property definition from the cleaned JSON
        val propertyDef =
            decodePolymorphicOrNull(decoder, cleanedJson)
                ?: decodeTypedProperty(decoder, cleanedJson)

        // If there are annotations, inject them using the copy method
        return if (annotations != null) {
            injectAnnotations(propertyDef, annotations)
        } else {
            propertyDef
        }
    }

    /**
     * Extracts unknown keywords from a JSON object as annotations.
     *
     * Per JSON Schema spec, unknown keywords should be treated as annotations.
     * This function identifies keywords that are not in the known JSON Schema 2020-12
     * vocabulary and returns them as a map.
     *
     * @param jsonObject The JSON object to extract annotations from
     * @return Map of unknown keywords to their values, or null if no unknown keywords present
     */
    private fun extractAnnotations(jsonObject: JsonObject): Map<String, JsonElement>? {
        val unknownKeywords =
            jsonObject.keys.filterNot { key ->
                key in JsonSchemaConstants.KNOWN_KEYWORDS || key == "annotations"
            }

        return if (unknownKeywords.isEmpty()) {
            null
        } else {
            unknownKeywords.associateWith { jsonObject.getValue(it) }
        }
    }

    /**
     * Injects annotations into a PropertyDefinition by using the copy method.
     *
     * Since each PropertyDefinition type is a data class, we can use its copy method
     * to create a new instance with the annotations field populated.
     *
     * @param propertyDef The property definition to inject annotations into
     * @param annotations The annotations to inject
     * @return A new PropertyDefinition instance with annotations populated
     */
    private fun injectAnnotations(
        propertyDef: PropertyDefinition,
        annotations: Map<String, JsonElement>,
    ): PropertyDefinition =
        when (propertyDef) {
            is BooleanSchemaDefinition -> propertyDef.copy(annotations = annotations)
            is StringPropertyDefinition -> propertyDef.copy(annotations = annotations)
            is NumericPropertyDefinition -> propertyDef.copy(annotations = annotations)
            is BooleanPropertyDefinition -> propertyDef.copy(annotations = annotations)
            is ArrayPropertyDefinition -> propertyDef.copy(annotations = annotations)
            is ObjectPropertyDefinition -> propertyDef.copy(annotations = annotations)
            is GenericPropertyDefinition -> propertyDef.copy(annotations = annotations)
            is OneOfPropertyDefinition -> propertyDef.copy(annotations = annotations)
            is AnyOfPropertyDefinition -> propertyDef.copy(annotations = annotations)
            is AllOfPropertyDefinition -> propertyDef.copy(annotations = annotations)
            is ReferencePropertyDefinition -> propertyDef.copy(annotations = annotations)
            is JsonSchema -> propertyDef.copy(annotations = annotations)
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

            jsonElement.containsKey($$"$ref") || jsonElement.containsKey($$"$dynamicRef") -> {
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
            // If it has items or prefixItems, it's an array
            jsonElement.containsKey("items") || jsonElement.containsKey("prefixItems") -> {
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

            is JsonSchema -> encodeTyped(encoder, value)
        }
    }

    /**
     * Encodes a boolean schema as a primitive JSON boolean.
     *
     * Boolean schemas (true/false) are special in JSON Schema - they represent
     * schemas that always accept (true) or always reject (false) values.
     *
     * Note: Boolean schemas with annotations cannot be serialized as primitives,
     * so they are serialized as objects with a "const" field and annotations.
     * This is a deviation from the pure boolean form but necessary to preserve annotations.
     */
    private fun encodeBooleanSchema(
        encoder: JsonEncoder,
        value: BooleanSchemaDefinition,
    ) {
        // If there are annotations, we cannot use primitive form
        // Serialize as an object with the boolean as a special marker
        val annotations = value.annotations
        if (annotations != null) {
            // Boolean schemas with annotations need to be represented as objects
            // We'll use a special encoding: { "const": <boolean>, ...annotations }
            val obj =
                buildJsonObject {
                    put("const", JsonPrimitive(value.value))
                    annotations.forEach { (key, jsonValue) ->
                        put(key, jsonValue)
                    }
                }
            encoder.encodeJsonElement(obj)
        } else {
            encoder.encodeJsonElement(JsonPrimitive(value.value))
        }
    }

    /**
     * Generic encoding function for typed property definitions.
     *
     * Uses reified type parameters to obtain the correct serializer at compile time,
     * eliminating boilerplate while maintaining type safety.
     *
     * This approach follows the Open/Closed Principle - the encoding logic is
     * centralized, but each PropertyDefinition type provides its own serializer.
     *
     * If the property definition has annotations, they are merged into the root level
     * of the JSON object (flat serialization), and the "annotations" field itself is removed.
     */
    private inline fun <reified T : PropertyDefinition> encodeTyped(
        encoder: JsonEncoder,
        value: T,
    ) {
        // First encode using the standard serializer
        val baseElement = encoder.json.encodeToJsonElement(kotlinx.serialization.serializer<T>(), value)

        // If there are annotations, merge them into the root level
        val annotations = value.annotations
        if (annotations != null && baseElement is JsonObject) {
            val merged =
                buildJsonObject {
                    // Add all fields except "annotations"
                    baseElement.forEach { (key, jsonValue) ->
                        if (key != "annotations") {
                            put(key, jsonValue)
                        }
                    }
                    // Add annotation keywords at root level
                    annotations.forEach { (key, jsonValue) ->
                        put(key, jsonValue)
                    }
                }
            encoder.encodeJsonElement(merged)
        } else {
            encoder.encodeJsonElement(baseElement)
        }
    }
}
