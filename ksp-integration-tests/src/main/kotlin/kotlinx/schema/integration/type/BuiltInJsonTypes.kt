package kotlinx.schema.integration.type

import kotlinx.schema.Schema
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

// Fixture for #338. Non-KDoc comment so KSP doesn't emit it as the schema description.
@Schema
data class BuiltInJsonTypes(
    val element: JsonElement,
    val obj: JsonObject,
    val arr: JsonArray,
    val prim: JsonPrimitive,
    val nullableElement: JsonElement?,
)
