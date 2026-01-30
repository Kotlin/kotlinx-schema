package com.example.shapes

import io.kotest.assertions.json.shouldEqualSpecifiedJson
import kotlin.test.Test

@Suppress("LongMethod")
class ShapeSchemaTest {
    @Test
    fun `Circle demonstrates KDoc and Description annotation`() {
        // Properties with defaults are optional
        Shape.Circle::class.jsonSchemaString shouldEqualSpecifiedJson $$"""
        {
          "$schema": "https://json-schema.org/draft/2020-12/schema",
          "$id": "com.example.shapes.Shape.Circle",
          "description": "A circle defined by its radius.",
          "type": "object",
          "properties": {
            "name": {
              "type": "string"
            },
            "radius": {
              "type": "number",
              "description": "Radius in units (must be positive)"
            }
          },
          "required": [
            "name",
            "radius"
          ],
          "additionalProperties": false
        }
        """
    }

    @Test
    fun `Shape sealed class generates oneOf schema`() {
        val schema = Shape::class.jsonSchemaString

        schema shouldEqualSpecifiedJson $$"""
            {
                "$schema": "https://json-schema.org/draft/2020-12/schema",
                "$id": "com.example.shapes.Shape",
                "description": "A geometric shape. This sealed class demonstrates polymorphic schema generation.",
                "type": "object",
                "additionalProperties": false,
                "oneOf": [
                    {
                        "$ref": "#/$defs/com.example.shapes.Shape.Circle"
                    },
                    {
                        "$ref": "#/$defs/com.example.shapes.Shape.Rectangle"
                    }
                ],
                "$defs": {
                    "com.example.shapes.Shape.Circle": {
                        "type": "object",
                        "description": "A circle defined by its radius.",
                        "properties": {
                            "name": {
                                "type": "string"
                            },
                            "radius": {
                                "type": "number",
                                "description": "Radius in units (must be positive)"
                            }
                        },
                        "required": [
                            "name",
                            "radius"
                        ],
                        "additionalProperties": false
                    },
                    "com.example.shapes.Shape.Rectangle": {
                        "type": "object",
                        "description": "A rectangle with width and height.",
                        "properties": {
                            "name": {
                                "type": "string"
                            },
                            "width": {
                                "type": "number",
                                "description": "Width in units (must be positive)"
                            },
                            "height": {
                                "type": "number",
                                "description": "Height in units (must be positive)"
                            }
                        },
                        "required": [
                            "name",
                            "width",
                            "height"
                        ],
                        "additionalProperties": false
                    }
                }
            }
            """
    }
}
