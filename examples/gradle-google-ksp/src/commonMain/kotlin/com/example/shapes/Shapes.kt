package com.example.shapes

import kotlinx.schema.Description
import kotlinx.schema.Schema
import kotlin.random.Random

/**
 * A geometric shape. This sealed class demonstrates polymorphic schema generation.
 */
@Schema
sealed class Shape {
    abstract val name: String
}

/**
 * A circle defined by its radius.
 */
@Schema
data class Circle(
    /**
     * Identifier for this circle
     */
    override val name: String,
    @Description("Radius in units (must be positive)")
    val radius: Double,
    /**
     * Fill color in hex format (e.g., #FF5733)
     */
    val color: String = "#FF5733",
) : Shape()

/**
 * A rectangle with width and height.
 */
@Schema
data class Rectangle(
    override val name: String,
    /**
     * Width in units
     */
    val width: Double,
    @Description("Height in units")
    val height: Double,
    val color: String = "#3498db",
) : Shape()

/**
 * Container for multiple shapes.
 */
@Schema
data class Drawing(
    @Description("Name of this drawing")
    val name: String,
    /**
     * Shapes included in this drawing
     */
    val shapes: List<Shape>,
)

/**
 * We don't care how it is calculated, just that it is.
 */
@Schema
internal fun calculateArea(shape: Shape): Double = Random.nextDouble(shape.name.length.toDouble())
