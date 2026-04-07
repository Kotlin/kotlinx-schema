package kotlinx.schema.generator.json

import io.kotest.assertions.json.shouldEqualJson
import io.kotest.assertions.throwables.shouldThrow
import kotlinx.schema.generator.core.ir.Discriminator
import kotlinx.schema.generator.core.ir.ObjectNode
import kotlinx.schema.generator.core.ir.PolymorphicNode
import kotlinx.schema.generator.core.ir.PrimitiveKind
import kotlinx.schema.generator.core.ir.PrimitiveNode
import kotlinx.schema.generator.core.ir.Property
import kotlinx.schema.generator.core.ir.SubtypeRef
import kotlinx.schema.generator.core.ir.TypeGraph
import kotlinx.schema.generator.core.ir.TypeId
import kotlinx.schema.generator.core.ir.TypeRef
import kotlinx.schema.json.encodeToString
import kotlin.test.Test

class TypeGraphToJsonSchemaTransformerTest {
    private val transformer = TypeGraphToJsonSchemaTransformer(config = JsonSchemaConfig.Default)

    @Test
    fun `failed node conversion does not leave placeholder in definitions`() {
        // A polymorphic type where one subtype references a TypeId not in the graph.
        // convertNode for the object will fail when it tries to resolve the dangling ref.
        val baseId = TypeId("Base")
        val goodId = TypeId("Good")
        val badId = TypeId("Bad")
        val danglingId = TypeId("Dangling")

        val goodNode = ObjectNode(
            name = "Good",
            properties = listOf(Property(name = "x", type = TypeRef.Inline(PrimitiveNode(PrimitiveKind.STRING)))),
            required = setOf("x"),
        )
        // Bad references a type that doesn't exist in the graph
        val badNode = ObjectNode(
            name = "Bad",
            properties = listOf(Property(name = "missing", type = TypeRef.Ref(danglingId))),
            required = setOf("missing"),
        )
        val polyNode = PolymorphicNode(
            baseName = "Base",
            subtypes = listOf(SubtypeRef(goodId), SubtypeRef(badId)),
            discriminator = Discriminator(name = "type"),
        )

        val rootNode = ObjectNode(
            name = "Root",
            properties = listOf(Property(name = "base", type = TypeRef.Ref(baseId))),
            required = setOf("base"),
        )
        val rootId = TypeId("Root")

        val graph = TypeGraph(
            root = TypeRef.Ref(rootId),
            nodes = mapOf(
                rootId to rootNode,
                baseId to polyNode,
                goodId to goodNode,
                badId to badNode,
                // danglingId intentionally missing
            ),
        )

        val error = shouldThrow<IllegalStateException> {
            transformer.transform(graph, "Root")
        }

        // The error should mention the dangling reference
        error.message.toString() shouldContainAny listOf("Dangling", "not found")
    }

    @Test
    fun `subtype used both in polymorphic hierarchy and as property gets discriminator exactly once`() {
        // Shape sealed hierarchy: Circle, Square
        // Container has both a `shape: Shape` (polymorphic) and `primaryCircle: Circle` (direct ref)
        // This exercises the path where Circle is registered via ensureNodeInDefinitions from
        // the direct ref, then convertPolymorphic also encounters it.
        val shapeId = TypeId("Shape")
        val circleId = TypeId("Circle")
        val squareId = TypeId("Square")

        val circleNode = ObjectNode(
            name = "Circle",
            properties = listOf(Property(name = "radius", type = TypeRef.Inline(PrimitiveNode(PrimitiveKind.DOUBLE)))),
            required = setOf("radius"),
        )
        val squareNode = ObjectNode(
            name = "Square",
            properties = listOf(Property(name = "side", type = TypeRef.Inline(PrimitiveNode(PrimitiveKind.DOUBLE)))),
            required = setOf("side"),
        )
        val shapeNode = PolymorphicNode(
            baseName = "Shape",
            subtypes = listOf(SubtypeRef(circleId), SubtypeRef(squareId)),
            discriminator = Discriminator(name = "type"),
        )

        // Container references Circle directly AND Shape (which includes Circle)
        val containerId = TypeId("Container")
        val containerNode = ObjectNode(
            name = "Container",
            properties = listOf(
                Property(name = "primaryCircle", type = TypeRef.Ref(circleId)),
                Property(name = "shape", type = TypeRef.Ref(shapeId)),
            ),
            required = setOf("primaryCircle", "shape"),
        )

        val graph = TypeGraph(
            root = TypeRef.Ref(containerId),
            nodes = mapOf(
                containerId to containerNode,
                shapeId to shapeNode,
                circleId to circleNode,
                squareId to squareNode,
            ),
        )

        val schema = transformer.transform(graph, "Container")
        val schemaJson = schema.encodeToString(json)

        // Circle should have discriminator "type" exactly once in required
        schemaJson shouldEqualJson
            // language=JSON
            $$"""
            {
              "$schema": "https://json-schema.org/draft/2020-12/schema",
              "$id": "Container",
              "type": "object",
              "properties": {
                "primaryCircle": {
                  "$ref": "#/$defs/Circle"
                },
                "shape": {
                  "$ref": "#/$defs/Shape"
                }
              },
              "required": ["primaryCircle", "shape"],
              "additionalProperties": false,
              "$defs": {
                "Circle": {
                  "type": "object",
                  "properties": {
                    "type": {
                      "type": "string",
                      "const": "Circle"
                    },
                    "radius": { "type": "number" }
                  },
                  "required": ["type", "radius"],
                  "additionalProperties": false
                },
                "Shape": {
                  "oneOf": [
                    { "$ref": "#/$defs/Circle" },
                    { "$ref": "#/$defs/Square" }
                  ]
                },
                "Square": {
                  "type": "object",
                  "properties": {
                    "type": {
                      "type": "string",
                      "const": "Square"
                    },
                    "side": { "type": "number" }
                  },
                  "required": ["type", "side"],
                  "additionalProperties": false
                }
              }
            }
            """.trimIndent()
    }
}

private infix fun String.shouldContainAny(candidates: List<String>) {
    require(candidates.any { this.contains(it) }) {
        "Expected string to contain any of $candidates, but was: $this"
    }
}
