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
 * Interface representing a container for properties in a JSON schema.
 */
public interface PropertiesContainer {
    /**
     * Represents a map of property definitions for an object.
     *
     * Each entry in the map consists of a key-value pair, where the key is the name of the property
     * (as a [String]) and the value is an instance of [PropertyDefinition], which describes the details
     * of the property based on the JSON Schema.
     *
     * This property may be null, indicating that no properties are defined for the object or the properties
     * are unavailable.
     */
    public val properties: Map<String, PropertyDefinition>?

    /**
     * Retrieves the boolean property definition associated with the specified property name.
     *
     * @param name The name of the property to fetch from the object definition.
     * @return The corresponding [BooleanPropertyDefinition] if the property exists and is a boolean,
     * or `null` if no such property exists or the property is not boolean property.
     */
    public fun booleanProperty(name: String): BooleanPropertyDefinition? =
        properties?.get(name) as? BooleanPropertyDefinition

    /**
     * Retrieves the numeric property definition associated with the specified property name.
     *
     * @param name The name of the property to fetch from the object definition.
     * @return The corresponding [NumericPropertyDefinition] if the property exists and is numeric,
     * or `null` if no such property exists or the property is not numeric.
     */
    public fun numericProperty(name: String): NumericPropertyDefinition? =
        properties?.get(name) as? NumericPropertyDefinition

    /**
     * Retrieves the string property definition associated with the specified property name.
     *
     * @param name The name of the property to fetch from the object definition.
     * @return The corresponding [StringPropertyDefinition] if the property exists and is a string,
     * or `null` if no such property exists or the property is not a string property.
     */
    public fun stringProperty(name: String): StringPropertyDefinition? =
        properties?.get(name) as? StringPropertyDefinition

    /**
     * Retrieves the `object` property definition associated with the specified property name.
     *
     * @param name The name of the property to fetch from the object definition.
     * @return The corresponding [ObjectPropertyDefinition] if the property exists and is an object,
     * or `null` if no such property exists or the property is not object property.
     */
    public fun objectProperty(name: String): ObjectPropertyDefinition? =
        properties?.get(name) as? ObjectPropertyDefinition

    /**
     * Retrieves the array property definition associated with the specified property name.
     *
     * @param name The name of the property to fetch from the object definition.
     * @return The corresponding [ArrayPropertyDefinition] if the property exists and is an array,
     * or `null` if no such property exists or the property is not an array.
     */
    public fun arrayProperty(name: String): ArrayPropertyDefinition? = properties?.get(name) as? ArrayPropertyDefinition

    /**
     * Retrieves the reference property definition associated with the specified property name.
     *
     * @param name The name of the property to fetch from the object definition.
     * @return The corresponding [ReferencePropertyDefinition] if the property exists and is a reference,
     * or `null` if no such property exists or the property is not a reference.
     */
    public fun referenceProperty(name: String): ReferencePropertyDefinition? =
        properties?.get(name) as? ReferencePropertyDefinition

    /**
     * Retrieves the oneOf property definition associated with the specified property name.
     *
     * @param name The name of the property to fetch from the object definition.
     * @return The corresponding [OneOfPropertyDefinition] if the property exists and is a oneOf,
     * or `null` if no such property exists or the property is not a oneOf.
     */
    public fun oneOfProperty(name: String): OneOfPropertyDefinition? = properties?.get(name) as? OneOfPropertyDefinition

    /**
     * Retrieves the anyOf property definition associated with the specified property name.
     *
     * @param name The name of the property to fetch from the object definition.
     * @return The corresponding [AnyOfPropertyDefinition] if the property exists and is an anyOf,
     * or `null` if no such property exists or the property is not an anyOf.
     */
    public fun anyOfProperty(name: String): AnyOfPropertyDefinition? = properties?.get(name) as? AnyOfPropertyDefinition

    /**
     * Retrieves the allOf property definition associated with the specified property name.
     *
     * @param name The name of the property to fetch from the object definition.
     * @return The corresponding [AllOfPropertyDefinition] if the property exists and is an allOf,
     * or `null` if no such property exists or the property is not an allOf.
     */
    public fun allOfProperty(name: String): AllOfPropertyDefinition? = properties?.get(name) as? AllOfPropertyDefinition

    /**
     * Retrieves the boolean schema definition associated with the specified property name.
     *
     * Note: This is different from [booleanProperty] which returns [BooleanPropertyDefinition]
     * (a schema for boolean values). This method returns [BooleanSchemaDefinition] which
     * represents a boolean schema (true/false as the entire schema).
     *
     * @param name The name of the property to fetch from the object definition.
     * @return The corresponding [BooleanSchemaDefinition] if the property exists and is a boolean schema,
     * or `null` if no such property exists or the property is not a boolean schema.
     */
    public fun booleanSchemaProperty(name: String): BooleanSchemaDefinition? =
        properties?.get(name) as? BooleanSchemaDefinition
}

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
 * @property oneOf List of alternative schemas for polymorphic types.
 * @property discriminator Optional discriminator for polymorphic schemas.
 * @property defs Schema definitions for reusable types (JSON Schema $defs).
 *
 * @author Konstantin Pavlov
 */
@Serializable
@Suppress("LongParameterList")
public data class JsonSchemaDefinition(
    @SerialName($$"$id") public val id: String? = null,
    @SerialName($$"$schema") public val schema: String? = null,
    @Serializable(with = StringOrListSerializer::class) @EncodeDefault
    public val type: List<String> = listOf("object"),
    public override val properties: Map<String, PropertyDefinition> = emptyMap(),
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
    public val oneOf: List<PropertyDefinition>? = null,
    public val discriminator: Discriminator? = null,
    @SerialName($$"$defs") public val defs: Map<String, PropertyDefinition>? = null,
) : PropertiesContainer

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
 * Represents a boolean schema in JSON Schema.
 *
 * Boolean schemas provide simple validation semantics:
 * - `true` schema: accepts/allows any value (always valid)
 * - `false` schema: rejects/disallows any value (always invalid)
 *
 * These are commonly used in:
 * - `items` to allow/disallow additional array items
 * - Polymorphic compositions (oneOf/anyOf/allOf)
 *
 * @property value true for "always valid", false for "always invalid"
 */
@Serializable
public data class BooleanSchemaDefinition(
    val value: Boolean,
) : PropertyDefinition

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
    @Serializable(with = PolymorphicEnumSerializer::class)
    val enum: List<JsonElement>? = null,
    val minLength: Int? = null,
    val maxLength: Int? = null,
    val pattern: String? = null,
    val default: JsonElement? = null,
    @SerialName("const") val constValue: JsonElement? = null,
) : ValuePropertyDefinition

/**
 * Creates StringPropertyDefinition with enum from strings.
 * Convenience function for backward compatibility.
 * Automatically converts List<String> to List<JsonElement>.
 *
 * Note: This overload only applies when enum parameter is explicitly provided with List<String>.
 * For calls without enum parameter, use the primary constructor.
 */
@Suppress("LongParameterList")
public fun StringPropertyDefinition(
    type: List<String> = listOf("string"),
    description: String? = null,
    nullable: Boolean? = null,
    format: String? = null,
    enum: List<String>?, // No default - required when this overload is used
    minLength: Int? = null,
    maxLength: Int? = null,
    pattern: String? = null,
    default: JsonElement? = null,
    constValue: JsonElement? = null,
): StringPropertyDefinition =
    StringPropertyDefinition(
        type = type,
        description = description,
        nullable = nullable,
        format = format,
        enum = enum?.toJsonElements(),
        minLength = minLength,
        maxLength = maxLength,
        pattern = pattern,
        default = default,
        constValue = constValue,
    )

/**
 * Represents a numeric property (integer or number).
 */
@Serializable
public data class NumericPropertyDefinition(
    @Serializable(with = StringOrListSerializer::class) override val type: List<String>,
    override val description: String? = null,
    override val nullable: Boolean? = null,
    @Serializable(with = PolymorphicEnumSerializer::class)
    val enum: List<JsonElement>? = null,
    val multipleOf: Double? = null,
    val minimum: Double? = null,
    val exclusiveMinimum: Double? = null,
    val maximum: Double? = null,
    val exclusiveMaximum: Double? = null,
    val default: JsonElement? = null,
    @SerialName("const") val constValue: JsonElement? = null,
) : ValuePropertyDefinition

/**
 * Creates NumericPropertyDefinition with enum from numbers.
 * Convenience function for backward compatibility.
 * Automatically converts List<Number> to List<JsonElement>.
 */
@Suppress("LongParameterList")
public fun NumericPropertyDefinition(
    type: List<String>,
    description: String? = null,
    nullable: Boolean? = null,
    enum: List<Number>?, // No default - required when this overload is used
    multipleOf: Double? = null,
    minimum: Double? = null,
    exclusiveMinimum: Double? = null,
    maximum: Double? = null,
    exclusiveMaximum: Double? = null,
    default: JsonElement? = null,
    constValue: JsonElement? = null,
): NumericPropertyDefinition =
    NumericPropertyDefinition(
        type = type,
        description = description,
        nullable = nullable,
        enum = enum?.toJsonElements(),
        multipleOf = multipleOf,
        minimum = minimum,
        exclusiveMinimum = exclusiveMinimum,
        maximum = maximum,
        exclusiveMaximum = exclusiveMaximum,
        default = default,
        constValue = constValue,
    )

/**
 * Represents an array property
 */
@Serializable
public data class ArrayPropertyDefinition(
    @Serializable(with = StringOrListSerializer::class) @EncodeDefault override val type: List<String> =
        listOf("array"),
    override val description: String? = null,
    override val nullable: Boolean? = null,
    @Serializable(with = PolymorphicEnumSerializer::class)
    val enum: List<JsonElement>? = null,
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
    @Serializable(with = PolymorphicEnumSerializer::class)
    val enum: List<JsonElement>? = null,
    override val properties: Map<String, PropertyDefinition>? = null,
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
) : ValuePropertyDefinition,
    PropertiesContainer

/**
 * Represents a boolean property
 */
@Serializable
public data class BooleanPropertyDefinition(
    @Serializable(with = StringOrListSerializer::class) @EncodeDefault override val type: List<String> =
        listOf("boolean"),
    override val description: String? = null,
    override val nullable: Boolean? = null,
    @Serializable(with = PolymorphicEnumSerializer::class)
    val enum: List<JsonElement>? = null,
    val default: JsonElement? = null,
    @SerialName("const") val constValue: JsonElement? = null,
) : ValuePropertyDefinition

/**
 * Creates BooleanPropertyDefinition with enum from booleans.
 * Convenience function for backward compatibility.
 * Automatically converts List<Boolean> to List<JsonElement>.
 */
@Suppress("LongParameterList")
public fun BooleanPropertyDefinition(
    type: List<String> = listOf("boolean"),
    description: String? = null,
    nullable: Boolean? = null,
    enum: List<Boolean>?, // No default - required when this overload is used
    default: JsonElement? = null,
    constValue: JsonElement? = null,
): BooleanPropertyDefinition =
    BooleanPropertyDefinition(
        type = type,
        description = description,
        nullable = nullable,
        enum = enum?.toJsonElements(),
        default = default,
        constValue = constValue,
    )

/**
 * Represents a reference to another element
 */
@Serializable
public data class ReferencePropertyDefinition(
    @SerialName($$"$ref") val ref: String,
) : PropertyDefinition

/**
 * Represents a discriminator for polymorphic schemas.
 *
 * The discriminator is used with oneOf to efficiently determine which schema applies
 * based on a property value in the instance data.
 *
 * @property propertyName The name of the property that holds the discriminator value
 * @property mapping Optional explicit mapping from discriminator values to schema references.
 *                   If null, implicit mapping is used (discriminator value matches schema name)
 *
 * @see <a href="https://spec.openapis.org/oas/v3.1.0#discriminator-object">
 *     OpenAPI Discriminator Object</a>
 */
@Serializable
public data class Discriminator(
    val propertyName: String,
    val mapping: Map<String, String>? = null,
)

/**
 * Represents a oneOf schema composition.
 *
 * Validates that the instance matches exactly one of the provided schemas.
 * Commonly used for polymorphic types with mutually exclusive alternatives.
 *
 * @property oneOf List of property definitions representing the alternatives.
 *                 Must contain at least 2 options.
 * @property discriminator Optional discriminator to efficiently determine which schema applies
 * @property description Optional description of the property
 *
 * @see <a href="https://json-schema.org/understanding-json-schema/reference/combining.html#oneOf">
 *     JSON Schema oneOf</a>
 */
@Serializable
public data class OneOfPropertyDefinition(
    val oneOf: List<PropertyDefinition>,
    val discriminator: Discriminator? = null,
    val description: String? = null,
) : PropertyDefinition

/**
 * Represents an anyOf schema composition.
 *
 * Validates that the instance matches one or more of the provided schemas.
 * Unlike oneOf, the instance can match multiple schemas simultaneously.
 *
 * @property anyOf List of property definitions representing the alternatives.
 *                 Must contain at least 2 options.
 * @property description Optional description of the property
 *
 * @see <a href="https://json-schema.org/understanding-json-schema/reference/combining.html#anyOf">
 *     JSON Schema anyOf</a>
 */
@Serializable
public data class AnyOfPropertyDefinition(
    val anyOf: List<PropertyDefinition>,
    val description: String? = null,
) : PropertyDefinition

/**
 * Represents an allOf schema composition.
 *
 * Validates that the instance matches all of the provided schemas.
 * Commonly used for schema composition and inheritance patterns.
 *
 * @property allOf List of property definitions that must all be satisfied.
 *                 Must contain at least 1 schema.
 * @property description Optional description of the property
 *
 * @see <a href="https://json-schema.org/understanding-json-schema/reference/combining.html#allOf">
 *     JSON Schema allOf</a>
 */
@Serializable
public data class AllOfPropertyDefinition(
    val allOf: List<PropertyDefinition>,
    val description: String? = null,
) : PropertyDefinition
