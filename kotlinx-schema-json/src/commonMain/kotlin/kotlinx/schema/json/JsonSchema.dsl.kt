@file:Suppress("TooManyFunctions")

package kotlinx.schema.json

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject

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
 *         required("name", "age")
 *         property("name") {
 *             string {
 *                 description = "Person's name"
 *                 minLength = 1
 *             }
 *         }
 *         property("age") {
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
    private val requiredFields: MutableList<String> = mutableListOf()

    public fun property(
        name: String,
        block: PropertyBuilder.() -> PropertyDefinition,
    ) {
        properties[name] = PropertyBuilder().block()
    }

    public fun required(vararg fields: String) {
        requiredFields.addAll(fields)
    }

    public fun build(): JsonSchemaDefinition =
        JsonSchemaDefinition(
            id = id,
            schema = schema,
            properties = properties,
            required = requiredFields,
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
    public var default: JsonElement? = null
    public var constValue: JsonElement? = null

    /**
     * Sets the default value as a String, automatically converting it to JsonPrimitive.
     */
    public fun defaultValue(value: String) {
        default = JsonPrimitive(value)
    }

    /**
     * Sets the const value as a String, automatically converting it to JsonPrimitive.
     */
    public fun constValue(value: String) {
        constValue = JsonPrimitive(value)
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
            default = default,
            constValue = constValue,
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
    public var default: JsonElement? = null
    public var constValue: JsonElement? = null

    /**
     * Sets the default value as a Number, automatically converting it to JsonPrimitive.
     */
    public fun defaultValue(value: Number) {
        default = JsonPrimitive(value)
    }

    /**
     * Sets the const value as a Number, automatically converting it to JsonPrimitive.
     */
    public fun constValue(value: Number) {
        constValue = JsonPrimitive(value)
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
            default = default,
            constValue = constValue,
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
    public var default: JsonElement? = null
    public var constValue: JsonElement? = null

    /**
     * Sets the default value as a Boolean, automatically converting it to JsonPrimitive.
     */
    public fun defaultValue(value: Boolean) {
        default = JsonPrimitive(value)
    }

    /**
     * Sets the const value as a Boolean, automatically converting it to JsonPrimitive.
     */
    public fun constValue(value: Boolean) {
        constValue = JsonPrimitive(value)
    }

    public fun build(): BooleanPropertyDefinition =
        BooleanPropertyDefinition(
            type = type,
            description = description,
            nullable = nullable,
            default = default,
            constValue = constValue,
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
    public var minItems: UInt? = null
    public var maxItems: UInt? = null
    public var default: JsonElement? = null
    private var itemsDefinition: PropertyDefinition? = null

    public fun items(block: PropertyBuilder.() -> PropertyDefinition) {
        itemsDefinition = PropertyBuilder().block()
    }

    /**
     * Sets the default value as a List, automatically converting it to JsonArray.
     * Each element in the list is converted to JsonPrimitive.
     */
    public fun defaultValue(value: List<Any?>) {
        default =
            buildJsonArray {
                value.forEach { item ->
                    when (item) {
                        null -> add(JsonPrimitive(null as String?))
                        is String -> add(JsonPrimitive(item))
                        is Number -> add(JsonPrimitive(item))
                        is Boolean -> add(JsonPrimitive(item))
                        is JsonElement -> add(item)
                        else -> add(JsonPrimitive(item.toString()))
                    }
                }
            }
    }

    public fun build(): ArrayPropertyDefinition =
        ArrayPropertyDefinition(
            type = type,
            description = description,
            nullable = nullable,
            items = itemsDefinition,
            minItems = minItems,
            maxItems = maxItems,
            default = default,
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
    public var default: JsonElement? = null
    private val properties: MutableMap<String, PropertyDefinition> = mutableMapOf()
    private val requiredFields: MutableList<String> = mutableListOf()

    public fun property(
        name: String,
        block: PropertyBuilder.() -> PropertyDefinition,
    ) {
        properties[name] = PropertyBuilder().block()
    }

    public fun required(vararg fields: String) {
        requiredFields.addAll(fields)
    }

    /**
     * Sets the default value as a Map, automatically converting it to JsonObject.
     * Each value in the map is converted to JsonPrimitive.
     */
    public fun defaultValue(value: Map<String, Any?>) {
        default =
            buildJsonObject {
                value.forEach { (key, item) ->
                    when (item) {
                        null -> put(key, JsonPrimitive(null as String?))
                        is String -> put(key, JsonPrimitive(item))
                        is Number -> put(key, JsonPrimitive(item))
                        is Boolean -> put(key, JsonPrimitive(item))
                        is JsonElement -> put(key, item)
                        else -> put(key, JsonPrimitive(item.toString()))
                    }
                }
            }
    }

    /**
     * Sets the default value using vararg pairs, automatically converting it to JsonObject.
     * Each value in the pairs is converted to JsonPrimitive.
     *
     * Example:
     * ```kotlin
     * defaultValue("foo" to "bar", "baz" to 2)
     * ```
     */
    public fun defaultValue(vararg pairs: Pair<String, Any?>) {
        defaultValue(pairs.toMap())
    }

    public fun build(): ObjectPropertyDefinition =
        ObjectPropertyDefinition(
            type = type,
            description = description,
            nullable = nullable,
            properties = properties.ifEmpty { null },
            required = if (requiredFields.isEmpty()) null else requiredFields,
            additionalProperties = additionalProperties,
            default = default,
        )
}
