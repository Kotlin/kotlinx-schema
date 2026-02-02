@file:OptIn(ExperimentalSerializationApi::class)

package kotlinx.schema.json

import kotlinx.schema.json.JsonSchemaConstants.Keys.ADDITIONAL_PROPERTIES
import kotlinx.schema.json.JsonSchemaConstants.Keys.ANCHOR
import kotlinx.schema.json.JsonSchemaConstants.Keys.COMMENT
import kotlinx.schema.json.JsonSchemaConstants.Keys.CONST
import kotlinx.schema.json.JsonSchemaConstants.Keys.DEFS
import kotlinx.schema.json.JsonSchemaConstants.Keys.DYNAMIC_ANCHOR
import kotlinx.schema.json.JsonSchemaConstants.Keys.DYNAMIC_REF
import kotlinx.schema.json.JsonSchemaConstants.Keys.ELSE
import kotlinx.schema.json.JsonSchemaConstants.Keys.ID
import kotlinx.schema.json.JsonSchemaConstants.Keys.IF
import kotlinx.schema.json.JsonSchemaConstants.Keys.REF
import kotlinx.schema.json.JsonSchemaConstants.Keys.SCHEMA
import kotlinx.schema.json.JsonSchemaConstants.Keys.THEN
import kotlinx.schema.json.JsonSchemaConstants.Types.ARRAY_TYPE
import kotlinx.schema.json.JsonSchemaConstants.Types.BOOLEAN_TYPE
import kotlinx.schema.json.JsonSchemaConstants.Types.OBJECT_TYPE
import kotlinx.schema.json.JsonSchemaConstants.Types.STRING_TYPE
import kotlinx.schema.json.serializers.AdditionalPropertiesSerializer
import kotlinx.schema.json.serializers.ArrayEnumSerializer
import kotlinx.schema.json.serializers.BooleanEnumSerializer
import kotlinx.schema.json.serializers.NumberToNullableIntSerializer
import kotlinx.schema.json.serializers.NumericEnumSerializer
import kotlinx.schema.json.serializers.ObjectEnumSerializer
import kotlinx.schema.json.serializers.PolymorphicEnumSerializer
import kotlinx.schema.json.serializers.PropertyDefinitionSerializer
import kotlinx.schema.json.serializers.StringEnumSerializer
import kotlinx.schema.json.serializers.StringOrListSerializer
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonIgnoreUnknownKeys
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Represents the constraint for additional properties in a JSON Schema object.
 *
 * In JSON Schema, `additionalProperties` can be:
 * - `true`: Allow any additional properties ([AllowAdditionalProperties])
 * - `false`: Disallow additional properties ([DenyAdditionalProperties])
 * - A schema: Additional properties must match this schema ([AdditionalPropertiesSchema])
 */
@Serializable(with = AdditionalPropertiesSerializer::class)
public sealed interface AdditionalPropertiesConstraint {
    public companion object {
        /**
         * Creates a constraint that allows any additional properties.
         *
         * Equivalent to `"additionalProperties": true` in JSON Schema.
         *
         * ## Example
         * ```kotlin
         * val schema = JsonSchema(
         *     properties = mapOf("name" to StringPropertyDefinition()),
         *     additionalProperties = AdditionalPropertiesConstraint.allow()
         * )
         * ```
         */
        public fun allow(): AdditionalPropertiesConstraint = AllowAdditionalProperties

        /**
         * Creates a constraint that disallows any additional properties.
         *
         * Equivalent to `"additionalProperties": false` in JSON Schema.
         *
         * ## Example
         * ```kotlin
         * val schema = JsonSchema(
         *     properties = mapOf("name" to StringPropertyDefinition()),
         *     additionalProperties = AdditionalPropertiesConstraint.deny()
         * )
         * ```
         */
        public fun deny(): AdditionalPropertiesConstraint = DenyAdditionalProperties

        /**
         * Creates a constraint that requires additional properties to match the specified schema.
         *
         * Equivalent to `"additionalProperties": { <schema> }` in JSON Schema.
         *
         * ## Example
         * ```kotlin
         * val schema = JsonSchema(
         *     properties = mapOf("name" to StringPropertyDefinition()),
         *     additionalProperties = AdditionalPropertiesConstraint.schema(
         *         NumericPropertyDefinition(minimum = 0.0)
         *     )
         * )
         * ```
         *
         * @param schema The schema that additional properties must match
         */
        public fun schema(schema: PropertyDefinition): AdditionalPropertiesConstraint =
            AdditionalPropertiesSchema(schema)
    }
}

/**
 * Allows any additional properties beyond those explicitly defined.
 * Corresponds to `"additionalProperties": true` in JSON Schema.
 */
@Serializable
public data object AllowAdditionalProperties : AdditionalPropertiesConstraint

/**
 * Disallows any additional properties beyond those explicitly defined.
 * Corresponds to `"additionalProperties": false` in JSON Schema.
 */
@Serializable
public data object DenyAdditionalProperties : AdditionalPropertiesConstraint

/**
 * Additional properties must conform to the specified schema.
 * Corresponds to `"additionalProperties": { <schema> }` in JSON Schema.
 *
 * @property schema The schema that additional properties must match
 */
@Serializable
public data class AdditionalPropertiesSchema(
    val schema: PropertyDefinition,
) : AdditionalPropertiesConstraint

/**
 * Encodes this [JsonSchema] to a [JsonObject].
 */
public fun JsonSchema.encodeToJsonObject(json: Json = Json): JsonObject = json.encodeToJsonElement(this).jsonObject

/**
 * Encodes this [JsonSchema] to a JSON string.
 */
public fun JsonSchema.encodeToString(json: Json = Json): String = json.encodeToString(this)

/**
 * Interface representing a container for properties in a JSON schema.
 */
public interface PropertiesContainer {
    /**
     * Map of property definitions.
     */
    public val properties: Map<String, PropertyDefinition>?

    /**
     * List of required property names.
     */
    public val required: List<String>?

    /**
     * Constraint for additional properties in the object.
     *
     * - `null`: No constraint specified (typically treated as allowing additional properties)
     * - [AllowAdditionalProperties]: Explicitly allow any additional properties
     * - [DenyAdditionalProperties]: Explicitly disallow additional properties
     * - [AdditionalPropertiesSchema]: Additional properties must match a specific schema
     */
    public val additionalProperties: AdditionalPropertiesConstraint?

    /**
     * Map of property definitions for properties matching a regular expression pattern.
     */
    public val patternProperties: Map<String, PropertyDefinition>?

    /**
     * Schema for unevaluated properties.
     */
    public val unevaluatedProperties: PropertyDefinition?

    /**
     * Schema for property names.
     */
    public val propertyNames: PropertyDefinition?

    /**
     * A map of property names to lists of other property names that are required if the given property is present.
     */
    public val dependentRequired: Map<String, List<String>>?

    /**
     * A map of property names to schemas that must be satisfied if the given property is present.
     */
    public val dependentSchemas: Map<String, PropertyDefinition>?

    /**
     * Minimum number of properties allowed in the object.
     */
    public val minProperties: Int?

    /**
     * Maximum number of properties allowed in the object.
     */
    public val maxProperties: Int?

    /**
     * Returns the boolean property definition for [name], or null if not found or not a boolean property.
     */
    public fun booleanProperty(name: String): BooleanPropertyDefinition? =
        properties?.get(name) as? BooleanPropertyDefinition

    /**
     * Returns the numeric property definition for [name], or null if not found or not a numeric property.
     */
    public fun numericProperty(name: String): NumericPropertyDefinition? =
        properties?.get(name) as? NumericPropertyDefinition

    /**
     * Returns the string property definition for [name], or null if not found or not a string property.
     */
    public fun stringProperty(name: String): StringPropertyDefinition? =
        properties?.get(name) as? StringPropertyDefinition

    /**
     * Returns the object property definition for [name], or null if not found or not an object property.
     */
    public fun objectProperty(name: String): ObjectPropertyDefinition? =
        properties?.get(name) as? ObjectPropertyDefinition

    /**
     * Returns the array property definition for [name], or null if not found or not an array property.
     */
    public fun arrayProperty(name: String): ArrayPropertyDefinition? = properties?.get(name) as? ArrayPropertyDefinition

    /**
     * Returns the reference property definition for [name], or null if not found or not a reference.
     */
    public fun referenceProperty(name: String): ReferencePropertyDefinition? =
        properties?.get(name) as? ReferencePropertyDefinition

    /**
     * Returns the oneOf property definition for [name], or null if not found or not a oneOf.
     */
    public fun oneOfProperty(name: String): OneOfPropertyDefinition? = properties?.get(name) as? OneOfPropertyDefinition

    /**
     * Returns the anyOf property definition for [name], or null if not found or not an anyOf.
     */
    public fun anyOfProperty(name: String): AnyOfPropertyDefinition? = properties?.get(name) as? AnyOfPropertyDefinition

    /**
     * Returns the allOf property definition for [name], or null if not found or not an allOf.
     */
    public fun allOfProperty(name: String): AllOfPropertyDefinition? = properties?.get(name) as? AllOfPropertyDefinition

    /**
     * Returns the boolean schema definition for [name], or null if not found or not a boolean schema.
     *
     * Note: This returns [BooleanSchemaDefinition] (true/false as the entire schema),
     * not [BooleanPropertyDefinition] (a schema for boolean values).
     */
    public fun booleanSchemaProperty(name: String): BooleanSchemaDefinition? =
        properties?.get(name) as? BooleanSchemaDefinition
}

/**
 * Interface representing a container for items in a JSON array schema.
 */
public interface ArrayContainer {
    /**
     * Schema for array items.
     */
    public val items: PropertyDefinition?

    /**
     * Array of schemas for positional items.
     */
    public val prefixItems: List<PropertyDefinition>?

    /**
     * Schema for unevaluated array items.
     */
    public val unevaluatedItems: PropertyDefinition?

    /**
     * Schema that at least one array item must match.
     */
    public val contains: PropertyDefinition?

    /**
     * Minimum number of items that must match the "contains" schema.
     */
    public val minContains: Int?

    /**
     * Maximum number of items that can match the "contains" schema.
     */
    public val maxContains: Int?

    /**
     * Minimum number of items allowed in the array.
     */
    public val minItems: Int?

    /**
     * Maximum number of items allowed in the array.
     */
    public val maxItems: Int?

    /**
     * Whether all items in the array must be unique.
     */
    public val uniqueItems: Boolean?
}

/**
 * Numeric validation keywords.
 */
public interface NumericConstraints {
    public val multipleOf: Double?
    public val minimum: Double?
    public val exclusiveMinimum: Double?
    public val maximum: Double?
    public val exclusiveMaximum: Double?
}

/**
 * String validation keywords.
 */
public interface StringConstraints {
    public val minLength: Int?
    public val maxLength: Int?
    public val pattern: String?
    public val format: String?
    public val contentEncoding: String?
    public val contentMediaType: String?
    public val contentSchema: PropertyDefinition?
}

/**
 * Combined interface for all structural validation keywords.
 */
public interface GeneralConstraints :
    PropertiesContainer,
    ArrayContainer,
    NumericConstraints,
    StringConstraints

/**
 * Common keywords for all object-based JSON schemas.
 */
public interface CommonSchemaAttributes {
    @SerialName(ID)
    public val id: String?

    @SerialName(ANCHOR)
    public val anchor: String?

    @SerialName(DYNAMIC_ANCHOR)
    public val dynamicAnchor: String?

    @SerialName(REF)
    public val ref: String?

    @SerialName(DYNAMIC_REF)
    public val dynamicRef: String?

    @SerialName(COMMENT)
    public val comment: String?
    public val title: String?
    public val description: String?

    @SerialName(DEFS)
    public val defs: Map<String, PropertyDefinition>?
    public val readOnly: Boolean?
    public val writeOnly: Boolean?
    public val deprecated: Boolean?
    public val examples: List<JsonElement>?
    public val default: JsonElement?

    @SerialName(CONST)
    public val constValue: JsonElement?

    @Serializable(with = StringOrListSerializer::class)
    public val type: List<String>?

    public val nullable: Boolean?
}

/**
 * Interface for schemas that can be composed of other schemas.
 */
public interface ApplicatorContainer {
    public val oneOf: List<PropertyDefinition>?
    public val anyOf: List<PropertyDefinition>?
    public val allOf: List<PropertyDefinition>?
    public val not: PropertyDefinition?
    public val ifSchema: PropertyDefinition?
    public val thenSchema: PropertyDefinition?
    public val elseSchema: PropertyDefinition?
}

/**
 * Represents a JSON Schema with validation constraints and metadata according to the JSON Schema specification.
 *
 * Supports object, array, string, number, and compositional schemas with validation rules,
 * type constraints, conditional logic, and metadata.
 *
 * @author Konstantin Pavlov
 */
@Serializable
@Suppress("LongParameterList")
@JsonIgnoreUnknownKeys
public data class JsonSchema(
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    @SerialName(SCHEMA)
    public val schema: String? = null,
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    @SerialName(ID)
    public override val id: String? = null,
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    @SerialName(ANCHOR)
    public override val anchor: String? = null,
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    @SerialName(DYNAMIC_ANCHOR)
    public override val dynamicAnchor: String? = null,
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    @SerialName(REF)
    public override val ref: String? = null,
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    @SerialName(DYNAMIC_REF)
    public override val dynamicRef: String? = null,
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    @SerialName(COMMENT)
    public override val comment: String? = null,
    public override val title: String? = null,
    public override val description: String? = null,
    @Serializable(with = StringOrListSerializer::class)
    @EncodeDefault
    public override val type: List<String> = OBJECT_TYPE,
    @Serializable(with = PolymorphicEnumSerializer::class)
    public val enum: List<JsonElement>? = null,
    @SerialName(CONST)
    public override val constValue: JsonElement? = null,
    public override val default: JsonElement? = null,
    public override val properties: Map<String, PropertyDefinition> = emptyMap(),
    public override val patternProperties: Map<String, PropertyDefinition>? = null,
    @SerialName(ADDITIONAL_PROPERTIES)
    public override val additionalProperties: AdditionalPropertiesConstraint? = null,
    public override val unevaluatedProperties: PropertyDefinition? = null,
    public override val propertyNames: PropertyDefinition? = null,
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    public override val required: List<String> = emptyList(),
    public override val dependentRequired: Map<String, List<String>>? = null,
    public override val dependentSchemas: Map<String, PropertyDefinition>? = null,
    public val dependencies: Map<String, JsonElement>? = null,
    @Serializable(with = NumberToNullableIntSerializer::class) public override val minProperties: Int? = null,
    @Serializable(with = NumberToNullableIntSerializer::class) public override val maxProperties: Int? = null,
    public override val items: PropertyDefinition? = null,
    public override val prefixItems: List<PropertyDefinition>? = null,
    public override val unevaluatedItems: PropertyDefinition? = null,
    public override val contains: PropertyDefinition? = null,
    @Serializable(with = NumberToNullableIntSerializer::class) public override val minContains: Int? = null,
    @Serializable(with = NumberToNullableIntSerializer::class) public override val maxContains: Int? = null,
    @Serializable(with = NumberToNullableIntSerializer::class) public override val minItems: Int? = null,
    @Serializable(with = NumberToNullableIntSerializer::class) public override val maxItems: Int? = null,
    public override val uniqueItems: Boolean? = null,
    @Serializable(with = NumberToNullableIntSerializer::class) public override val minLength: Int? = null,
    @Serializable(with = NumberToNullableIntSerializer::class) public override val maxLength: Int? = null,
    public override val pattern: String? = null,
    public override val format: String? = null,
    public override val contentEncoding: String? = null,
    public override val contentMediaType: String? = null,
    public override val contentSchema: PropertyDefinition? = null,
    public override val minimum: Double? = null,
    public override val maximum: Double? = null,
    public override val exclusiveMinimum: Double? = null,
    public override val exclusiveMaximum: Double? = null,
    public override val multipleOf: Double? = null,
    public override val oneOf: List<PropertyDefinition>? = null,
    public override val anyOf: List<PropertyDefinition>? = null,
    public override val allOf: List<PropertyDefinition>? = null,
    @SerialName(IF) public override val ifSchema: PropertyDefinition? = null,
    @SerialName("then") public override val thenSchema: PropertyDefinition? = null,
    @SerialName("else") public override val elseSchema: PropertyDefinition? = null,
    public override val not: PropertyDefinition? = null,
    public val discriminator: Discriminator? = null,
    public override val readOnly: Boolean? = null,
    public override val writeOnly: Boolean? = null,
    public override val deprecated: Boolean? = null,
    public override val examples: List<JsonElement>? = null,
    public override val nullable: Boolean? = null,
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    @SerialName(DEFS) public override val defs: Map<String, PropertyDefinition>? = null,
) : GeneralConstraints,
    CommonSchemaAttributes,
    ApplicatorContainer,
    PropertyDefinition

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
 *
 * @param T The native Kotlin type that this property definition represents (e.g., String, Double, Boolean).
 *          The type parameter enables type-safe accessors for default and const values while maintaining
 *          JSON Schema 2020-12 spec compliance by storing values as JsonElement during serialization.
 */
public sealed interface ValuePropertyDefinition<out T> :
    PropertyDefinition,
    CommonSchemaAttributes,
    ApplicatorContainer {
    /**
     * The data type of the property.
     */
    public override val type: List<String>?

    /**
     * Whether the property can be null.
     */
    public override val nullable: Boolean?

    /**
     * Default value as JsonElement (spec-compliant storage).
     * Can be any JSON value per JSON Schema 2020-12 spec.
     */
    public override val default: JsonElement?

    /**
     * Const value as JsonElement (spec-compliant storage).
     * Can be any JSON value per JSON Schema 2020-12 spec.
     */
    @SerialName(CONST)
    public override val constValue: JsonElement?

    /**
     * Returns the default value converted to the native Kotlin type T.
     * Returns null if the value is null or cannot be converted to type T.
     */
    public fun getTypedDefault(): T?

    /**
     * Returns the const value converted to the native Kotlin type T.
     * Returns null if the value is null or cannot be converted to type T.
     */
    public fun getTypedConst(): T?
}

/**
 * Represents a string property.
 */
@Serializable
@JsonIgnoreUnknownKeys
public data class StringPropertyDefinition(
    @Serializable(with = StringOrListSerializer::class) @EncodeDefault override val type: List<String> =
        STRING_TYPE,
    @SerialName(ID) override val id: String? = null,
    @SerialName(ANCHOR) override val anchor: String? = null,
    @SerialName(DYNAMIC_ANCHOR) override val dynamicAnchor: String? = null,
    @SerialName(REF) override val ref: String? = null,
    @SerialName(DYNAMIC_REF) override val dynamicRef: String? = null,
    @SerialName(COMMENT) override val comment: String? = null,
    override val title: String? = null,
    override val description: String? = null,
    override val nullable: Boolean? = null,
    override val default: JsonElement? = null,
    @SerialName(CONST) override val constValue: JsonElement? = null,
    @SerialName(DEFS) override val defs: Map<String, PropertyDefinition>? = null,
    override val readOnly: Boolean? = null,
    override val writeOnly: Boolean? = null,
    override val deprecated: Boolean? = null,
    override val examples: List<JsonElement>? = null,
    override val oneOf: List<PropertyDefinition>? = null,
    override val anyOf: List<PropertyDefinition>? = null,
    override val allOf: List<PropertyDefinition>? = null,
    override val not: PropertyDefinition? = null,
    @SerialName(IF) override val ifSchema: PropertyDefinition? = null,
    @SerialName("then") override val thenSchema: PropertyDefinition? = null,
    @SerialName("else") override val elseSchema: PropertyDefinition? = null,
    override val format: String? = null,
    override val contentEncoding: String? = null,
    override val contentMediaType: String? = null,
    override val contentSchema: PropertyDefinition? = null,
    @Serializable(with = StringEnumSerializer::class)
    val enum: List<String>? = null,
    @Serializable(with = NumberToNullableIntSerializer::class) override val minLength: Int? = null,
    @Serializable(with = NumberToNullableIntSerializer::class) override val maxLength: Int? = null,
    override val pattern: String? = null,
) : ValuePropertyDefinition<String>,
    StringConstraints {
    /**
     * Returns the default value as a String, or null if not set or not a string.
     */
    override fun getTypedDefault(): String? = default?.jsonPrimitive?.contentOrNull

    /**
     * Returns the const value as a String, or null if not set or not a string.
     */
    override fun getTypedConst(): String? = constValue?.jsonPrimitive?.contentOrNull
}

/**
 * Represents a numeric property (integer or number).
 */
@Serializable
@JsonIgnoreUnknownKeys
public data class NumericPropertyDefinition(
    @Serializable(with = StringOrListSerializer::class) override val type: List<String>,
    @SerialName(ID) override val id: String? = null,
    @SerialName(ANCHOR) override val anchor: String? = null,
    @SerialName(DYNAMIC_ANCHOR) override val dynamicAnchor: String? = null,
    @SerialName(REF) override val ref: String? = null,
    @SerialName(DYNAMIC_REF) override val dynamicRef: String? = null,
    @SerialName(COMMENT) override val comment: String? = null,
    override val title: String? = null,
    override val description: String? = null,
    override val nullable: Boolean? = null,
    override val default: JsonElement? = null,
    @SerialName(CONST) override val constValue: JsonElement? = null,
    @SerialName(DEFS) override val defs: Map<String, PropertyDefinition>? = null,
    override val readOnly: Boolean? = null,
    override val writeOnly: Boolean? = null,
    override val deprecated: Boolean? = null,
    override val examples: List<JsonElement>? = null,
    override val oneOf: List<PropertyDefinition>? = null,
    override val anyOf: List<PropertyDefinition>? = null,
    override val allOf: List<PropertyDefinition>? = null,
    override val not: PropertyDefinition? = null,
    @SerialName(IF) override val ifSchema: PropertyDefinition? = null,
    @SerialName(THEN) override val thenSchema: PropertyDefinition? = null,
    @SerialName(ELSE) override val elseSchema: PropertyDefinition? = null,
    @Serializable(with = NumericEnumSerializer::class)
    val enum: List<Double>? = null,
    override val multipleOf: Double? = null,
    override val minimum: Double? = null,
    override val exclusiveMinimum: Double? = null,
    override val maximum: Double? = null,
    override val exclusiveMaximum: Double? = null,
) : ValuePropertyDefinition<Double>,
    NumericConstraints {
    /**
     * Returns the default value as a Double, or null if not set or not a number.
     */
    override fun getTypedDefault(): Double? = default?.jsonPrimitive?.doubleOrNull

    /**
     * Returns the const value as a Double, or null if not set or not a number.
     */
    override fun getTypedConst(): Double? = constValue?.jsonPrimitive?.doubleOrNull
}

/**
 * Represents an array property
 */
@Serializable
@JsonIgnoreUnknownKeys
public data class ArrayPropertyDefinition(
    @Serializable(with = StringOrListSerializer::class) @EncodeDefault override val type: List<String> =
        ARRAY_TYPE,
    @SerialName(ID) override val id: String? = null,
    @SerialName(ANCHOR) override val anchor: String? = null,
    @SerialName(DYNAMIC_ANCHOR) override val dynamicAnchor: String? = null,
    @SerialName(REF) override val ref: String? = null,
    @SerialName(DYNAMIC_REF) override val dynamicRef: String? = null,
    @SerialName(COMMENT) override val comment: String? = null,
    override val title: String? = null,
    override val description: String? = null,
    override val nullable: Boolean? = null,
    override val default: JsonElement? = null,
    @SerialName(CONST) override val constValue: JsonElement? = null,
    @SerialName(DEFS) override val defs: Map<String, PropertyDefinition>? = null,
    override val readOnly: Boolean? = null,
    override val writeOnly: Boolean? = null,
    override val deprecated: Boolean? = null,
    override val examples: List<JsonElement>? = null,
    override val oneOf: List<PropertyDefinition>? = null,
    override val anyOf: List<PropertyDefinition>? = null,
    override val allOf: List<PropertyDefinition>? = null,
    override val not: PropertyDefinition? = null,
    @SerialName(IF) override val ifSchema: PropertyDefinition? = null,
    @SerialName("then") override val thenSchema: PropertyDefinition? = null,
    @SerialName("else") override val elseSchema: PropertyDefinition? = null,
    @Serializable(with = ArrayEnumSerializer::class)
    val enum: List<JsonArray>? = null,
    override val items: PropertyDefinition? = null,
    override val prefixItems: List<PropertyDefinition>? = null,
    override val unevaluatedItems: PropertyDefinition? = null,
    override val contains: PropertyDefinition? = null,
    @Serializable(with = NumberToNullableIntSerializer::class) override val minContains: Int? = null,
    @Serializable(with = NumberToNullableIntSerializer::class) override val maxContains: Int? = null,
    @Serializable(with = NumberToNullableIntSerializer::class) override val minItems: Int? = null,
    @Serializable(with = NumberToNullableIntSerializer::class) override val maxItems: Int? = null,
    override val uniqueItems: Boolean? = null,
) : ValuePropertyDefinition<JsonArray>,
    ArrayContainer {
    /**
     * Returns the default value as a JsonArray, or null if not set or not an array.
     */
    override fun getTypedDefault(): JsonArray? = default as? JsonArray

    /**
     * Returns the const value as a JsonArray, or null if not set or not an array.
     */
    override fun getTypedConst(): JsonArray? = constValue as? JsonArray
}

/**
 * Represents an object property
 */
@Serializable
@JsonIgnoreUnknownKeys
public data class ObjectPropertyDefinition(
    @Serializable(with = StringOrListSerializer::class) @EncodeDefault override val type: List<String> =
        OBJECT_TYPE,
    @SerialName(ID) override val id: String? = null,
    @SerialName(ANCHOR) override val anchor: String? = null,
    @SerialName(DYNAMIC_ANCHOR) override val dynamicAnchor: String? = null,
    @SerialName(REF) override val ref: String? = null,
    @SerialName(DYNAMIC_REF) override val dynamicRef: String? = null,
    @SerialName(COMMENT) override val comment: String? = null,
    override val title: String? = null,
    override val description: String? = null,
    override val nullable: Boolean? = null,
    override val default: JsonElement? = null,
    @SerialName(CONST) override val constValue: JsonElement? = null,
    @SerialName(DEFS) override val defs: Map<String, PropertyDefinition>? = null,
    override val readOnly: Boolean? = null,
    override val writeOnly: Boolean? = null,
    override val deprecated: Boolean? = null,
    override val examples: List<JsonElement>? = null,
    override val oneOf: List<PropertyDefinition>? = null,
    override val anyOf: List<PropertyDefinition>? = null,
    override val allOf: List<PropertyDefinition>? = null,
    override val not: PropertyDefinition? = null,
    @SerialName(IF) override val ifSchema: PropertyDefinition? = null,
    @SerialName("then") override val thenSchema: PropertyDefinition? = null,
    @SerialName("else") override val elseSchema: PropertyDefinition? = null,
    @Serializable(with = ObjectEnumSerializer::class)
    val enum: List<JsonObject>? = null,
    override val properties: Map<String, PropertyDefinition>? = null,
    override val patternProperties: Map<String, PropertyDefinition>? = null,
    override val unevaluatedProperties: PropertyDefinition? = null,
    override val propertyNames: PropertyDefinition? = null,
    override val required: List<String>? = null,
    override val dependentRequired: Map<String, List<String>>? = null,
    override val dependentSchemas: Map<String, PropertyDefinition>? = null,
    val dependencies: Map<String, JsonElement>? = null,
    @Serializable(with = NumberToNullableIntSerializer::class) override val minProperties: Int? = null,
    @Serializable(with = NumberToNullableIntSerializer::class) override val maxProperties: Int? = null,
    @SerialName("additionalProperties") override val additionalProperties: AdditionalPropertiesConstraint? = null,
) : ValuePropertyDefinition<JsonObject>,
    PropertiesContainer {
    /**
     * Returns the default value as a JsonObject, or null if not set or not an object.
     */
    override fun getTypedDefault(): JsonObject? = default as? JsonObject

    /**
     * Returns the const value as a JsonObject, or null if not set or not an object.
     */
    override fun getTypedConst(): JsonObject? = constValue as? JsonObject
}

/**
 * Represents a boolean property
 */
@Serializable
@JsonIgnoreUnknownKeys
public data class BooleanPropertyDefinition(
    @Serializable(with = StringOrListSerializer::class) @EncodeDefault override val type: List<String> =
        BOOLEAN_TYPE,
    @SerialName(ID) override val id: String? = null,
    @SerialName(ANCHOR) override val anchor: String? = null,
    @SerialName(DYNAMIC_ANCHOR) override val dynamicAnchor: String? = null,
    @SerialName(REF) override val ref: String? = null,
    @SerialName(DYNAMIC_REF) override val dynamicRef: String? = null,
    @SerialName(COMMENT) override val comment: String? = null,
    override val title: String? = null,
    override val description: String? = null,
    override val nullable: Boolean? = null,
    override val default: JsonElement? = null,
    @SerialName(CONST) override val constValue: JsonElement? = null,
    @SerialName(DEFS) override val defs: Map<String, PropertyDefinition>? = null,
    override val readOnly: Boolean? = null,
    override val writeOnly: Boolean? = null,
    override val deprecated: Boolean? = null,
    override val examples: List<JsonElement>? = null,
    override val oneOf: List<PropertyDefinition>? = null,
    override val anyOf: List<PropertyDefinition>? = null,
    override val allOf: List<PropertyDefinition>? = null,
    override val not: PropertyDefinition? = null,
    @SerialName(IF) override val ifSchema: PropertyDefinition? = null,
    @SerialName("then") override val thenSchema: PropertyDefinition? = null,
    @SerialName("else") override val elseSchema: PropertyDefinition? = null,
    @Serializable(with = BooleanEnumSerializer::class)
    val enum: List<Boolean>? = null,
) : ValuePropertyDefinition<Boolean> {
    /**
     * Returns the default value as a Boolean, or null if not set or not a boolean.
     */
    override fun getTypedDefault(): Boolean? = default?.jsonPrimitive?.booleanOrNull

    /**
     * Returns the const value as a Boolean, or null if not set or not a boolean.
     */
    override fun getTypedConst(): Boolean? = constValue?.jsonPrimitive?.booleanOrNull
}

/**
 * Represents a property definition without specific type constraints.
 *
 * Used for schemas that:
 * - Have no type specified
 * - Have multiple types
 * - Have heterogeneous enums with mixed types
 *
 * This allows maximum flexibility while still supporting validation keywords like enum.
 */
@Serializable
@JsonIgnoreUnknownKeys
public data class GenericPropertyDefinition(
    @Serializable(with = StringOrListSerializer::class)
    override val type: List<String>? = null,
    @SerialName(ID) override val id: String? = null,
    @SerialName(ANCHOR) override val anchor: String? = null,
    @SerialName(DYNAMIC_ANCHOR) override val dynamicAnchor: String? = null,
    @SerialName(REF) override val ref: String? = null,
    @SerialName(DYNAMIC_REF) override val dynamicRef: String? = null,
    @SerialName(COMMENT) override val comment: String? = null,
    override val title: String? = null,
    override val description: String? = null,
    override val nullable: Boolean? = null,
    @Serializable(with = PolymorphicEnumSerializer::class)
    val enum: List<JsonElement>? = null,
    override val default: JsonElement? = null,
    @SerialName(CONST) override val constValue: JsonElement? = null,
    @SerialName(DEFS) override val defs: Map<String, PropertyDefinition>? = null,
    override val readOnly: Boolean? = null,
    override val writeOnly: Boolean? = null,
    override val deprecated: Boolean? = null,
    override val examples: List<JsonElement>? = null,
    override val oneOf: List<PropertyDefinition>? = null,
    override val anyOf: List<PropertyDefinition>? = null,
    override val allOf: List<PropertyDefinition>? = null,
    override val not: PropertyDefinition? = null,
    /**
     * This keyword's value MUST be a valid JSON Schema.
     *
     * See [10.2.2.1. if](https://json-schema.org/draft/2020-12/draft-bhutton-json-schema-01#name-if)
     */
    @SerialName(IF) override val ifSchema: PropertyDefinition? = null,
    /**
     * This keyword's value MUST be a valid JSON Schema.
     *
     * See [10.2.2.2. then](https://json-schema.org/draft/2020-12/draft-bhutton-json-schema-01#name-then)
     */
    @SerialName("then") override val thenSchema: PropertyDefinition? = null,
    /**
     * This keyword's value MUST be a valid JSON Schema.
     *
     * See [10.2.2.3. else](https://json-schema.org/draft/2020-12/draft-bhutton-json-schema-01#name-else)
     */
    @SerialName("else") override val elseSchema: PropertyDefinition? = null,
    override val properties: Map<String, PropertyDefinition>? = null,
    override val patternProperties: Map<String, PropertyDefinition>? = null,
    @SerialName("additionalProperties") override val additionalProperties: AdditionalPropertiesConstraint? = null,
    override val propertyNames: PropertyDefinition? = null,
    override val required: List<String>? = null,
    override val dependentRequired: Map<String, List<String>>? = null,
    override val dependentSchemas: Map<String, PropertyDefinition>? = null,
    val dependencies: Map<String, JsonElement>? = null,
    override val unevaluatedItems: PropertyDefinition? = null,
    override val unevaluatedProperties: PropertyDefinition? = null,
    override val items: PropertyDefinition? = null,
    override val prefixItems: List<PropertyDefinition>? = null,
    override val contains: PropertyDefinition? = null,
    @Serializable(with = NumberToNullableIntSerializer::class) override val minContains: Int? = null,
    @Serializable(with = NumberToNullableIntSerializer::class) override val maxContains: Int? = null,
    @Serializable(with = NumberToNullableIntSerializer::class) override val minLength: Int? = null,
    @Serializable(with = NumberToNullableIntSerializer::class) override val maxLength: Int? = null,
    override val pattern: String? = null,
    override val format: String? = null,
    /**
     * If the instance value is a string, this property defines that the string SHOULD be interpreted
     * as encoded binary data and decoded using the encoding named by this property.
     *
     * The value of this property MUST be a string.
     *
     * See [8.3. contentEncoding](https://json-schema.org/draft/2020-12/draft-bhutton-json-schema-validation-01#name-contentencoding)
     */
    override val contentEncoding: String? = null,
    /**
     * If the instance is a string, this property indicates the media type of the contents of the string.
     * If [contentEncoding] is present, this property describes the decoded string.
     *
     * The value of this property MUST be a string, which MUST be a media type, as defined by RFC 2046
     *
     * See [8.4. contentMediaType](https://json-schema.org/draft/2020-12/draft-bhutton-json-schema-validation-01#name-contentmediatype)
     */
    override val contentMediaType: String? = null,
    /**
     * If the instance is a string, and if "contentMediaType" is present,
     * this property contains a schema which describes the structure of the string.
     *
     * This keyword MAY be used with any media type that can be mapped into JSON Schema's data model.
     *
     * The value of this property MUST be a valid JSON schema.
     * It SHOULD be ignored if "contentMediaType" is not present.
     */
    override val contentSchema: PropertyDefinition? = null,
    override val minimum: Double? = null,
    override val maximum: Double? = null,
    override val exclusiveMinimum: Double? = null,
    override val exclusiveMaximum: Double? = null,
    override val multipleOf: Double? = null,
    @Serializable(with = NumberToNullableIntSerializer::class) override val minItems: Int? = null,
    @Serializable(with = NumberToNullableIntSerializer::class) override val maxItems: Int? = null,
    override val uniqueItems: Boolean? = null,
    @Serializable(with = NumberToNullableIntSerializer::class) override val minProperties: Int? = null,
    @Serializable(with = NumberToNullableIntSerializer::class) override val maxProperties: Int? = null,
) : ValuePropertyDefinition<JsonElement>,
    GeneralConstraints {
    /**
     * Returns the default value as a JsonElement (any JSON value).
     */
    override fun getTypedDefault(): JsonElement? = default

    /**
     * Returns the const value as a JsonElement (any JSON value).
     */
    override fun getTypedConst(): JsonElement? = constValue
}

/**
 * Represents a reference to another element
 */
@Serializable
@JsonIgnoreUnknownKeys
public data class ReferencePropertyDefinition(
    @SerialName(REF) override val ref: String? = null,
    @SerialName(DYNAMIC_REF) override val dynamicRef: String? = null,
    @SerialName(ID) override val id: String? = null,
    @SerialName(ANCHOR) override val anchor: String? = null,
    @SerialName(DYNAMIC_ANCHOR) override val dynamicAnchor: String? = null,
    @SerialName(COMMENT) override val comment: String? = null,
    override val title: String? = null,
    override val description: String? = null,
    override val nullable: Boolean? = null,
    override val default: JsonElement? = null,
    @SerialName(CONST) override val constValue: JsonElement? = null,
    @Serializable(with = StringOrListSerializer::class)
    override val type: List<String>? = null,
    @SerialName(DEFS) override val defs: Map<String, PropertyDefinition>? = null,
    override val readOnly: Boolean? = null,
    override val writeOnly: Boolean? = null,
    override val deprecated: Boolean? = null,
    override val examples: List<JsonElement>? = null,
    override val oneOf: List<PropertyDefinition>? = null,
    override val anyOf: List<PropertyDefinition>? = null,
    override val allOf: List<PropertyDefinition>? = null,
    override val not: PropertyDefinition? = null,
    @SerialName(IF) override val ifSchema: PropertyDefinition? = null,
    @SerialName("then") override val thenSchema: PropertyDefinition? = null,
    @SerialName("else") override val elseSchema: PropertyDefinition? = null,
    @Serializable(with = PolymorphicEnumSerializer::class)
    val enum: List<JsonElement>? = null,
    @Serializable(with = NumberToNullableIntSerializer::class) override val minLength: Int? = null,
    @Serializable(with = NumberToNullableIntSerializer::class) override val maxLength: Int? = null,
    override val pattern: String? = null,
    override val format: String? = null,
    override val contentEncoding: String? = null,
    override val contentMediaType: String? = null,
    override val contentSchema: PropertyDefinition? = null,
    override val minimum: Double? = null,
    override val maximum: Double? = null,
    override val exclusiveMinimum: Double? = null,
    override val exclusiveMaximum: Double? = null,
    override val multipleOf: Double? = null,
    override val properties: Map<String, PropertyDefinition>? = null,
    override val patternProperties: Map<String, PropertyDefinition>? = null,
    @SerialName("additionalProperties") override val additionalProperties: AdditionalPropertiesConstraint? = null,
    override val unevaluatedProperties: PropertyDefinition? = null,
    override val propertyNames: PropertyDefinition? = null,
    override val required: List<String>? = null,
    override val dependentRequired: Map<String, List<String>>? = null,
    override val dependentSchemas: Map<String, PropertyDefinition>? = null,
    @Serializable(with = NumberToNullableIntSerializer::class) override val minProperties: Int? = null,
    @Serializable(with = NumberToNullableIntSerializer::class) override val maxProperties: Int? = null,
    override val items: PropertyDefinition? = null,
    override val prefixItems: List<PropertyDefinition>? = null,
    override val unevaluatedItems: PropertyDefinition? = null,
    override val contains: PropertyDefinition? = null,
    @Serializable(with = NumberToNullableIntSerializer::class) override val minContains: Int? = null,
    @Serializable(with = NumberToNullableIntSerializer::class) override val maxContains: Int? = null,
    @Serializable(with = NumberToNullableIntSerializer::class) override val minItems: Int? = null,
    @Serializable(with = NumberToNullableIntSerializer::class) override val maxItems: Int? = null,
    override val uniqueItems: Boolean? = null,
) : PropertyDefinition,
    CommonSchemaAttributes,
    ApplicatorContainer,
    GeneralConstraints

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
    @EncodeDefault
    val propertyName: String = "type",
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
@JsonIgnoreUnknownKeys
public data class OneOfPropertyDefinition(
    override val oneOf: List<PropertyDefinition>,
    @SerialName(ID) override val id: String? = null,
    @SerialName(ANCHOR) override val anchor: String? = null,
    @SerialName(DYNAMIC_ANCHOR) override val dynamicAnchor: String? = null,
    @SerialName(COMMENT) override val comment: String? = null,
    override val title: String? = null,
    override val description: String? = null,
    override val nullable: Boolean? = null,
    override val default: JsonElement? = null,
    @SerialName(CONST) override val constValue: JsonElement? = null,
    @Serializable(with = StringOrListSerializer::class)
    override val type: List<String>? = null,
    @SerialName(DEFS) override val defs: Map<String, PropertyDefinition>? = null,
    override val readOnly: Boolean? = null,
    override val writeOnly: Boolean? = null,
    override val deprecated: Boolean? = null,
    override val examples: List<JsonElement>? = null,
    override val anyOf: List<PropertyDefinition>? = null,
    override val allOf: List<PropertyDefinition>? = null,
    override val not: PropertyDefinition? = null,
    @SerialName(IF) override val ifSchema: PropertyDefinition? = null,
    @SerialName("then") override val thenSchema: PropertyDefinition? = null,
    @SerialName("else") override val elseSchema: PropertyDefinition? = null,
    @Serializable(with = PolymorphicEnumSerializer::class)
    val enum: List<JsonElement>? = null,
    @Serializable(with = NumberToNullableIntSerializer::class) override val minLength: Int? = null,
    @Serializable(with = NumberToNullableIntSerializer::class) override val maxLength: Int? = null,
    override val pattern: String? = null,
    override val format: String? = null,
    override val contentEncoding: String? = null,
    override val contentMediaType: String? = null,
    override val contentSchema: PropertyDefinition? = null,
    override val minimum: Double? = null,
    override val maximum: Double? = null,
    override val exclusiveMinimum: Double? = null,
    override val exclusiveMaximum: Double? = null,
    override val multipleOf: Double? = null,
    val discriminator: Discriminator? = null,
    override val properties: Map<String, PropertyDefinition>? = null,
    override val patternProperties: Map<String, PropertyDefinition>? = null,
    @SerialName("additionalProperties") override val additionalProperties: AdditionalPropertiesConstraint? = null,
    override val unevaluatedProperties: PropertyDefinition? = null,
    override val propertyNames: PropertyDefinition? = null,
    override val required: List<String>? = null,
    override val dependentRequired: Map<String, List<String>>? = null,
    override val dependentSchemas: Map<String, PropertyDefinition>? = null,
    @Serializable(with = NumberToNullableIntSerializer::class) override val minProperties: Int? = null,
    @Serializable(with = NumberToNullableIntSerializer::class) override val maxProperties: Int? = null,
    override val items: PropertyDefinition? = null,
    override val prefixItems: List<PropertyDefinition>? = null,
    override val unevaluatedItems: PropertyDefinition? = null,
    override val contains: PropertyDefinition? = null,
    @Serializable(with = NumberToNullableIntSerializer::class) override val minContains: Int? = null,
    @Serializable(with = NumberToNullableIntSerializer::class) override val maxContains: Int? = null,
    @Serializable(with = NumberToNullableIntSerializer::class) override val minItems: Int? = null,
    @Serializable(with = NumberToNullableIntSerializer::class) override val maxItems: Int? = null,
    override val uniqueItems: Boolean? = null,
    @SerialName(REF) override val ref: String? = null,
    @SerialName(DYNAMIC_REF) override val dynamicRef: String? = null,
) : PropertyDefinition,
    CommonSchemaAttributes,
    ApplicatorContainer,
    GeneralConstraints

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
@JsonIgnoreUnknownKeys
public data class AnyOfPropertyDefinition(
    override val anyOf: List<PropertyDefinition>,
    @SerialName(ID) override val id: String? = null,
    @SerialName(ANCHOR) override val anchor: String? = null,
    @SerialName(DYNAMIC_ANCHOR) override val dynamicAnchor: String? = null,
    @SerialName(COMMENT) override val comment: String? = null,
    override val title: String? = null,
    override val description: String? = null,
    override val nullable: Boolean? = null,
    override val default: JsonElement? = null,
    @SerialName(CONST) override val constValue: JsonElement? = null,
    @Serializable(with = StringOrListSerializer::class) override val type: List<String>? = null,
    @SerialName(DEFS) override val defs: Map<String, PropertyDefinition>? = null,
    override val readOnly: Boolean? = null,
    override val writeOnly: Boolean? = null,
    override val deprecated: Boolean? = null,
    override val examples: List<JsonElement>? = null,
    override val oneOf: List<PropertyDefinition>? = null,
    override val allOf: List<PropertyDefinition>? = null,
    override val not: PropertyDefinition? = null,
    @SerialName(IF) override val ifSchema: PropertyDefinition? = null,
    @SerialName("then") override val thenSchema: PropertyDefinition? = null,
    @SerialName("else") override val elseSchema: PropertyDefinition? = null,
    @Serializable(with = PolymorphicEnumSerializer::class)
    val enum: List<JsonElement>? = null,
    @Serializable(with = NumberToNullableIntSerializer::class) override val minLength: Int? = null,
    @Serializable(with = NumberToNullableIntSerializer::class) override val maxLength: Int? = null,
    override val pattern: String? = null,
    override val format: String? = null,
    override val contentEncoding: String? = null,
    override val contentMediaType: String? = null,
    override val contentSchema: PropertyDefinition? = null,
    override val minimum: Double? = null,
    override val maximum: Double? = null,
    override val exclusiveMinimum: Double? = null,
    override val exclusiveMaximum: Double? = null,
    override val multipleOf: Double? = null,
    override val properties: Map<String, PropertyDefinition>? = null,
    override val patternProperties: Map<String, PropertyDefinition>? = null,
    @SerialName("additionalProperties") override val additionalProperties: AdditionalPropertiesConstraint? = null,
    override val unevaluatedProperties: PropertyDefinition? = null,
    override val propertyNames: PropertyDefinition? = null,
    override val required: List<String>? = null,
    override val dependentRequired: Map<String, List<String>>? = null,
    override val dependentSchemas: Map<String, PropertyDefinition>? = null,
    @Serializable(with = NumberToNullableIntSerializer::class) override val minProperties: Int? = null,
    @Serializable(with = NumberToNullableIntSerializer::class) override val maxProperties: Int? = null,
    override val items: PropertyDefinition? = null,
    override val prefixItems: List<PropertyDefinition>? = null,
    override val unevaluatedItems: PropertyDefinition? = null,
    override val contains: PropertyDefinition? = null,
    @Serializable(with = NumberToNullableIntSerializer::class) override val minContains: Int? = null,
    @Serializable(with = NumberToNullableIntSerializer::class) override val maxContains: Int? = null,
    @Serializable(with = NumberToNullableIntSerializer::class) override val minItems: Int? = null,
    @Serializable(with = NumberToNullableIntSerializer::class) override val maxItems: Int? = null,
    override val uniqueItems: Boolean? = null,
    @SerialName(REF) override val ref: String? = null,
    @SerialName(DYNAMIC_REF) override val dynamicRef: String? = null,
) : PropertyDefinition,
    CommonSchemaAttributes,
    ApplicatorContainer,
    GeneralConstraints

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
@JsonIgnoreUnknownKeys
public data class AllOfPropertyDefinition(
    override val allOf: List<PropertyDefinition>,
    @SerialName(ID) override val id: String? = null,
    @SerialName(ANCHOR) override val anchor: String? = null,
    @SerialName(DYNAMIC_ANCHOR) override val dynamicAnchor: String? = null,
    @SerialName(COMMENT) override val comment: String? = null,
    override val title: String? = null,
    override val description: String? = null,
    override val nullable: Boolean? = null,
    override val default: JsonElement? = null,
    @SerialName(CONST) override val constValue: JsonElement? = null,
    @Serializable(with = StringOrListSerializer::class) override val type: List<String>? = null,
    @SerialName(DEFS) override val defs: Map<String, PropertyDefinition>? = null,
    override val readOnly: Boolean? = null,
    override val writeOnly: Boolean? = null,
    override val deprecated: Boolean? = null,
    override val examples: List<JsonElement>? = null,
    override val oneOf: List<PropertyDefinition>? = null,
    override val anyOf: List<PropertyDefinition>? = null,
    override val not: PropertyDefinition? = null,
    @SerialName(IF) override val ifSchema: PropertyDefinition? = null,
    @SerialName("then") override val thenSchema: PropertyDefinition? = null,
    @SerialName("else") override val elseSchema: PropertyDefinition? = null,
    @Serializable(with = PolymorphicEnumSerializer::class)
    val enum: List<JsonElement>? = null,
    @Serializable(with = NumberToNullableIntSerializer::class) override val minLength: Int? = null,
    @Serializable(with = NumberToNullableIntSerializer::class) override val maxLength: Int? = null,
    override val pattern: String? = null,
    override val format: String? = null,
    override val contentEncoding: String? = null,
    override val contentMediaType: String? = null,
    override val contentSchema: PropertyDefinition? = null,
    override val minimum: Double? = null,
    override val maximum: Double? = null,
    override val exclusiveMinimum: Double? = null,
    override val exclusiveMaximum: Double? = null,
    override val multipleOf: Double? = null,
    override val properties: Map<String, PropertyDefinition>? = null,
    override val patternProperties: Map<String, PropertyDefinition>? = null,
    @SerialName("additionalProperties") override val additionalProperties: AdditionalPropertiesConstraint? = null,
    override val unevaluatedProperties: PropertyDefinition? = null,
    override val propertyNames: PropertyDefinition? = null,
    override val required: List<String>? = null,
    override val dependentRequired: Map<String, List<String>>? = null,
    override val dependentSchemas: Map<String, PropertyDefinition>? = null,
    @Serializable(with = NumberToNullableIntSerializer::class) override val minProperties: Int? = null,
    @Serializable(with = NumberToNullableIntSerializer::class) override val maxProperties: Int? = null,
    override val items: PropertyDefinition? = null,
    override val prefixItems: List<PropertyDefinition>? = null,
    override val unevaluatedItems: PropertyDefinition? = null,
    override val contains: PropertyDefinition? = null,
    @Serializable(with = NumberToNullableIntSerializer::class) override val minContains: Int? = null,
    @Serializable(with = NumberToNullableIntSerializer::class) override val maxContains: Int? = null,
    @Serializable(with = NumberToNullableIntSerializer::class) override val minItems: Int? = null,
    @Serializable(with = NumberToNullableIntSerializer::class) override val maxItems: Int? = null,
    override val uniqueItems: Boolean? = null,
    @SerialName(REF) override val ref: String? = null,
    @SerialName(DYNAMIC_REF) override val dynamicRef: String? = null,
) : PropertyDefinition,
    CommonSchemaAttributes,
    ApplicatorContainer,
    GeneralConstraints
