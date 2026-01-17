@file:Suppress("TooManyFunctions")

package kotlinx.schema.json

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull

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
        val schema = requireNotNull(schemaDefinition) { "Schema definition must be provided" }
        return JsonSchema(
            name = name,
            strict = strict,
            description = description,
            schema = schema,
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
 * import kotlinx.serialization.json.JsonPrimitive
 *
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

    private var _additionalProperties: JsonElement? = null

    /**
     * Whether additional properties beyond those defined are allowed.
     * - `JsonPrimitive(true)`: Additional properties allowed
     * - `JsonPrimitive(false)`: Only defined properties allowed
     * - `JsonObject`: Schema for additional properties (e.g., for maps)
     * - `null`: No constraint (default)
     */
    public var additionalProperties: Any?
        get() = _additionalProperties
        set(value) {
            _additionalProperties =
                when (value) {
                    is JsonObject -> {
                        value
                    }

                    is Boolean -> {
                        JsonPrimitive(value)
                    }

                    null -> {
                        null
                    }

                    else -> {
                        error(
                            "additionalProperties  must be Boolean, JsonElement, or null, " +
                                "but got: ${value::class.simpleName}",
                        )
                    }
                }
        }

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
            additionalProperties = _additionalProperties,
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

    /**
     * Creates a oneOf property definition.
     *
     * Validates that the value matches exactly one of the provided schemas.
     * Useful for polymorphic types with mutually exclusive alternatives.
     *
     * ## Example - Simple oneOf
     * ```kotlin
     * property("payment") {
     *     oneOf {
     *         obj {
     *             property("type") {
     *                 required = true
     *                 string { constValue = "credit_card" }
     *             }
     *             property("cardNumber") {
     *                 required = true
     *                 string()
     *             }
     *         }
     *         obj {
     *             property("type") {
     *                 required = true
     *                 string { constValue = "paypal" }
     *             }
     *             property("email") {
     *                 required = true
     *                 string { format = "email" }
     *             }
     *         }
     *     }
     * }
     * ```
     *
     * ## Example - oneOf with Discriminator
     * ```kotlin
     * property("shape") {
     *     oneOf {
     *         description = "A geometric shape"
     *         discriminator("type") {
     *             mapping = mapOf(
     *                 "circle" to "#/definitions/Circle",
     *                 "rectangle" to "#/definitions/Rectangle"
     *             )
     *         }
     *         reference("#/definitions/Circle")
     *         reference("#/definitions/Rectangle")
     *     }
     * }
     * ```
     *
     * @param block Configuration for the oneOf property
     * @return A configured [OneOfPropertyDefinition]
     * @see OneOfPropertyBuilder
     */
    public fun oneOf(block: OneOfPropertyBuilder.() -> Unit = {}): OneOfPropertyDefinition =
        OneOfPropertyBuilder().apply(block).build()

    /**
     * Creates an anyOf property definition.
     *
     * Validates that the value matches one or more of the provided schemas.
     * Unlike oneOf, the value can match multiple schemas simultaneously.
     *
     * ## Example
     * ```kotlin
     * property("value") {
     *     anyOf {
     *         description = "A value that can be a string or number"
     *         string { minLength = 1 }
     *         number { minimum = 0.0 }
     *     }
     * }
     * ```
     *
     * @param block Configuration for the anyOf property
     * @return A configured [AnyOfPropertyDefinition]
     * @see AnyOfPropertyBuilder
     */
    public fun anyOf(block: AnyOfPropertyBuilder.() -> Unit = {}): AnyOfPropertyDefinition =
        AnyOfPropertyBuilder().apply(block).build()

    /**
     * Creates an allOf property definition.
     *
     * Validates that the value matches all of the provided schemas.
     * Commonly used for schema composition and inheritance.
     *
     * ## Example
     * ```kotlin
     * property("extendedUser") {
     *     allOf {
     *         description = "User with additional properties"
     *         reference("#/definitions/BaseUser")
     *         obj {
     *             property("role") {
     *                 required = true
     *                 string { enum = listOf("admin", "user") }
     *             }
     *         }
     *     }
     * }
     * ```
     *
     * @param block Configuration for the allOf property
     * @return A configured [AllOfPropertyDefinition]
     * @see AllOfPropertyBuilder
     */
    public fun allOf(block: AllOfPropertyBuilder.() -> Unit = {}): AllOfPropertyDefinition =
        AllOfPropertyBuilder().apply(block).build()
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

    private var _enum: List<JsonElement>? = null

    /**
     * List of allowed string values (enumeration).
     *
     * Accepts List<String> or null. String values are automatically converted to JsonPrimitive elements.
     * Throws IllegalArgumentException for non-string types to prevent semantically invalid schemas.
     *
     * ## Example
     * ```kotlin
     * string {
     *     enum = listOf("active", "inactive", "pending")
     * }
     * ```
     */
    public var enum: Any?
        get() = _enum
        set(value) {
            _enum =
                when (value) {
                    is List<*> -> {
                        value.map { item ->
                            when (item) {
                                is String -> {
                                    JsonPrimitive(item)
                                }

                                is JsonPrimitive -> {
                                    // Validate that JsonPrimitive contains a string
                                    require(item.isString) {
                                        "String property enum must contain only string values, " +
                                            "but got JsonPrimitive with non-string content"
                                    }
                                    item
                                }

                                null -> {
                                    JsonNull
                                }

                                else -> {
                                    throw IllegalArgumentException(
                                        "String property enum must contain only String values or null, " +
                                            "but got: ${item::class.simpleName}",
                                    )
                                }
                            }
                        }
                    }

                    null -> {
                        null
                    }

                    else -> {
                        throw IllegalArgumentException(
                            "Enum must be a List or null, but got: ${value::class.simpleName}",
                        )
                    }
                }
        }

    /**
     * Sets enum values from strings.
     * Convenience function for the common case of string enums.
     */
    public fun stringEnum(vararg values: String) {
        enum = values.toList()
    }

    /**
     * Sets enum values from any JSON-compatible values.
     * Supports numbers, booleans, strings, and null.
     */
    public fun mixedEnum(vararg values: Any?) {
        enum = values.toList()
    }

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
                    is JsonElement -> {
                        value
                    }

                    is String -> {
                        JsonPrimitive(value)
                    }

                    null -> {
                        null
                    }

                    else -> {
                        error(
                            "String property default must be String, JsonElement, or null, " +
                                "but got: ${value::class.simpleName}",
                        )
                    }
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
                    is JsonElement -> {
                        value
                    }

                    is String -> {
                        JsonPrimitive(value)
                    }

                    null -> {
                        null
                    }

                    else -> {
                        error(
                            "String property constValue must be String, JsonElement, or null, " +
                                "but got: ${value::class.simpleName}",
                        )
                    }
                }
        }

    public fun build(): StringPropertyDefinition =
        StringPropertyDefinition(
            type = type,
            description = description,
            nullable = nullable,
            format = format,
            enum = _enum,
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

    private var _enum: List<JsonElement>? = null

    /**
     * List of allowed numeric values (enumeration).
     *
     * Accepts List<Number> or null. Numeric values are automatically converted to JsonPrimitive elements.
     * Throws IllegalArgumentException for non-numeric types to prevent semantically invalid schemas.
     *
     * ## Example
     * ```kotlin
     * number {
     *     enum = listOf(1, 2, 3, 5, 8, 13)
     * }
     * integer {
     *     enum = listOf(0, 1)
     * }
     * ```
     */
    public var enum: Any?
        get() = _enum
        set(value) {
            _enum =
                when (value) {
                    is List<*> -> {
                        value.map { item ->
                            when (item) {
                                is Number -> {
                                    JsonPrimitive(item)
                                }

                                is JsonPrimitive -> {
                                    // Validate that JsonPrimitive contains a number
                                    val content = item.content
                                    require(content.toDoubleOrNull() != null || content.toIntOrNull() != null) {
                                        "Numeric property enum must contain only numeric values, " +
                                            "but got JsonPrimitive with non-numeric content: $content"
                                    }
                                    item
                                }

                                null -> {
                                    JsonNull
                                }

                                else -> {
                                    throw IllegalArgumentException(
                                        "Numeric property enum must contain only Number values or null, " +
                                            "but got: ${item::class.simpleName}",
                                    )
                                }
                            }
                        }
                    }

                    null -> {
                        null
                    }

                    else -> {
                        throw IllegalArgumentException(
                            "Enum must be a List or null, but got: ${value::class.simpleName}",
                        )
                    }
                }
        }

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
                    is JsonElement -> {
                        value
                    }

                    is Number -> {
                        JsonPrimitive(value)
                    }

                    null -> {
                        null
                    }

                    else -> {
                        error(
                            "Numeric property default must be Number, JsonElement, or null, " +
                                "but got: ${value::class.simpleName}",
                        )
                    }
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
                    is JsonElement -> {
                        value
                    }

                    is Number -> {
                        JsonPrimitive(value)
                    }

                    null -> {
                        null
                    }

                    else -> {
                        error(
                            "Numeric property constValue must be Number, JsonElement, or null, " +
                                "but got: ${value::class.simpleName}",
                        )
                    }
                }
        }

    public fun build(): NumericPropertyDefinition =
        NumericPropertyDefinition(
            type = type,
            description = description,
            nullable = nullable,
            enum = _enum,
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

    private var _enum: List<JsonElement>? = null

    /**
     * List of allowed boolean values (enumeration).
     *
     * Accepts List<Boolean> or null. Boolean values are automatically converted to JsonPrimitive elements.
     * Throws IllegalArgumentException for non-boolean types to prevent semantically invalid schemas.
     *
     * ## Example
     * ```kotlin
     * boolean {
     *     enum = listOf(true, false)
     * }
     * ```
     */
    public var enum: Any?
        get() = _enum
        set(value) {
            _enum =
                when (value) {
                    is List<*> -> {
                        value.map { item ->
                            when (item) {
                                is Boolean -> {
                                    JsonPrimitive(item)
                                }

                                is JsonPrimitive -> {
                                    // Validate that JsonPrimitive contains a boolean
                                    require(
                                        item.booleanOrNull != null &&
                                            (item.content == "true" || item.content == "false"),
                                    ) {
                                        "Boolean property enum must contain only boolean values, " +
                                            "but got JsonPrimitive with non-boolean content: ${item.content}"
                                    }
                                    item
                                }

                                null -> {
                                    JsonNull
                                }

                                else -> {
                                    throw IllegalArgumentException(
                                        "Boolean property enum must contain only Boolean values or null, " +
                                            "but got: ${item::class.simpleName}",
                                    )
                                }
                            }
                        }
                    }

                    null -> {
                        null
                    }

                    else -> {
                        throw IllegalArgumentException(
                            "Enum must be a List or null, but got: ${value::class.simpleName}",
                        )
                    }
                }
        }

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
                    is JsonElement -> {
                        value
                    }

                    is Boolean -> {
                        JsonPrimitive(value)
                    }

                    null -> {
                        null
                    }

                    else -> {
                        error(
                            "Boolean property default must be Boolean, JsonElement, or null, " +
                                "but got: ${value::class.simpleName}",
                        )
                    }
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
                    is JsonElement -> {
                        value
                    }

                    is Boolean -> {
                        JsonPrimitive(value)
                    }

                    null -> {
                        null
                    }

                    else -> {
                        error(
                            "Boolean property constValue must be Boolean, JsonElement, or null, " +
                                "but got: ${value::class.simpleName}",
                        )
                    }
                }
        }

    public fun build(): BooleanPropertyDefinition =
        BooleanPropertyDefinition(
            type = type,
            description = description,
            nullable = nullable,
            enum = _enum,
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

    private var _enum: List<JsonElement>? = null

    /**
     * List of allowed array values (enumeration).
     *
     * Accepts List<JsonArray> or null. Only specific array values are allowed.
     * Throws IllegalArgumentException for non-array types to prevent semantically invalid schemas.
     *
     * This is less common but valid for schemas that allow only specific array instances.
     *
     * ## Example
     * ```kotlin
     * array {
     *     description = "Allowed coordinate pairs"
     *     enum = listOf(
     *         JsonArray(listOf(JsonPrimitive(0), JsonPrimitive(0))),
     *         JsonArray(listOf(JsonPrimitive(1), JsonPrimitive(1)))
     *     )
     * }
     * ```
     */
    public var enum: Any?
        get() = _enum
        set(value) {
            _enum =
                when (value) {
                    is List<*> -> {
                        value.map { item ->
                            when (item) {
                                is JsonArray -> item

                                null -> JsonNull

                                else -> throw IllegalArgumentException(
                                    "Array property enum must contain only JsonArray values or null, " +
                                        "but got: ${item::class.simpleName}",
                                )
                            }
                        }
                    }

                    null -> {
                        null
                    }

                    else -> {
                        throw IllegalArgumentException(
                            "Enum must be a List or null, but got: ${value::class.simpleName}",
                        )
                    }
                }
        }

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
                    is JsonElement -> {
                        value
                    }

                    is List<*> -> {
                        JsonArray(
                            value.map { item ->
                                when (item) {
                                    is JsonElement -> {
                                        item
                                    }

                                    is String -> {
                                        JsonPrimitive(item)
                                    }

                                    is Number -> {
                                        JsonPrimitive(item)
                                    }

                                    is Boolean -> {
                                        JsonPrimitive(item)
                                    }

                                    null -> {
                                        JsonNull
                                    }

                                    else -> {
                                        error(
                                            "Array property default list item must be JsonElement, String, Number, " +
                                                "Boolean, or null, but got: ${item::class.simpleName}",
                                        )
                                    }
                                }
                            },
                        )
                    }

                    null -> {
                        null
                    }

                    else -> {
                        error(
                            "Array property default must be List, JsonElement, or null, " +
                                "but got: ${value::class.simpleName}",
                        )
                    }
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
            enum = _enum,
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

    private var _enum: List<JsonElement>? = null

    /**
     * List of allowed object values (enumeration).
     *
     * Accepts List<JsonObject>, List<Map>, or null. Map values are automatically converted to JsonObject.
     * Throws IllegalArgumentException for non-object types to prevent semantically invalid schemas.
     *
     * This is less common but valid for schemas that allow only specific object instances.
     *
     * ## Example with JsonObject
     * ```kotlin
     * obj {
     *     description = "Allowed configurations"
     *     enum = listOf(
     *         JsonObject(mapOf("mode" to JsonPrimitive("read"))),
     *         JsonObject(mapOf("mode" to JsonPrimitive("write")))
     *     )
     * }
     * ```
     *
     * ## Example with Map (convenience)
     * ```kotlin
     * obj {
     *     description = "Allowed configurations"
     *     enum = listOf(
     *         mapOf("mode" to "read"),
     *         mapOf("mode" to "write")
     *     )
     * }
     * ```
     */
    @OptIn(ExperimentalSerializationApi::class)
    public var enum: Any?
        get() = _enum
        set(value) {
            _enum =
                when (value) {
                    is List<*> -> {
                        value.map { item ->
                            when (item) {
                                is JsonObject -> {
                                    item
                                }

                                is Map<*, *> -> {
                                    // Convert Map to JsonObject for convenience
                                    JsonObject(
                                        item.mapKeys { it.key.toString() }.mapValues { (_, v) ->
                                            when (v) {
                                                is JsonElement -> v

                                                is String -> JsonPrimitive(v)

                                                is Number -> JsonPrimitive(v)

                                                is Boolean -> JsonPrimitive(v)

                                                null -> JsonNull

                                                else -> throw IllegalArgumentException(
                                                    "Object enum map value must be JsonElement, String, Number, " +
                                                        "Boolean, or null, but got: ${v::class.simpleName}",
                                                )
                                            }
                                        },
                                    )
                                }

                                null -> {
                                    JsonNull
                                }

                                else -> {
                                    throw IllegalArgumentException(
                                        "Object property enum must contain only JsonObject, Map, or null values, " +
                                            "but got: ${item::class.simpleName}",
                                    )
                                }
                            }
                        }
                    }

                    null -> {
                        null
                    }

                    else -> {
                        throw IllegalArgumentException(
                            "Enum must be a List or null, but got: ${value::class.simpleName}",
                        )
                    }
                }
        }

    /**
     * Whether additional properties beyond those defined are allowed.
     * - `JsonPrimitive(true)`: Additional properties allowed
     * - `JsonPrimitive(false)`: Only defined properties allowed
     * - `JsonObject`: Schema for additional properties (e.g., for maps)
     * - `null`: No constraint (default)
     */
    public var additionalProperties: JsonElement? = null

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
                    is JsonElement -> {
                        value
                    }

                    is Map<*, *> -> {
                        JsonObject(
                            value.mapKeys { it.key.toString() }.mapValues { (_, v) ->
                                when (v) {
                                    is JsonElement -> {
                                        v
                                    }

                                    is String -> {
                                        JsonPrimitive(v)
                                    }

                                    is Number -> {
                                        JsonPrimitive(v)
                                    }

                                    is Boolean -> {
                                        JsonPrimitive(v)
                                    }

                                    null -> {
                                        JsonNull
                                    }

                                    else -> {
                                        error(
                                            "Object property default map value must be JsonElement, String, Number, " +
                                                "Boolean, or null, but got: ${v::class.simpleName}",
                                        )
                                    }
                                }
                            },
                        )
                    }

                    null -> {
                        null
                    }

                    else -> {
                        error(
                            "Object property default must be Map, JsonElement, or null, " +
                                "but got: ${value::class.simpleName}",
                        )
                    }
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
            enum = _enum,
            properties = properties.ifEmpty { null },
            required = if (requiredFields.isEmpty()) null else requiredFields.toList(),
            additionalProperties = additionalProperties,
            default = _default,
        )
}

/**
 * Common interface for builders that collect polymorphic schema options.
 *
 * Provides methods to add various property types as options in oneOf, anyOf, or allOf compositions.
 * This interface is used internally by polymorphic property builders.
 */
@JsonSchemaDsl
public interface PolymorphicOptionsCollector {
    /**
     * Adds a property definition as an option.
     * @suppress Internal API - use type-specific methods like string(), obj(), etc. instead
     */
    public fun addOption(option: PropertyDefinition)
}

/*
 * Extension functions for [PolymorphicOptionsCollector] that provide type-safe option builders.
 */

/** Adds a string schema option. */
public fun PolymorphicOptionsCollector.string(block: StringPropertyBuilder.() -> Unit = {}) {
    addOption(StringPropertyBuilder().apply(block).build())
}

/** Adds an integer schema option. */
public fun PolymorphicOptionsCollector.integer(block: NumericPropertyBuilder.() -> Unit = {}) {
    addOption(NumericPropertyBuilder(type = "integer").apply(block).build())
}

/** Adds a number schema option. */
public fun PolymorphicOptionsCollector.number(block: NumericPropertyBuilder.() -> Unit = {}) {
    addOption(NumericPropertyBuilder(type = "number").apply(block).build())
}

/** Adds a boolean schema option. */
public fun PolymorphicOptionsCollector.boolean(block: BooleanPropertyBuilder.() -> Unit = {}) {
    addOption(BooleanPropertyBuilder().apply(block).build())
}

/** Adds an array schema option. */
public fun PolymorphicOptionsCollector.array(block: ArrayPropertyBuilder.() -> Unit = {}) {
    addOption(ArrayPropertyBuilder().apply(block).build())
}

/** Adds an object schema option. */
public fun PolymorphicOptionsCollector.obj(block: ObjectPropertyBuilder.() -> Unit = {}) {
    addOption(ObjectPropertyBuilder().apply(block).build())
}

/** Adds a reference schema option. */
public fun PolymorphicOptionsCollector.reference(ref: String) {
    addOption(ReferencePropertyDefinition(ref))
}

/** Adds a nested oneOf schema option. */
public fun PolymorphicOptionsCollector.oneOf(block: OneOfPropertyBuilder.() -> Unit = {}) {
    addOption(OneOfPropertyBuilder().apply(block).build())
}

/** Adds a nested anyOf schema option. */
public fun PolymorphicOptionsCollector.anyOf(block: AnyOfPropertyBuilder.() -> Unit = {}) {
    addOption(AnyOfPropertyBuilder().apply(block).build())
}

/** Adds a nested allOf schema option. */
public fun PolymorphicOptionsCollector.allOf(block: AllOfPropertyBuilder.() -> Unit = {}) {
    addOption(AllOfPropertyBuilder().apply(block).build())
}

/**
 * Builder for [Discriminator].
 *
 * Configures a discriminator for efficient polymorphic type resolution.
 * The discriminator identifies which schema applies based on a property value.
 *
 * This class is part of the JSON Schema DSL and cannot be instantiated directly.
 * Use within [OneOfPropertyBuilder.discriminator] context.
 *
 * ## Example with references
 * ```kotlin
 * oneOf {
 *     discriminator("type") {
 *         "dog" mappedTo "#/definitions/Dog"
 *         "cat" mappedTo "#/definitions/Cat"
 *     }
 *     // References are added automatically
 * }
 * ```
 *
 * ## Example with inline schemas
 * ```kotlin
 * oneOf {
 *     discriminator("paymentType") {
 *         "credit_card" mappedTo {
 *             property("type") { string { constValue = "credit_card" } }
 *             property("cardNumber") { string() }
 *         }
 *         "paypal" mappedTo {
 *             property("type") { string { constValue = "paypal" } }
 *             property("email") { string { format = "email" } }
 *         }
 *     }
 * }
 * ```
 *
 * @see Discriminator
 * @see OneOfPropertyBuilder
 */
@JsonSchemaDsl
public class DiscriminatorBuilder internal constructor(
    private val propertyName: String,
    private val optionsCollector: PolymorphicOptionsCollector,
) {
    private val mappingMap = mutableMapOf<String, String>()

    /**
     * Maps a discriminator value to a schema reference, automatically adding
     * the reference to the parent oneOf options and the explicit mapping.
     *
     * This eliminates duplication - you don't need to specify the reference twice.
     *
     * @receiver The discriminator value (e.g., "dog", "cat")
     * @param ref The schema reference (e.g., "#/definitions/Dog")
     */
    public infix fun String.mappedTo(ref: String) {
        require(ref.isNotEmpty()) { "Schema reference cannot be empty" }
        mappingMap[this] = ref
        optionsCollector.addOption(ReferencePropertyDefinition(ref = ref))
    }

    /**
     * Maps a discriminator value to an inline object schema, automatically adding
     * it to the parent oneOf options.
     *
     * This is a concise form for defining inline objects without the `obj` keyword.
     *
     * @receiver The discriminator value (e.g., "credit_card", "paypal")
     * @param block Builder block for defining object properties
     */
    public infix fun String.mappedTo(block: ObjectPropertyBuilder.() -> Unit) {
        val definition = ObjectPropertyBuilder().apply(block).build()
        optionsCollector.addOption(definition)
        // Inline objects don't have references, so no mapping entry
    }

    internal fun build(): Discriminator =
        Discriminator(
            propertyName = propertyName,
            mapping = mappingMap.takeIf { it.isNotEmpty() },
        )
}

/**
 * Builder for [OneOfPropertyDefinition].
 *
 * Configures oneOf schema composition where exactly one schema must match.
 * Supports optional discriminator for efficient polymorphic type resolution.
 *
 * This class is part of the JSON Schema DSL and cannot be instantiated directly.
 * Use [PropertyBuilder.oneOf] instead within the DSL context.
 *
 * ## Example - Basic oneOf
 * ```kotlin
 * property("contact") {
 *     oneOf {
 *         description = "Email or phone contact"
 *         obj {
 *             property("email") {
 *                 required = true
 *                 string { format = "email" }
 *             }
 *         }
 *         obj {
 *             property("phone") {
 *                 required = true
 *                 string()
 *             }
 *         }
 *     }
 * }
 * ```
 *
 * @see PropertyBuilder.oneOf
 * @see Discriminator
 */
@JsonSchemaDsl
public class OneOfPropertyBuilder internal constructor() : PolymorphicOptionsCollector {
    /**
     * Human-readable description of this property.
     */
    public var description: String? = null

    private val options: MutableList<PropertyDefinition> = mutableListOf()
    private var discriminatorDef: Discriminator? = null

    override fun addOption(option: PropertyDefinition) {
        options.add(option)
    }

    /**
     * Configures the discriminator for this oneOf.
     *
     * The discriminator enables efficient determination of which schema applies
     * based on a property value in the instance data.
     *
     * Use the [DiscriminatorBuilder.mappedTo] infix operator to map discriminator
     * values to references, eliminating duplication.
     *
     * @param propertyName The name of the property that holds the discriminator value.
     *                     Must be non-empty and present in all schemas within the oneOf.
     * @param block Configuration block for discriminator mappings
     *
     * ## Example
     * ```kotlin
     * discriminator("type") {
     *     "dog" mappedTo "#/definitions/Dog"
     *     "cat" mappedTo "#/definitions/Cat"
     * }
     * // No need to add references separately!
     * ```
     */
    public fun discriminator(
        propertyName: String,
        block: DiscriminatorBuilder.() -> Unit = {},
    ) {
        require(propertyName.isNotEmpty()) { "Discriminator propertyName cannot be empty" }
        discriminatorDef = DiscriminatorBuilder(propertyName, this).apply(block).build()
    }

    public fun build(): OneOfPropertyDefinition {
        require(options.size >= 2) {
            "oneOf requires at least 2 options, but got ${options.size}"
        }
        return OneOfPropertyDefinition(
            oneOf = options.toList(),
            discriminator = discriminatorDef,
            description = description,
        )
    }
}

/**
 * Builder for [AnyOfPropertyDefinition].
 *
 * Configures anyOf schema composition where one or more schemas must match.
 * Unlike oneOf, the value can satisfy multiple schemas simultaneously.
 *
 * This class is part of the JSON Schema DSL and cannot be instantiated directly.
 * Use [PropertyBuilder.anyOf] instead within the DSL context.
 *
 * ## Example
 * ```kotlin
 * property("identifier") {
 *     anyOf {
 *         description = "Can be UUID or integer ID"
 *         string {
 *             format = "uuid"
 *             description = "UUID identifier"
 *         }
 *         integer {
 *             minimum = 1.0
 *             description = "Numeric identifier"
 *         }
 *     }
 * }
 * ```
 *
 * @see PropertyBuilder.anyOf
 */
@JsonSchemaDsl
public class AnyOfPropertyBuilder internal constructor() : PolymorphicOptionsCollector {
    /**
     * Human-readable description of this property.
     */
    public var description: String? = null

    private val options: MutableList<PropertyDefinition> = mutableListOf()

    override fun addOption(option: PropertyDefinition) {
        options.add(option)
    }

    public fun build(): AnyOfPropertyDefinition {
        require(options.size >= 2) {
            "anyOf requires at least 2 options, but got ${options.size}"
        }
        return AnyOfPropertyDefinition(
            anyOf = options.toList(),
            description = description,
        )
    }
}

/**
 * Builder for [AllOfPropertyDefinition].
 *
 * Configures allOf schema composition where all schemas must match.
 * Commonly used for schema composition, extension, and inheritance patterns.
 *
 * This class is part of the JSON Schema DSL and cannot be instantiated directly.
 * Use [PropertyBuilder.allOf] instead within the DSL context.
 *
 * ## Example - Schema Extension
 * ```kotlin
 * property("adminUser") {
 *     allOf {
 *         description = "Admin user with base properties plus admin-specific properties"
 *         reference("#/definitions/BaseUser")
 *         obj {
 *             property("permissions") {
 *                 required = true
 *                 array {
 *                     ofString()
 *                 }
 *             }
 *         }
 *     }
 * }
 * ```
 *
 * @see PropertyBuilder.allOf
 */
@JsonSchemaDsl
public class AllOfPropertyBuilder internal constructor() : PolymorphicOptionsCollector {
    /**
     * Human-readable description of this property.
     */
    public var description: String? = null

    private val options: MutableList<PropertyDefinition> = mutableListOf()

    override fun addOption(option: PropertyDefinition) {
        options.add(option)
    }

    public fun build(): AllOfPropertyDefinition {
        require(options.isNotEmpty()) {
            "allOf requires at least 1 schema, but got 0"
        }
        return AllOfPropertyDefinition(
            allOf = options.toList(),
            description = description,
        )
    }
}
