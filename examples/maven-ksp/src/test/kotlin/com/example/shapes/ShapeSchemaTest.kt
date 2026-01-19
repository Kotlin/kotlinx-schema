package com.example.shapes

import io.kotest.assertions.json.shouldEqualJson
import kotlin.test.Test
import kotlin.test.assertContains

class ShapeSchemaTest {
    @Test
    fun `Circle demonstrates KDoc and Description annotation`() {
        val schema = Circle::class.jsonSchemaString

        // Class-level KDoc is extracted
        assertContains(schema, "A circle defined by its radius")

        // @Description annotation is extracted
        assertContains(schema, "Radius in units (must be positive)")

        // Properties with defaults are optional
        schema shouldEqualJson $$"""
        {
            "$id": "com.example.shapes.Circle",
            "$defs": {
                "com.example.shapes.Circle": {
                    "type": "object",
                    "properties": {
                        "name": { "type": "string" },
                        "radius": {
                            "type": "number",
                            "description": "Radius in units (must be positive)"
                        },
                        "color": { "type": "string" }
                    },
                    "required": ["name", "radius"],
                    "additionalProperties": false,
                    "description": "A circle defined by its radius."
                }
            },
            "$ref": "#/$defs/com.example.shapes.Circle"
        }
        """
    }

    @Test
    fun `Shape sealed class generates oneOf schema`() {
        val schema = Shape::class.jsonSchemaString

        assertContains(schema, "oneOf")
        assertContains(schema, "com.example.shapes.Circle")
        assertContains(schema, "com.example.shapes.Rectangle")
    }

    @Test
    fun `Rectangle has class KDoc and property Description`() {
        val schema = Rectangle::class.jsonSchemaString

        // Class-level KDoc
        assertContains(schema, "A rectangle with width and height")

        // @Description annotation
        assertContains(schema, "Height in units")
    }

    @Test
    fun `Drawing contains nested Shape references`() {
        val schema = Drawing::class.jsonSchemaString

        // Contains reference to sealed Shape class
        assertContains(schema, "com.example.shapes.Shape")

        // Descriptions from @Description and KDoc
        assertContains(schema, "Name of this drawing")
    }
}
