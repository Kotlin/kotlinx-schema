package kotlinx.schema.generator.json.serialization

import io.kotest.assertions.json.shouldEqualJson
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.test.Test

class RecursiveTypeSchemaTest {
    //region Test models

    @Serializable
    @SerialName("Node")
    private sealed class Node {
        abstract val id: String

        @Serializable
        @SerialName("Leaf")
        data class Leaf(
            override val id: String,
            val value: String,
        ) : Node()

        @Serializable
        @SerialName("Branch")
        data class Branch(
            override val id: String,
            val left: Node?,
            val right: Node,
        ) : Node()
    }

    @Serializable
    @SerialName("Tree")
    private data class Tree(
        val root: Node,
    )

    @Serializable
    @SerialName("LinkedNode")
    private data class LinkedNode(
        val value: String,
        val next: LinkedNode?,
    )

    //endregion

    private val generator = SerializationClassJsonSchemaGenerator()

    @Test
    fun `should generate schema for recursive sealed hierarchy`() {
        val schema = generator.generateSchemaString(Tree.serializer().descriptor)

        schema shouldEqualJson
            // language=JSON
            $$"""
            {
              "$schema": "https://json-schema.org/draft/2020-12/schema",
              "$id": "Tree",
              "type": "object",
              "properties": {
                "root": {
                  "$ref": "#/$defs/Node"
                }
              },
              "required": ["root"],
              "additionalProperties": false,
              "$defs": {
                "Node": {
                  "oneOf": [
                    { "$ref": "#/$defs/Branch" },
                    { "$ref": "#/$defs/Leaf" }
                  ]
                },
                "Branch": {
                  "type": "object",
                  "properties": {
                    "type": {
                      "type": "string",
                      "const": "Branch"
                    },
                    "id": { "type": "string" },
                    "left": {
                      "oneOf": [
                        { "type": "null" },
                        { "$ref": "#/$defs/Node" }
                      ]
                    },
                    "right": {
                      "$ref": "#/$defs/Node"
                    }
                  },
                  "required": ["type", "id", "left", "right"],
                  "additionalProperties": false
                },
                "Leaf": {
                  "type": "object",
                  "properties": {
                    "type": {
                      "type": "string",
                      "const": "Leaf"
                    },
                    "id": { "type": "string" },
                    "value": { "type": "string" }
                  },
                  "required": ["type", "id", "value"],
                  "additionalProperties": false
                }
              }
            }
            """.trimIndent()
    }

    @Test
    fun `should generate schema for self-referencing type`() {
        val schema = generator.generateSchemaString(LinkedNode.serializer().descriptor)

        schema shouldEqualJson
            // language=JSON
            $$"""
            {
              "$schema": "https://json-schema.org/draft/2020-12/schema",
              "$id": "LinkedNode",
              "type": "object",
              "properties": {
                "value": { "type": "string" },
                "next": {
                  "oneOf": [
                    { "type": "null" },
                    { "$ref": "#/$defs/LinkedNode" }
                  ]
                }
              },
              "required": ["value", "next"],
              "additionalProperties": false,
              "$defs": {
                "LinkedNode": {
                  "type": "object",
                  "properties": {
                    "value": { "type": "string" },
                    "next": {
                      "oneOf": [
                        { "type": "null" },
                        { "$ref": "#/$defs/LinkedNode" }
                      ]
                    }
                  },
                  "required": ["value", "next"],
                  "additionalProperties": false
                }
              }
            }
            """.trimIndent()
    }
}
