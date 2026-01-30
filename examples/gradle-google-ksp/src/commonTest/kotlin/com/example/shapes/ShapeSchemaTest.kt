package com.example.shapes

import io.kotest.assertions.json.shouldEqualJson
import io.kotest.assertions.json.shouldEqualSpecifiedJson
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlin.test.Test

@Suppress("LongMethod")
class ShapeSchemaTest {
    @Test
    fun `Circle demonstrates KDoc and Description annotation`() {
        // Properties with defaults are optional
        Circle::class.jsonSchemaString shouldEqualSpecifiedJson $$"""
        {
            "$schema": "https://json-schema.org/draft/2020-12/schema",
            "$id": "com.example.shapes.Circle",
            "description": "A circle defined by its radius.",
            "type": "object",
            "properties": {
                "name": {
                  "type": "string"
                },
                "radius": {
                  "type": "number",
                  "description": "Radius in units (must be positive)"
                },
                "color": {
                  "type": "string"
                }
            },
            "required": [
                "name",
                "radius",
                "color"
            ],
            "additionalProperties": false
        }
        """
    }

    @Test
    fun `Shape sealed class generates oneOf schema`() {
        val schema = Shape::class.jsonSchemaString

        schema shouldEqualSpecifiedJson $$"""{
            "$schema": "https://json-schema.org/draft/2020-12/schema",
            "$id": "com.example.shapes.Shape",
            "description": "A geometric shape. This sealed class demonstrates polymorphic schema generation.",
            "type": "object",
            "additionalProperties": false,
            "oneOf": [
                {
                    "$ref": "#/$defs/com.example.shapes.Circle"
                },
                {
                    "$ref": "#/$defs/com.example.shapes.Rectangle"
                }
            ],
            "$defs": {
                "com.example.shapes.Circle": {
                    "type": "object",
                    "description": "A circle defined by its radius.",
                    "properties": {
                        "name": {
                            "type": "string"
                        },
                        "radius": {
                            "type": "number",
                            "description": "Radius in units (must be positive)"
                        },
                        "color": {
                            "type": "string"
                        }
                    },
                    "required": [
                        "name",
                        "radius",
                        "color"
                    ],
                    "additionalProperties": false
                },
                "com.example.shapes.Rectangle": {
                    "type": "object",
                    "description": "A rectangle with width and height.",
                    "properties": {
                        "name": {
                            "type": "string"
                        },
                        "width": {
                            "type": "number"
                        },
                        "height": {
                            "type": "number",
                            "description": "Height in units"
                        },
                        "color": {
                            "type": "string"
                        }
                    },
                    "required": [
                        "name",
                        "width",
                        "height",
                        "color"
                    ],
                    "additionalProperties": false
                }
            }
        }"""
    }

    @Test
    fun `Drawing contains nested Shape references`() {
        val jsonSchemaString = Drawing::class.jsonSchemaString

        Json.encodeToString(Drawing::class.jsonSchema) shouldEqualJson jsonSchemaString

        jsonSchemaString shouldEqualSpecifiedJson
            $$"""
           {
            "$schema": "https://json-schema.org/draft/2020-12/schema",
            "$id": "com.example.shapes.Drawing",
            "description": "Container for multiple shapes.",
            "type": "object",
            "properties": {
                "name": {
                    "type": "string",
                    "description": "Name of this drawing"
                },
                "shapes": {
                    "type": "array",
                    "items": {
                        "oneOf": [
                            {
                                "$ref": "#/$defs/com.example.shapes.Circle"
                            },
                            {
                                "$ref": "#/$defs/com.example.shapes.Rectangle"
                            }
                        ],
                        "description": "A geometric shape. This sealed class demonstrates polymorphic schema generation."
                    }
                }
            },
            "required": [
                "name",
                "shapes"
            ],
            "additionalProperties": false,
            "$defs": {
                "com.example.shapes.Circle": {
                    "type": "object",
                    "description": "A circle defined by its radius.",
                    "properties": {
                        "name": {
                            "type": "string"
                        },
                        "radius": {
                            "type": "number",
                            "description": "Radius in units (must be positive)"
                        },
                        "color": {
                            "type": "string"
                        }
                    },
                    "required": [
                        "name",
                        "radius",
                        "color"
                    ],
                    "additionalProperties": false
                },
                "com.example.shapes.Rectangle": {
                    "type": "object",
                    "description": "A rectangle with width and height.",
                    "properties": {
                        "name": {
                            "type": "string"
                        },
                        "width": {
                            "type": "number"
                        },
                        "height": {
                            "type": "number",
                            "description": "Height in units"
                        },
                        "color": {
                            "type": "string"
                        }
                    },
                    "required": [
                        "name",
                        "width",
                        "height",
                        "color"
                    ],
                    "additionalProperties": false
                }
            }
        }
            """.trimMargin()
    }

    @Test
    fun `should generate function schema`() {
        val functionCallSchema: JsonObject = calculateAreaJsonSchema()
        val functionCallSchemaString: String = calculateAreaJsonSchemaString()
        functionCallSchemaString shouldEqualJson
            """
            {
                "type":"function",
                "name":"calculateArea",
                "description":"We don't care how it is calculated, just that it is.",
                "strict":true,
                "parameters": {
                  "type":"object",
                  "properties": {
                    "shape": {"type":"string"}
                  },
                  "required":["shape"],
                  "additionalProperties":false
                }
            }
            """.trimIndent()

        Json.encodeToString(functionCallSchema) shouldEqualJson functionCallSchemaString
    }
}
