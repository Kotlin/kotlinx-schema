@file:Suppress("TooManyFunctions")

package kotlinx.schema.json

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/**
 * Marker annotation for the JSON Schema DSL.
 *
 * This annotation is used to indicate the context of the JSON Schema DSL,
 * enabling safer and declarative construction of JSON Schema definitions
 * within a DSL using Kotlin's type-safe builders.
 *
 * Applying this annotation helps prevent accidental mixing of DSL contexts
 * by restricting the scope of the annotated receivers within the DSL usage.
 *
 * @see DslMarker
 *
 * @author Konstantin Pavlov
 */
@DslMarker
public annotation class JsonSchemaDsl

/**
 * DSL for building JSON Schema objects in a type-safe and readable way.
 *
 * This is the main entry point for creating JSON Schema definitions using a Kotlin DSL.
 * The DSL provides type-safe builders for all JSON Schema property types with automatic
 * validation and conversion.
 *
 * ## Basic Example
 * ```kotlin
 * val schema = jsonSchema {
 *     name = "UserEmail"
 *     strict = false
 *     schema {
 *         property("email") {
 *             required = true
 *             string {
 *                 description = "Email address"
 *                 format = "email"
 *             }
 *         }
 *     }
 * }
 * ```
 *
 * ## Schema with Multiple Property Types
 * ```kotlin
 * val schema = jsonSchema {
 *     name = "Config"
 *     schema {
 *         property("enabled") {
 *             boolean {
 *                 description = "Feature enabled"
 *                 default = true
 *             }
 *         }
 *         property("count") {
 *             integer {
 *                 description = "Item count"
 *                 default = 10
 *             }
 *         }
 *         property("score") {
 *             number {
 *                 description = "User score"
 *                 minimum = 0.0
 *                 maximum = 100.0
 *             }
 *         }
 *     }
 * }
 * ```
 *
 * ## Schema with Arrays
 * ```kotlin
 * val schema = jsonSchema {
 *     name = "Tags"
 *     schema {
 *         property("tags") {
 *             array {
 *                 description = "List of tags"
 *                 minItems = 1
 *                 maxItems = 10
 *                 ofString()
 *             }
 *         }
 *     }
 * }
 * ```
 *
 * ## Schema with Nested Objects
 * ```kotlin
 * val schema = jsonSchema {
 *     name = "Metadata"
 *     schema {
 *         property("metadata") {
 *             obj {
 *                 description = "User metadata"
 *                 property("createdAt") {
 *                     required = true
 *                     string {
 *                         format = "date-time"
 *                     }
 *                 }
 *             }
 *         }
 *     }
 * }
 * ```
 *
 * @param block Configuration block for the schema builder
 * @return A fully constructed [JsonSchema] instance
 * @see JsonSchemaBuilder
 */
public fun jsonSchema(block: JsonSchemaBuilder.() -> Unit): JsonSchema = JsonSchemaBuilder().apply(block).build()

/**
 * Builder for [JsonSchema] instances.
 *
 * This is the top-level builder used within the [jsonSchema] DSL function.
 * It configures the schema's metadata and delegates to [JsonSchemaDefinitionBuilder]
 * for the actual schema structure.
 *
 * ## Properties
 * - [name]: The schema name (required)
 * - [strict]: Whether the schema enforces strict validation (default: false)
 * - [description]: Optional human-readable description
 *
 * ## Example
 * ```kotlin
 * jsonSchema {
 *     name = "UserStatus"
 *     strict = true
 *     description = "Describes user account status"
 *     schema {
 *         property("status") {
 *             string {
 *                 enum = listOf("active", "inactive", "pending")
 *             }
 *         }
 *     }
 * }
 * ```
 *
 * @see jsonSchema
 * @see JsonSchemaDefinitionBuilder
 */
@JsonSchemaDsl
public class JsonSchemaBuilder {
    /**
     * The name of the schema. Must not be empty.
     */
    public var name: String = ""

    /**
     * Whether the schema enforces strict validation.
     * Defaults to false.
     */
    public var strict: Boolean = false

    /**
     * Optional human-readable description of the schema.
     */
    public var description: String? = null

    private var schemaDefinition: JsonSchemaDefinition? = null

    /**
     * Defines the schema structure and properties.
     *
     * @param block Configuration block for the schema definition
     * @see JsonSchemaDefinitionBuilder
     */
    public fun schema(block: JsonSchemaDefinitionBuilder.() -> Unit) {
        schemaDefinition = JsonSchemaDefinitionBuilder().apply(block).build()
    }

    public fun build(): JsonSchema {
        require(name.isNotEmpty()) { "Schema name must not be empty" }
        requireNotNull(schemaDefinition) { "Schema definition must be provided" }
        return JsonSchema(
            name = name,
            strict = strict,
            description = description,
            schema = schemaDefinition!!,
        )
    }
}

/**
 * Builder for [JsonSchemaDefinition].
 *
 * Defines the structure of a JSON Schema including its properties, constraints,
 * and metadata. This builder is used within the `schema { }` block of [JsonSchemaBuilder].
 *
 * ## Example with Multiple Properties
 * ```kotlin
 * schema {
 *     additionalProperties = false
 *     property("id") {
 *         required = true
 *         string { format = "uuid" }
 *     }
 *     property("email") {
 *         required = true
 *         string { format = "email" }
 *     }
 * }
 * ```
 *
 * @see JsonSchemaBuilder
 * @see PropertyBuilder
 */
@JsonSchemaDsl
public class JsonSchemaDefinitionBuilder {
    /**
     * Optional schema identifier (JSON Schema $id).
     */
    public var id: String? = null

    /**
     * Optional schema version reference (JSON Schema $schema).
     */
    public var schema: String? = null

    /**
     * Whether additional properties beyond those defined are allowed.
     * - `true`: Additional properties allowed
     * - `false`: Only defined properties allowed
     * - `null`: No constraint (default)
     */
    public var additionalProperties: Boolean? = null

    /**
     * Optional human-readable description.
     */
    public var description: String? = null

    /**
     * Optional items definition for array schemas.
     */
    public var items: ObjectPropertyDefinition? = null

    private val properties: MutableMap<String, PropertyDefinition> = mutableMapOf()
    private val requiredFields: MutableSet<String> = mutableSetOf()

    /**
     * Defines a property in the schema.
     *
     * The property name is specified as a parameter, and the property type
     * and constraints are defined in the block using [PropertyBuilder].
     *
     * ## Example
     * ```kotlin
     * property("email") {
     *     required = true
     *     string {
     *         description = "Email address"
     *         format = "email"
     *     }
     * }
     * ```
     *
     * @param name The property name
     * @param block Configuration block for the property
     * @see PropertyBuilder
     */
    public fun property(
        name: String,
        block: PropertyBuilder.() -> PropertyDefinition,
    ) {
        val builder = PropertyBuilder()
        properties[name] = builder.block()
        if (builder.required) {
            requiredFields.add(name)
        }
    }

    public fun build(): JsonSchemaDefinition =
        JsonSchemaDefinition(
            id = id,
            schema = schema,
            properties = properties,
            required = requiredFields.toList(),
            additionalProperties = additionalProperties,
            description = description,
            items = items,
        )
}

/**
 * Builder for defining property types and constraints.
 *
 * This builder provides methods to specify the type of a property
 * (string, number, boolean, array, object, etc.) along with its constraints.
 * Use the [required] flag to mark properties as required in the schema.
 *
 * ## Supported Property Types
 * - [string] - String properties with format, pattern, length constraints
 * - [integer] - Integer numeric properties
 * - [number] - Numeric properties (including decimals)
 * - [boolean] - Boolean properties
 * - [array] - Array properties with item type definitions
 * - [obj] - Nested object properties
 * - [reference] - Schema references ($ref)
 *
 * ## Example
 * ```kotlin
 * property("status") {
 *     required = true
 *     string {
 *         description = "Current status"
 *         enum = listOf("active", "inactive", "pending")
 *     }
 * }
 * ```
 *
 * @see JsonSchemaDefinitionBuilder.property
 */
@JsonSchemaDsl
public class PropertyBuilder {
    /**
     * Marks this property as required in the parent schema.
     *
     * When set to true, the property name will be included in the parent's
     * "required" array. This should be set before defining the property type.
     *
     * ## Example
     * ```kotlin
     * property("email") {
     *     required = true
     *     string { format = "email" }
     * }
     * ```
     */
    public var required: Boolean = false

    /**
     * Creates a string property definition.
     *
     * ## Example
     * ```kotlin
     * property("email") {
     *     string {
     *         description = "Email address"
     *         format = "email"
     *         minLength = 5
     *         maxLength = 100
     *     }
     * }
     * ```
     *
     * @param block Configuration for the string property
     * @return A configured [StringPropertyDefinition]
     * @see StringPropertyBuilder
     */
    public fun string(block: StringPropertyBuilder.() -> Unit = {}): StringPropertyDefinition =
        StringPropertyBuilder().apply(block).build()

    /**
     * Creates an integer property definition.
     *
     * ## Example
     * ```kotlin
     * property("age") {
     *     integer {
     *         description = "Person's age"
     *         minimum = 0.0
     *         maximum = 150.0
     *     }
     * }
     * ```
     *
     * @param block Configuration for the integer property
     * @return A configured [NumericPropertyDefinition] with type "integer"
     * @see NumericPropertyBuilder
     */
    public fun integer(block: NumericPropertyBuilder.() -> Unit = {}): NumericPropertyDefinition =
        NumericPropertyBuilder(type = "integer").apply(block).build()

    /**
     * Creates a numeric property definition (supports decimals).
     *
     * ## Example
     * ```kotlin
     * property("score") {
     *     number {
     *         description = "User score"
     *         minimum = 0.0
     *         maximum = 100.0
     *         multipleOf = 0.5
     *     }
     * }
     * ```
     *
     * @param block Configuration for the number property
     * @return A configured [NumericPropertyDefinition] with type "number"
     * @see NumericPropertyBuilder
     */
    public fun number(block: NumericPropertyBuilder.() -> Unit = {}): NumericPropertyDefinition =
        NumericPropertyBuilder(type = "number").apply(block).build()

    /**
     * Creates a boolean property definition.
     *
     * ## Example
     * ```kotlin
     * property("enabled") {
     *     boolean {
     *         description = "Feature enabled"
     *         default = true
     *     }
     * }
     * ```
     *
     * @param block Configuration for the boolean property
     * @return A configured [BooleanPropertyDefinition]
     * @see BooleanPropertyBuilder
     */
    public fun boolean(block: BooleanPropertyBuilder.() -> Unit = {}): BooleanPropertyDefinition =
        BooleanPropertyBuilder().apply(block).build()

    /**
     * Creates an array property definition.
     *
     * ## Example
     * ```kotlin
     * property("tags") {
     *     array {
     *         description = "List of tags"
     *         minItems = 1
     *         maxItems = 10
     *         ofString()
     *     }
     * }
     * ```
     *
     * @param block Configuration for the array property
     * @return A configured [ArrayPropertyDefinition]
     * @see ArrayPropertyBuilder
     */
    public fun array(block: ArrayPropertyBuilder.() -> Unit = {}): ArrayPropertyDefinition =
        ArrayPropertyBuilder().apply(block).build()

    /**
     * Creates a nested object property definition.
     *
     * ## Example
     * ```kotlin
     * property("metadata") {
     *     obj {
     *         description = "User metadata"
     *         property("createdAt") {
     *             required = true
     *             string { format = "date-time" }
     *         }
     *     }
     * }
     * ```
     *
     * @param block Configuration for the object property
     * @return A configured [ObjectPropertyDefinition]
     * @see ObjectPropertyBuilder
     */
    public fun obj(block: ObjectPropertyBuilder.() -> Unit = {}): ObjectPropertyDefinition =
        ObjectPropertyBuilder().apply(block).build()

    /**
     * Creates a reference to another schema definition.
     *
     * ## Example
     * ```kotlin
     * property("address") {
     *     reference("#/definitions/Address")
     * }
     * ```
     *
     * @param ref The reference URI (e.g., "#/definitions/Address")
     * @return A configured [ReferencePropertyDefinition]
     */
    public fun reference(ref: String): ReferencePropertyDefinition = ReferencePropertyDefinition(ref)
}

/**
 * Builder for [StringPropertyDefinition].
 *
 * Configures string-type properties with various constraints like format,
 * length, pattern matching, and enumeration. Supports automatic type conversion
 * for default and constant values.
 *
 * This class is part of the JSON Schema DSL and cannot be instantiated directly.
 * Use [PropertyBuilder.string] instead within the DSL context.
 *
 * ## Basic String Property
 * ```kotlin
 * property("email") {
 *     string {
 *         description = "Email address"
 *         format = "email"
 *     }
 * }
 * ```
 *
 * ## String with Length Constraints
 * ```kotlin
 * property("username") {
 *     string {
 *         minLength = 3
 *         maxLength = 20
 *         pattern = "^[a-zA-Z0-9_]+$"
 *     }
 * }
 * ```
 *
 * ## String Enum
 * ```kotlin
 * property("status") {
 *     string {
 *         description = "Current status"
 *         enum = listOf("active", "inactive", "pending")
 *     }
 * }
 * ```
 *
 * ## String with Default Value
 * ```kotlin
 * property("name") {
 *     string {
 *         description = "Config name"
 *         default = "default"
 *     }
 * }
 * ```
 *
 * ## String with Constant Value
 * ```kotlin
 * property("version") {
 *     string {
 *         description = "API version"
 *         constValue = "v1.0"
 *     }
 * }
 * ```
 *
 * @see PropertyBuilder.string
 */
@JsonSchemaDsl
public class StringPropertyBuilder internal constructor() {
    /**
     * The JSON Schema type. Always ["string"] for this builder.
     */
    public var type: List<String> = listOf("string")

    /**
     * Human-readable description of this property.
     */
    public var description: String? = null

    /**
     * Whether null values are allowed.
     */
    public var nullable: Boolean? = null

    /**
     * Format specification (e.g., "email", "uri", "date-time", "uuid").
     */
    public var format: String? = null

    /**
     * List of allowed string values (enumeration).
     */
    public var enum: List<String>? = null

    /**
     * Minimum string length constraint.
     */
    public var minLength: Int? = null

    /**
     * Maximum string length constraint.
     */
    public var maxLength: Int? = null

    /**
     * Regular expression pattern the string must match.
     */
    public var pattern: String? = null

    private var _default: JsonElement? = null
    private var _constValue: JsonElement? = null

    /**
     * Default value for this property.
     *
     * Accepts String, JsonElement, or null. String values are automatically
     * converted to JsonPrimitive. Throws IllegalStateException for invalid types.
     *
     * ## Example
     * ```kotlin
     * string {
     *     default = "default value"
     * }
     * ```
     */
    public var default: Any?
        get() = _default
        set(value) {
            _default =
                when (value) {
                    is JsonElement -> value
                    is String -> JsonPrimitive(value)
                    null -> null
                    else ->
                        error(
                            "String property default must be String, JsonElement, or null, " +
                                "but got: ${value::class.simpleName}",
                        )
                }
        }

    /**
     * Constant value for this property.
     *
     * Accepts String, JsonElement, or null. String values are automatically
     * converted to JsonPrimitive. Throws IllegalStateException for invalid types.
     *
     * ## Example
     * ```kotlin
     * string {
     *     constValue = "v1.0"
     * }
     * ```
     */
    public var constValue: Any?
        get() = _constValue
        set(value) {
            _constValue =
                when (value) {
                    is JsonElement -> value
                    is String -> JsonPrimitive(value)
                    null -> null
                    else ->
                        error(
                            "String property constValue must be String, JsonElement, or null, " +
                                "but got: ${value::class.simpleName}",
                        )
                }
        }

    public fun build(): StringPropertyDefinition =
        StringPropertyDefinition(
            type = type,
            description = description,
            nullable = nullable,
            format = format,
            enum = enum,
            minLength = minLength,
            maxLength = maxLength,
            pattern = pattern,
            default = _default,
            constValue = _constValue,
        )
}

/**
 * Builder for [NumericPropertyDefinition].
 *
 * Configures numeric properties (integer or number type) with range constraints,
 * multiple-of validation, and default values. Supports automatic type conversion
 * for all numeric types (Int, Long, Double, Float, etc.).
 *
 * This class is part of the JSON Schema DSL and cannot be instantiated directly.
 * Use [PropertyBuilder.integer] or [PropertyBuilder.number] instead within the DSL context.
 *
 * ## Integer Property
 * ```kotlin
 * property("count") {
 *     integer {
 *         description = "Item count"
 *         minimum = 0.0
 *         default = 10
 *     }
 * }
 * ```
 *
 * ## Number Property with Constraints
 * ```kotlin
 * property("score") {
 *     number {
 *         description = "User score"
 *         minimum = 0.0
 *         maximum = 100.0
 *         multipleOf = 0.5
 *     }
 * }
 * ```
 *
 * @see PropertyBuilder.integer
 * @see PropertyBuilder.number
 */
@JsonSchemaDsl
public class NumericPropertyBuilder internal constructor(
    type: String = "number",
) {
    /**
     * The JSON Schema type. Either ["integer"] or ["number"].
     */
    public var type: List<String> = listOf(type)

    /**
     * Human-readable description of this property.
     */
    public var description: String? = null

    /**
     * Whether null values are allowed.
     */
    public var nullable: Boolean? = null

    /**
     * Value must be a multiple of this number.
     */
    public var multipleOf: Double? = null

    /**
     * Minimum value (inclusive).
     */
    public var minimum: Double? = null

    /**
     * Minimum value (exclusive).
     */
    public var exclusiveMinimum: Double? = null

    /**
     * Maximum value (inclusive).
     */
    public var maximum: Double? = null

    /**
     * Maximum value (exclusive).
     */
    public var exclusiveMaximum: Double? = null

    private var _default: JsonElement? = null
    private var _constValue: JsonElement? = null

    /**
     * Default value for this property.
     *
     * Accepts Number (`Int`, `Long`, `Double`, etc.), JsonElement, or null.
     * Numeric values are automatically converted to JsonPrimitive.
     */
    public var default: Any?
        get() = _default
        set(value) {
            _default =
                when (value) {
                    is JsonElement -> value
                    is Number -> JsonPrimitive(value)
                    null -> null
                    else ->
                        error(
                            "Numeric property default must be Number, JsonElement, or null, " +
                                "but got: ${value::class.simpleName}",
                        )
                }
        }

    /**
     * Constant value for this property.
     *
     * Accepts Number (Int, Long, Double, etc.), JsonElement, or null.
     * Numeric values are automatically converted to JsonPrimitive.
     */
    public var constValue: Any?
        get() = _constValue
        set(value) {
            _constValue =
                when (value) {
                    is JsonElement -> value
                    is Number -> JsonPrimitive(value)
                    null -> null
                    else ->
                        error(
                            "Numeric property constValue must be Number, JsonElement, or null, " +
                                "but got: ${value::class.simpleName}",
                        )
                }
        }

    public fun build(): NumericPropertyDefinition =
        NumericPropertyDefinition(
            type = type,
            description = description,
            nullable = nullable,
            multipleOf = multipleOf,
            minimum = minimum,
            exclusiveMinimum = exclusiveMinimum,
            maximum = maximum,
            exclusiveMaximum = exclusiveMaximum,
            default = _default,
            constValue = _constValue,
        )
}

/**
 * Builder for [BooleanPropertyDefinition].
 *
 * Configures boolean-type properties with default and constant values.
 * Supports automatic type conversion for boolean values.
 *
 * This class is part of the JSON Schema DSL and cannot be instantiated directly.
 * Use [PropertyBuilder.boolean] instead within the DSL context.
 *
 * ## Boolean Property with Default
 * ```kotlin
 * property("enabled") {
 *     boolean {
 *         description = "Feature enabled"
 *         default = true
 *     }
 * }
 * ```
 *
 * @see PropertyBuilder.boolean
 */
@JsonSchemaDsl
public class BooleanPropertyBuilder internal constructor() {
    /**
     * The JSON Schema type. Always ["boolean"] for this builder.
     */
    public var type: List<String> = listOf("boolean")

    /**
     * Human-readable description of this property.
     */
    public var description: String? = null

    /**
     * Whether null values are allowed.
     */
    public var nullable: Boolean? = null

    private var _default: JsonElement? = null
    private var _constValue: JsonElement? = null

    /**
     * Default value for this property.
     *
     * Accepts Boolean, JsonElement, or null. Boolean values are automatically
     * converted to JsonPrimitive.
     */
    public var default: Any?
        get() = _default
        set(value) {
            _default =
                when (value) {
                    is JsonElement -> value
                    is Boolean -> JsonPrimitive(value)
                    null -> null
                    else ->
                        error(
                            "Boolean property default must be Boolean, JsonElement, or null, " +
                                "but got: ${value::class.simpleName}",
                        )
                }
        }

    /**
     * Constant value for this property.
     *
     * Accepts Boolean, JsonElement, or null. Boolean values are automatically
     * converted to JsonPrimitive.
     */
    public var constValue: Any?
        get() = _constValue
        set(value) {
            _constValue =
                when (value) {
                    is JsonElement -> value
                    is Boolean -> JsonPrimitive(value)
                    null -> null
                    else ->
                        error(
                            "Boolean property constValue must be Boolean, JsonElement, or null, " +
                                "but got: ${value::class.simpleName}",
                        )
                }
        }

    public fun build(): BooleanPropertyDefinition =
        BooleanPropertyDefinition(
            type = type,
            description = description,
            nullable = nullable,
            default = _default,
            constValue = _constValue,
        )
}

/**
 * Builder for [ArrayPropertyDefinition].
 *
 * Configures array-type properties with item type definitions and size constraints.
 * Provides convenient methods for specifying item types using `ofString()`, `ofObject()`, etc.
 * Supports automatic conversion of List values to JsonArray.
 *
 * This class is part of the JSON Schema DSL and cannot be instantiated directly.
 * Use [PropertyBuilder.array] instead within the DSL context.
 *
 * ## Array of Strings
 * ```kotlin
 * property("tags") {
 *     array {
 *         description = "List of tags"
 *         minItems = 1
 *         maxItems = 10
 *         ofString()
 *     }
 * }
 * ```
 *
 * ## Array of Objects
 * ```kotlin
 * property("steps") {
 *     array {
 *         description = "Processing steps"
 *         ofObject {
 *             additionalProperties = false
 *             property("explanation") {
 *                 required = true
 *                 string {
 *                     description = "Step explanation"
 *                 }
 *             }
 *             property("output") {
 *                 required = true
 *                 string {
 *                     description = "Step output"
 *                 }
 *             }
 *         }
 *     }
 * }
 * ```
 *
 * @see PropertyBuilder.array
 */
@JsonSchemaDsl
public class ArrayPropertyBuilder internal constructor() {
    /**
     * The JSON Schema type. Always ["array"] for this builder.
     */
    public var type: List<String> = listOf("array")

    /**
     * Human-readable description of this property.
     */
    public var description: String? = null

    /**
     * Whether null values are allowed.
     */
    public var nullable: Boolean? = null

    /**
     * Minimum number of items in the array.
     */
    public var minItems: Int? = null

    /**
     * Maximum number of items in the array.
     */
    public var maxItems: Int? = null

    private var itemsDefinition: PropertyDefinition? = null

    private var _default: JsonElement? = null

    /**
     * Default value for this property.
     *
     * Accepts List<*>, JsonElement, or null. List values are automatically converted to JsonArray
     * with items converted as follows:
     * - String → JsonPrimitive
     * - Number → JsonPrimitive
     * - Boolean → JsonPrimitive
     * - null → JsonNull
     * - JsonElement → used as-is
     *
     * Throws IllegalStateException for unsupported item types or non-List values.
     *
     * ## Example
     * ```kotlin
     * array {
     *     default = listOf("a", "b", "c")
     * }
     * ```
     */
    @OptIn(ExperimentalSerializationApi::class)
    public var default: Any?
        get() = _default
        set(value) {
            _default =
                when (value) {
                    is JsonElement -> value
                    is List<*> ->
                        JsonArray(
                            value.map { item ->
                                when (item) {
                                    is JsonElement -> item
                                    is String -> JsonPrimitive(item)
                                    is Number -> JsonPrimitive(item)
                                    is Boolean -> JsonPrimitive(item)
                                    null -> JsonNull
                                    else ->
                                        error(
                                            "Array property default list item must be JsonElement, String, Number, " +
                                                "Boolean, or null, but got: ${item::class.simpleName}",
                                        )
                                }
                            },
                        )

                    null -> null
                    else ->
                        error(
                            "Array property default must be List, JsonElement, or null, " +
                                "but got: ${value::class.simpleName}",
                        )
                }
        }

    /**
     * Defines the type of items in the array using a generic property builder.
     * Consider using the `of*()` convenience methods instead.
     */
    public fun items(block: PropertyBuilder.() -> PropertyDefinition) {
        itemsDefinition = PropertyBuilder().block()
    }

    /**
     * Specifies that array items are strings.
     *
     * ## Example
     * ```kotlin
     * array {
     *     ofString {
     *         minLength = 1
     *     }
     * }
     * ```
     */
    public fun ofString(block: StringPropertyBuilder.() -> Unit = {}) {
        itemsDefinition = StringPropertyBuilder().apply(block).build()
    }

    /**
     * Specifies that array items are integers.
     */
    public fun ofInteger(block: NumericPropertyBuilder.() -> Unit = {}) {
        itemsDefinition = NumericPropertyBuilder(type = "integer").apply(block).build()
    }

    /**
     * Specifies that array items are numbers (supports decimals).
     */
    public fun ofNumber(block: NumericPropertyBuilder.() -> Unit = {}) {
        itemsDefinition = NumericPropertyBuilder(type = "number").apply(block).build()
    }

    /**
     * Specifies that array items are booleans.
     */
    public fun ofBoolean(block: BooleanPropertyBuilder.() -> Unit = {}) {
        itemsDefinition = BooleanPropertyBuilder().apply(block).build()
    }

    /**
     * Specifies that array items are arrays (nested arrays).
     */
    public fun ofArray(block: ArrayPropertyBuilder.() -> Unit = {}) {
        itemsDefinition = ArrayPropertyBuilder().apply(block).build()
    }

    /**
     * Specifies that array items are objects.
     *
     * ## Example
     * ```kotlin
     * array {
     *     ofObject {
     *         property("name") {
     *             required = true
     *             string()
     *         }
     *     }
     * }
     * ```
     */
    public fun ofObject(block: ObjectPropertyBuilder.() -> Unit = {}) {
        itemsDefinition = ObjectPropertyBuilder().apply(block).build()
    }

    /**
     * Specifies that array items reference another schema definition.
     *
     * @param ref The reference URI (e.g., "#/definitions/Item")
     */
    public fun ofReference(ref: String) {
        itemsDefinition = ReferencePropertyDefinition(ref)
    }

    public fun build(): ArrayPropertyDefinition =
        ArrayPropertyDefinition(
            type = type,
            description = description,
            nullable = nullable,
            items = itemsDefinition,
            minItems = minItems?.toUInt(),
            maxItems = maxItems?.toUInt(),
            default = _default,
        )
}

/**
 * Builder for [ObjectPropertyDefinition].
 *
 * Configures nested object properties with their own property definitions,
 * constraints, and validation rules. Supports automatic conversion of Map values to JsonObject.
 *
 * This class is part of the JSON Schema DSL and cannot be instantiated directly.
 * Use [PropertyBuilder.obj] instead within the DSL context.
 *
 * ## Nested Object
 * ```kotlin
 * property("metadata") {
 *     obj {
 *         description = "User metadata"
 *         property("createdAt") {
 *             required = true
 *             string {
 *                 format = "date-time"
 *             }
 *         }
 *         property("updatedAt") {
 *             string {
 *                 format = "date-time"
 *             }
 *         }
 *     }
 * }
 * ```
 *
 * ## Object with additionalProperties Constraint
 * ```kotlin
 * property("config") {
 *     obj {
 *         additionalProperties = false
 *         property("enabled") {
 *             required = true
 *             boolean()
 *         }
 *     }
 * }
 * ```
 *
 * @see PropertyBuilder.obj
 */
@JsonSchemaDsl
public class ObjectPropertyBuilder internal constructor() {
    /**
     * The JSON Schema type. Always ["object"] for this builder.
     */
    public var type: List<String> = listOf("object")

    /**
     * Human-readable description of this property.
     */
    public var description: String? = null

    /**
     * Whether null values are allowed.
     */
    public var nullable: Boolean? = null

    /**
     * Whether additional properties beyond those defined are allowed.
     * - `true`: Additional properties allowed
     * - `false`: Only defined properties allowed
     * - `null`: No constraint (default)
     */
    public var additionalProperties: Boolean? = null

    private val properties: MutableMap<String, PropertyDefinition> = mutableMapOf()
    private val requiredFields: MutableSet<String> = mutableSetOf()

    private var _default: JsonElement? = null

    /**
     * Default value for this property.
     *
     * Accepts Map<*, *>, JsonElement, or null. Map keys are converted to strings and values
     * are converted as follows:
     * - String → JsonPrimitive
     * - Number → JsonPrimitive
     * - Boolean → JsonPrimitive
     * - null → JsonNull
     * - JsonElement → used as-is
     *
     * Throws IllegalStateException for unsupported value types or non-Map values.
     *
     * ## Example
     * ```kotlin
     * obj {
     *     default = mapOf("key" to "value", "count" to 42)
     * }
     * ```
     */
    @OptIn(ExperimentalSerializationApi::class)
    public var default: Any?
        get() = _default
        set(value) {
            _default =
                when (value) {
                    is JsonElement -> value
                    is Map<*, *> ->
                        JsonObject(
                            value.mapKeys { it.key.toString() }.mapValues { (_, v) ->
                                when (v) {
                                    is JsonElement -> v
                                    is String -> JsonPrimitive(v)
                                    is Number -> JsonPrimitive(v)
                                    is Boolean -> JsonPrimitive(v)
                                    null -> JsonNull
                                    else ->
                                        error(
                                            "Object property default map value must be JsonElement, String, Number, " +
                                                "Boolean, or null, but got: ${v::class.simpleName}",
                                        )
                                }
                            },
                        )

                    null -> null
                    else ->
                        error(
                            "Object property default must be Map, JsonElement, or null, " +
                                "but got: ${value::class.simpleName}",
                        )
                }
        }

    /**
     * Defines a property within this nested object.
     *
     * Works the same as [JsonSchemaDefinitionBuilder.property], allowing you to
     * define nested properties with their types and constraints.
     *
     * ## Example
     * ```kotlin
     * obj {
     *     property("createdAt") {
     *         required = true
     *         string { format = "date-time" }
     *     }
     * }
     * ```
     *
     * @param name The property name
     * @param block Configuration block for the property
     * @see JsonSchemaDefinitionBuilder.property
     */
    public fun property(
        name: String,
        block: PropertyBuilder.() -> PropertyDefinition,
    ) {
        val builder = PropertyBuilder()
        properties[name] = builder.block()
        if (builder.required) {
            requiredFields.add(name)
        }
    }

    public fun build(): ObjectPropertyDefinition =
        ObjectPropertyDefinition(
            type = type,
            description = description,
            nullable = nullable,
            properties = properties.ifEmpty { null },
            required = if (requiredFields.isEmpty()) null else requiredFields.toList(),
            additionalProperties = additionalProperties,
            default = _default,
        )
}
