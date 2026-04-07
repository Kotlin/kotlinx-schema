package kotlinx.schema.generator.json

import io.kotest.assertions.json.shouldEqualJson
import kotlinx.serialization.json.Json
import kotlin.test.Test

class RecursiveTypeReflectionTest {
    //region Test models

    @Suppress("unused")
    sealed class TreeNode {
        abstract val id: String

        data class Leaf(
            override val id: String,
            val value: String,
        ) : TreeNode()

        data class Branch(
            override val id: String,
            val left: TreeNode?,
            val right: TreeNode,
        ) : TreeNode()
    }

    data class Tree(
        val root: TreeNode,
    )

    data class LinkedNode(
        val value: String,
        val next: LinkedNode?,
    )

    //endregion

    private val generator =
        ReflectionClassJsonSchemaGenerator(
            json = Json { prettyPrint = true },
            config = JsonSchemaConfig.Default,
        )

    @Test
    fun `should generate schema for recursive sealed hierarchy`() {
        val schema = generator.generateSchemaString(Tree::class)

        schema shouldEqualJson
            // language=JSON
            $$"""
            {
              "$schema": "https://json-schema.org/draft/2020-12/schema",
              "$id": "kotlinx.schema.generator.json.RecursiveTypeReflectionTest.Tree",
              "type": "object",
              "properties": {
                "root": {
                  "$ref": "#/$defs/kotlinx.schema.generator.json.RecursiveTypeReflectionTest.TreeNode"
                }
              },
              "required": ["root"],
              "additionalProperties": false,
              "$defs": {
                "kotlinx.schema.generator.json.RecursiveTypeReflectionTest.TreeNode": {
                  "oneOf": [
                    { "$ref": "#/$defs/kotlinx.schema.generator.json.RecursiveTypeReflectionTest.TreeNode.Branch" },
                    { "$ref": "#/$defs/kotlinx.schema.generator.json.RecursiveTypeReflectionTest.TreeNode.Leaf" }
                  ]
                },
                "kotlinx.schema.generator.json.RecursiveTypeReflectionTest.TreeNode.Branch": {
                  "type": "object",
                  "properties": {
                    "type": {
                      "type": "string",
                      "const": "kotlinx.schema.generator.json.RecursiveTypeReflectionTest.TreeNode.Branch"
                    },
                    "id": { "type": "string" },
                    "left": {
                      "oneOf": [
                        { "type": "null" },
                        { "$ref": "#/$defs/kotlinx.schema.generator.json.RecursiveTypeReflectionTest.TreeNode" }
                      ]
                    },
                    "right": {
                      "$ref": "#/$defs/kotlinx.schema.generator.json.RecursiveTypeReflectionTest.TreeNode"
                    }
                  },
                  "required": ["type", "id", "left", "right"],
                  "additionalProperties": false
                },
                "kotlinx.schema.generator.json.RecursiveTypeReflectionTest.TreeNode.Leaf": {
                  "type": "object",
                  "properties": {
                    "type": {
                      "type": "string",
                      "const": "kotlinx.schema.generator.json.RecursiveTypeReflectionTest.TreeNode.Leaf"
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
        val schema = generator.generateSchemaString(LinkedNode::class)

        schema shouldEqualJson
            // language=JSON
            $$"""
            {
              "$schema": "https://json-schema.org/draft/2020-12/schema",
              "$id": "kotlinx.schema.generator.json.RecursiveTypeReflectionTest.LinkedNode",
              "type": "object",
              "properties": {
                "value": { "type": "string" },
                "next": {
                  "oneOf": [
                    { "type": "null" },
                    { "$ref": "#/$defs/kotlinx.schema.generator.json.RecursiveTypeReflectionTest.LinkedNode" }
                  ]
                }
              },
              "required": ["value", "next"],
              "additionalProperties": false,
              "$defs": {
                "kotlinx.schema.generator.json.RecursiveTypeReflectionTest.LinkedNode": {
                  "type": "object",
                  "properties": {
                    "value": { "type": "string" },
                    "next": {
                      "oneOf": [
                        { "type": "null" },
                        { "$ref": "#/$defs/kotlinx.schema.generator.json.RecursiveTypeReflectionTest.LinkedNode" }
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
