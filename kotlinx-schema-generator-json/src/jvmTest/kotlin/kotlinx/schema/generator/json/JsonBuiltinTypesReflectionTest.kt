package kotlinx.schema.generator.json

import io.kotest.assertions.json.shouldEqualJson
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlin.test.Test

/** Reflection counterpart of the serialization #338 test (reflection mis-generated these types). */
class JsonBuiltinTypesReflectionTest {
    data class AllJsonTypes(
        val element: JsonElement,
        val obj: JsonObject,
        val arr: JsonArray,
        val prim: JsonPrimitive,
        val nullableElement: JsonElement?,
    )

    private val generator = ReflectionClassJsonSchemaGenerator.Default

    @Test
    fun `Should generate schema for built-in kotlinx serialization JSON types via reflection`() {
        val schema = generator.generateSchemaString(AllJsonTypes::class)

        schema shouldEqualJson
            // language=JSON
            $$"""
            {
              "$schema": "https://json-schema.org/draft/2020-12/schema",
              "$id": "kotlinx.schema.generator.json.JsonBuiltinTypesReflectionTest.AllJsonTypes",
              "type": "object",
              "properties": {
                "element": {},
                "obj": {
                  "type": "object",
                  "additionalProperties": {}
                },
                "arr": {
                  "type": "array",
                  "items": {}
                },
                "prim": {},
                "nullableElement": {}
              },
              "additionalProperties": false,
              "required": [
                "element",
                "obj",
                "arr",
                "prim",
                "nullableElement"
              ]
            }
            """.trimIndent()
    }
}
