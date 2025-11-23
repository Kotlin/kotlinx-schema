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
 * Example usage:
 * ```kotlin
 * val schema = jsonSchema {
 *     name = "Person"
 *     strict = true
 *     description = "A person schema"
 *     schema {
 *         type = "object"
 *         property("name") {
 *             required = true
 *             string {
 *                 description = "Person's name"
 *                 minLength = 1
 *             }
 *         }
 *         property("age") {
 *             required = true
 *             integer {
 *                 description = "Person's age"
 *                 minimum = 0.0
 *             }
 *         }
 *     }
 * }
 * ```
 */
public fun jsonSchema(block: JsonSchemaBuilder.() -> Unit): JsonSchema = JsonSchemaBuilder().apply(block).build()

/**
 * Builder for [JsonSchema].
 */
@JsonSchemaDsl
public class JsonSchemaBuilder {
    public var name: String = ""
    public var strict: Boolean = false
    public var description: String? = null
    private var schemaDefinition: JsonSchemaDefinition? = null

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
 */
@JsonSchemaDsl
public class JsonSchemaDefinitionBuilder {
    public var id: String? = null
    public var schema: String? = null
    public var additionalProperties: Boolean? = null
    public var description: String? = null
    public var items: ObjectPropertyDefinition? = null
    private val properties: MutableMap<String, PropertyDefinition> = mutableMapOf()
    private val requiredFields: MutableSet<String> = mutableSetOf()

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
 * Builder for property definitions.
 */
@JsonSchemaDsl
public class PropertyBuilder {
    /**
     * Marks this property as required in the parent schema.
     *
     * When set to true, the property name will be included in the parent's
     * "required" array. This should be set before defining the property type.
     *
     * Example:
     * ```kotlin
     * property("email") {
     *     required = true
     *     string { format = "email" }
     * }
     * ```
     */
    public var required: Boolean = false

    public fun string(block: StringPropertyBuilder.() -> Unit = {}): StringPropertyDefinition =
        StringPropertyBuilder().apply(block).build()

    public fun integer(block: NumericPropertyBuilder.() -> Unit = {}): NumericPropertyDefinition =
        NumericPropertyBuilder(type = "integer").apply(block).build()

    public fun number(block: NumericPropertyBuilder.() -> Unit = {}): NumericPropertyDefinition =
        NumericPropertyBuilder(type = "number").apply(block).build()

    public fun boolean(block: BooleanPropertyBuilder.() -> Unit = {}): BooleanPropertyDefinition =
        BooleanPropertyBuilder().apply(block).build()

    public fun array(block: ArrayPropertyBuilder.() -> Unit = {}): ArrayPropertyDefinition =
        ArrayPropertyBuilder().apply(block).build()

    public fun obj(block: ObjectPropertyBuilder.() -> Unit = {}): ObjectPropertyDefinition =
        ObjectPropertyBuilder().apply(block).build()

    public fun reference(ref: String): ReferencePropertyDefinition = ReferencePropertyDefinition(ref)
}

/**
 * Builder for [StringPropertyDefinition].
 *
 * This class is part of the JSON Schema DSL and cannot be instantiated directly.
 * Use [PropertyBuilder.string] instead within the DSL context.
 */
@JsonSchemaDsl
public class StringPropertyBuilder internal constructor() {
    public var type: List<String> = listOf("string")
    public var description: String? = null
    public var nullable: Boolean? = null
    public var format: String? = null
    public var enum: List<String>? = null
    public var minLength: Int? = null
    public var maxLength: Int? = null
    public var pattern: String? = null

    private var _default: JsonElement? = null
    private var _constValue: JsonElement? = null

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
 * This class is part of the JSON Schema DSL and cannot be instantiated directly.
 * Use [PropertyBuilder.integer] or [PropertyBuilder.number] instead within the DSL context.
 */
@JsonSchemaDsl
public class NumericPropertyBuilder internal constructor(
    type: String = "number",
) {
    public var type: List<String> = listOf(type)
    public var description: String? = null
    public var nullable: Boolean? = null
    public var multipleOf: Double? = null
    public var minimum: Double? = null
    public var exclusiveMinimum: Double? = null
    public var maximum: Double? = null
    public var exclusiveMaximum: Double? = null

    private var _default: JsonElement? = null
    private var _constValue: JsonElement? = null

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
 * This class is part of the JSON Schema DSL and cannot be instantiated directly.
 * Use [PropertyBuilder.boolean] instead within the DSL context.
 */
@JsonSchemaDsl
public class BooleanPropertyBuilder internal constructor() {
    public var type: List<String> = listOf("boolean")
    public var description: String? = null
    public var nullable: Boolean? = null

    private var _default: JsonElement? = null
    private var _constValue: JsonElement? = null

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
 * This class is part of the JSON Schema DSL and cannot be instantiated directly.
 * Use [PropertyBuilder.array] instead within the DSL context.
 */
@JsonSchemaDsl
public class ArrayPropertyBuilder internal constructor() {
    public var type: List<String> = listOf("array")
    public var description: String? = null
    public var nullable: Boolean? = null
    public var minItems: Int? = null
    public var maxItems: Int? = null
    private var itemsDefinition: PropertyDefinition? = null

    private var _default: JsonElement? = null

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
                                            "Array property default list item must be JsonElement, String, Number, Boolean, or null, but got: ${item::class.simpleName}",
                                        )
                                }
                            },
                        )

                    null -> null
                    else ->
                        error(
                            "Array property default must be List, JsonElement, or null, but got: ${value::class.simpleName}",
                        )
                }
        }

    public fun items(block: PropertyBuilder.() -> PropertyDefinition) {
        itemsDefinition = PropertyBuilder().block()
    }

    // Convenience methods for directly specifying item types without items { } wrapper
    public fun ofString(block: StringPropertyBuilder.() -> Unit = {}) {
        itemsDefinition = StringPropertyBuilder().apply(block).build()
    }

    public fun ofInteger(block: NumericPropertyBuilder.() -> Unit = {}) {
        itemsDefinition = NumericPropertyBuilder(type = "integer").apply(block).build()
    }

    public fun ofNumber(block: NumericPropertyBuilder.() -> Unit = {}) {
        itemsDefinition = NumericPropertyBuilder(type = "number").apply(block).build()
    }

    public fun ofBoolean(block: BooleanPropertyBuilder.() -> Unit = {}) {
        itemsDefinition = BooleanPropertyBuilder().apply(block).build()
    }

    public fun ofArray(block: ArrayPropertyBuilder.() -> Unit = {}) {
        itemsDefinition = ArrayPropertyBuilder().apply(block).build()
    }

    public fun ofObject(block: ObjectPropertyBuilder.() -> Unit = {}) {
        itemsDefinition = ObjectPropertyBuilder().apply(block).build()
    }

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
 * This class is part of the JSON Schema DSL and cannot be instantiated directly.
 * Use [PropertyBuilder.obj] instead within the DSL context.
 */
@JsonSchemaDsl
public class ObjectPropertyBuilder internal constructor() {
    public var type: List<String> = listOf("object")
    public var description: String? = null
    public var nullable: Boolean? = null
    public var additionalProperties: Boolean? = null
    private val properties: MutableMap<String, PropertyDefinition> = mutableMapOf()
    private val requiredFields: MutableSet<String> = mutableSetOf()

    private var _default: JsonElement? = null

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
                                            "Object property default map value must be JsonElement, String, Number, Boolean, or null, but got: ${v::class.simpleName}",
                                        )
                                }
                            },
                        )

                    null -> null
                    else ->
                        error(
                            "Object property default must be Map, JsonElement, or null, but got: ${value::class.simpleName}",
                        )
                }
        }

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
