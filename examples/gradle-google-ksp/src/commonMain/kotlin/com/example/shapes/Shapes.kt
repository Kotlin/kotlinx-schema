@file:Suppress("unused", "MatchingDeclarationName")

package com.example.shapes

import kotlinx.schema.Description
import kotlinx.schema.Schema

/**
 * A geometric shape. This sealed class demonstrates polymorphic schema generation.
 */
@Schema // Marker annotation!
sealed class Shape {
    @Description("A name for this shape")
    abstract val name: String

    /**
     * A circle defined by its radius.
     */
    @Schema
    data class Circle(
        override val name: String,
        @Description("Radius in units (must be positive)")
        val radius: Double,
    ) : Shape()

    /**
     * A rectangle with width and height.
     */
    @Schema
    data class Rectangle(
        override val name: String,
        @Description("Width in units (must be positive)")
        val width: Double,
        @Description("Height in units (must be positive)")
        val height: Double,
    ) : Shape()
}

/**
 * Greets the user with a personalized message.
 *
 * @param name the name of the person to greet
 * @return a greeting message addressed to the specified name
 */
@Schema
internal fun sayHello(
    @Description("Name to greet") name: String?,
): String = "Hello, ${name ?: "friend"}!"
