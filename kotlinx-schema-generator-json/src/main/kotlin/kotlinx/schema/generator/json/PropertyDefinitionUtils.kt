package kotlinx.schema.generator.json

import kotlinx.schema.json.AnyOfPropertyDefinition
import kotlinx.schema.json.ArrayPropertyDefinition
import kotlinx.schema.json.BooleanPropertyDefinition
import kotlinx.schema.json.NumericPropertyDefinition
import kotlinx.schema.json.ObjectPropertyDefinition
import kotlinx.schema.json.OneOfPropertyDefinition
import kotlinx.schema.json.PropertyDefinition
import kotlinx.schema.json.StringPropertyDefinition
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/**
 * Sets the const value on a property definition.
 * Only StringPropertyDefinition, NumericPropertyDefinition, and BooleanPropertyDefinition support const values.
 */
internal fun setConstValue(
    propertyDef: PropertyDefinition,
    constValue: Any?,
): PropertyDefinition {
    val jsonElement = toJsonElement(constValue) ?: return propertyDef

    return when (propertyDef) {
        is StringPropertyDefinition -> propertyDef.copy(constValue = jsonElement)
        is NumericPropertyDefinition -> propertyDef.copy(constValue = jsonElement)
        is BooleanPropertyDefinition -> propertyDef.copy(constValue = jsonElement)
        else -> propertyDef // Arrays and objects don't support const
    }
}

/**
 * Sets the default value on a property definition.
 */
internal fun setDefaultValue(
    propertyDef: PropertyDefinition,
    defaultValue: Any?,
): PropertyDefinition {
    val jsonElement = toJsonElement(defaultValue) ?: return propertyDef

    return when (propertyDef) {
        is StringPropertyDefinition -> propertyDef.copy(default = jsonElement)
        is NumericPropertyDefinition -> propertyDef.copy(default = jsonElement)
        is BooleanPropertyDefinition -> propertyDef.copy(default = jsonElement)
        is ArrayPropertyDefinition -> propertyDef.copy(default = jsonElement)
        is ObjectPropertyDefinition -> propertyDef.copy(default = jsonElement)
        else -> propertyDef
    }
}

/**
 * Sets the description on a property definition.
 */
internal fun setDescription(
    propertyDef: PropertyDefinition,
    description: String,
): PropertyDefinition =
    when (propertyDef) {
        is StringPropertyDefinition -> propertyDef.copy(description = description)
        is NumericPropertyDefinition -> propertyDef.copy(description = description)
        is BooleanPropertyDefinition -> propertyDef.copy(description = description)
        is ArrayPropertyDefinition -> propertyDef.copy(description = description)
        is ObjectPropertyDefinition -> propertyDef.copy(description = description)
        is AnyOfPropertyDefinition -> propertyDef.copy(description = description)
        is OneOfPropertyDefinition -> propertyDef.copy(description = description)
        else -> propertyDef
    }

/**
 * Removes the nullable flag from a property definition.
 */
internal fun removeNullableFlag(propertyDef: PropertyDefinition): PropertyDefinition =
    when (propertyDef) {
        is StringPropertyDefinition -> propertyDef.copy(nullable = null)
        is NumericPropertyDefinition -> propertyDef.copy(nullable = null)
        is BooleanPropertyDefinition -> propertyDef.copy(nullable = null)
        is ArrayPropertyDefinition -> propertyDef.copy(nullable = null)
        is ObjectPropertyDefinition -> propertyDef.copy(nullable = null)
        else -> propertyDef
    }

/**
 * Converts a Kotlin value to a JsonElement.
 */
private fun toJsonElement(value: Any?): JsonElement? =
    when (value) {
        null -> {
            JsonNull
        }

        is String -> {
            JsonPrimitive(value)
        }

        is Number -> {
            JsonPrimitive(value)
        }

        is Boolean -> {
            JsonPrimitive(value)
        }

        is Enum<*> -> {
            JsonPrimitive(value.name)
        }

        is List<*> -> {
            JsonArray(value.mapNotNull { toJsonElement(it) })
        }

        is Array<*> -> {
            JsonArray(value.mapNotNull { toJsonElement(it) })
        }

        is Map<*, *> -> {
            val entries =
                value.entries.mapNotNull { (k, v) ->
                    val key = k?.toString() ?: return@mapNotNull null
                    val element = toJsonElement(v) ?: return@mapNotNull null
                    key to element
                }
            JsonObject(entries.toMap())
        }

        else -> {
            null
        }
    }
