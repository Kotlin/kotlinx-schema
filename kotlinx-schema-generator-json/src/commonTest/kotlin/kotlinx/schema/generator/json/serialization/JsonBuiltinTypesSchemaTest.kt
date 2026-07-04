package kotlinx.schema.generator.json.serialization

import io.kotest.assertions.json.shouldEqualJson
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.schema.generator.core.ir.AnyNode
import kotlinx.schema.generator.core.ir.MapNode
import kotlinx.schema.generator.core.ir.ObjectNode
import kotlinx.schema.generator.core.ir.TypeRef
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlin.test.Test

/** Regression for #338: the serialization introspector crashed on built-in kotlinx.serialization JSON types. */
class JsonBuiltinTypesSchemaTest {
    @Serializable
    data class AllJsonTypes(
        val element: JsonElement,
        val obj: JsonObject,
        val arr: JsonArray,
        val prim: JsonPrimitive,
        val nullableElement: JsonElement?,
    )

    private val generator = SerializationClassJsonSchemaGenerator()

    @Test
    fun `Should generate schema for built-in kotlinx serialization JSON types`() {
        val schema = generator.generateSchemaString(AllJsonTypes.serializer().descriptor)

        schema shouldEqualJson
            // language=JSON
            $$"""
            {
              "$schema": "https://json-schema.org/draft/2020-12/schema",
              "$id": "kotlinx.schema.generator.json.serialization.JsonBuiltinTypesSchemaTest.AllJsonTypes",
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

    @Test
    fun `Should introspect JsonElement as AnyNode and JsonObject as Map to Any`() {
        val graph = SerializationClassSchemaIntrospector().introspect(AllJsonTypes.serializer().descriptor)
        val root = graph.nodes[(graph.root as TypeRef.Ref).id].shouldBeInstanceOf<ObjectNode>()

        val element = root.properties.first { it.name == "element" }.type.shouldBeInstanceOf<TypeRef.Inline>()
        element.node.shouldBeInstanceOf<AnyNode>()

        val obj = root.properties.first { it.name == "obj" }.type.shouldBeInstanceOf<TypeRef.Inline>()
        val map = obj.node.shouldBeInstanceOf<MapNode>()
        map.value.shouldBeInstanceOf<TypeRef.Inline>().node.shouldBeInstanceOf<AnyNode>()
    }
}
