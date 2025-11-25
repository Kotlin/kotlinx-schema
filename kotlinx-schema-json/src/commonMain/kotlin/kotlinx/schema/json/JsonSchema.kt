@file:OptIn(ExperimentalSerializationApi::class)

package kotlinx.schema.json

import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonObject

/**
 * Represents a JSON Schema definition
 *
 * @property name The name of the schema.
 * @property strict Whether to enable strict schema adherence.
 * @property schema The actual JSON schema definition.
 *
 * @author Konstantin Pavlov
 */
@Serializable
public data class JsonSchema(
    val name: String,
    @EncodeDefault val strict: Boolean = false,
    val description: String? = null,
    val schema: JsonSchemaDefinition,
)

/**
 * Encodes the given [JsonSchema] instance into a [JsonObject] representation.
 *
 * @param json The [Json] instance to use for serialization. Defaults to [Json] instance with default configuration.
 * @return A [JsonObject] representing the serialized form of the [JsonSchema].
 */
public fun JsonSchema.encodeToJsonObject(json: Json = Json): JsonObject = json.encodeToJsonElement(this).jsonObject

/**
 * Encodes the [JsonSchema] instance into its JSON string representation.
 *
 * @param json The [Json] instance to use for serialization. Defaults to [Json] instance with default configuration.
 * @return The JSON string representation of the [JsonSchema] instance.
 */
public fun JsonSchema.encodeToString(json: Json = Json): String = json.encodeToString(this)

/**
 * Represents a JSON Schema definition.
 *
 * @property id JSON Schema [$id](https://json-schema.org/draft/2020-12/draft-bhutton-json-schema-01#name-the-id-keyword)
 * @property schema JSON Schema [$schema](https://json-schema.org/draft/2020-12/draft-bhutton-json-schema-01#name-the-schema-keyword)
 * keyword, e.g. `https://json-schema.org/draft/2020-12/schema`
 * @property type The JSON schema type (e.g., "object", "array", "string", etc.).
 * @property properties A map of property definitions.
 * @property required List of required property names.
 * @property additionalProperties Whether to allow additional properties in the object.
 * @property description Optional description of the schema.
 *
 * @author Konstantin Pavlov
 */
@Serializable
@Suppress("LongParameterList")
public data class JsonSchemaDefinition(
    @SerialName($$"$id") public val id: String? = null,
    @SerialName($$"$schema") public val schema: String? = null,
    @EncodeDefault
    public val type: String = "object",
    public val properties: Map<String, PropertyDefinition> = emptyMap(),
    public val required: List<String> = emptyList(),
    /**
     * Defines whether additional properties are allowed and their schema.
     * Can be:
     * - `null`: not specified (defaults to true in JSON Schema)
     * - `JsonPrimitive(true)`: allow any additional properties
     * - `JsonPrimitive(false)`: disallow additional properties
     * - `JsonObject`: a schema defining the type of additional properties (e.g., for Maps)
     */
    public val additionalProperties: JsonElement? = null,
    public val description: String? = null,
    public val items: PropertyDefinition? = null,
)

/**
 * Represents a property definition in a JSON Schema.
 *
 * This is a sealed interface that serves as the base for all property definition types.
 * Different property types (string, number, array, object, reference) have different implementations.
 *
 * @see <a href="https://json-schema.org/draft/2020-12/draft-bhutton-json-schema-validation-00">
 *     JSON Schema Validation: A Vocabulary for Structural Validation of JSON
 *     </a>
 */
@Serializable(with = PropertyDefinitionSerializer::class)
public sealed interface PropertyDefinition

/**
 * Represents a value-based property definition in a JSON Schema.
 *
 * This is a sealed interface that extends from [PropertyDefinition] and serves as the base
 * for properties that define specific types, such as strings, numbers, arrays, objects, and booleans.
 * Each implementation of this interface allows defining additional type-specific constraints and attributes.
 */
public sealed interface ValuePropertyDefinition : PropertyDefinition {
    /**
     * The data type of the property.
     */
    public val type: List<String>

    /**
     * Optional description of the property.
     */
    public val description: String?

    /**
     * Whether the property can be null.
     */
    public val nullable: Boolean?
}

/**
 * Represents a string property.
 */
@Serializable
public data class StringPropertyDefinition(
    @Serializable(with = StringOrListSerializer::class) @EncodeDefault override val type: List<String> =
        listOf("string"),
    override val description: String? = null,
    override val nullable: Boolean? = null,
    val format: String? = null,
    val enum: List<String>? = null,
    val minLength: Int? = null,
    val maxLength: Int? = null,
    val pattern: String? = null,
    val default: JsonElement? = null,
    @SerialName("const") val constValue: JsonElement? = null,
) : ValuePropertyDefinition

/**
 * Represents a numeric property (integer or number).
 */
@Serializable
public data class NumericPropertyDefinition(
    @Serializable(with = StringOrListSerializer::class) override val type: List<String>,
    override val description: String? = null,
    override val nullable: Boolean? = null,
    val multipleOf: Double? = null,
    val minimum: Double? = null,
    val exclusiveMinimum: Double? = null,
    val maximum: Double? = null,
    val exclusiveMaximum: Double? = null,
    val default: JsonElement? = null,
    @SerialName("const") val constValue: JsonElement? = null,
) : ValuePropertyDefinition

/**
 * Represents an array property
 */
@Serializable
public data class ArrayPropertyDefinition(
    @Serializable(with = StringOrListSerializer::class) @EncodeDefault override val type: List<String> =
        listOf("array"),
    override val description: String? = null,
    override val nullable: Boolean? = null,
    val items: PropertyDefinition? = null,
    val minItems: UInt? = null,
    val maxItems: UInt? = null,
    val default: JsonElement? = null,
) : ValuePropertyDefinition

/**
 * Represents an object property
 */
@Serializable
public data class ObjectPropertyDefinition(
    @Serializable(with = StringOrListSerializer::class) @EncodeDefault override val type: List<String> =
        listOf("object"),
    override val description: String? = null,
    override val nullable: Boolean? = null,
    val properties: Map<String, PropertyDefinition>? = null,
    val required: List<String>? = null,
    /**
     * Defines whether additional properties are allowed and their schema.
     * Can be:
     * - `null`: not specified (defaults to true in JSON Schema)
     * - `JsonPrimitive(true)`: allow any additional properties
     * - `JsonPrimitive(false)`: disallow additional properties
     * - `JsonObject`: a schema defining the type of additional properties (e.g., for Maps)
     */
    @SerialName("additionalProperties") val additionalProperties: JsonElement? = null,
    val default: JsonElement? = null,
) : ValuePropertyDefinition

/**
 * Represents a boolean property
 */
@Serializable
public data class BooleanPropertyDefinition(
    @Serializable(with = StringOrListSerializer::class) @EncodeDefault override val type: List<String> =
        listOf("boolean"),
    override val description: String? = null,
    override val nullable: Boolean? = null,
    val default: JsonElement? = null,
    @SerialName("const") val constValue: JsonElement? = null,
) : ValuePropertyDefinition

/**
 * Represents a reference to another element
 */
@Serializable
public data class ReferencePropertyDefinition(
    @SerialName($$"$ref") val ref: String,
) : PropertyDefinition
