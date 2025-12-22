package kotlinx.schema.integration

import kotlinx.schema.Description
import kotlinx.schema.Schema

/**
 * A person with a first and last name and age.
 *
 * @since 1.0
 */
@Schema(withSchemaObject = true)
data class Person(
    @Description("Given name of the person")
    val firstName: String,
    @Description("Family name of the person")
    val lastName: String,
    @Description("Age of the person in years")
    val age: Int,
)

/**
 * Nested class structure to test complex scenarios
 */
@Description("A postal address for deliveries and billing.")
@Schema
data class Address(
    @Description("Street address, including house number")
    val street: String,
    @Description("City or town name")
    val city: String,
    @Description("Postal or ZIP code")
    val zipCode: String,
    @Description("Two-letter ISO country code; defaults to US")
    val country: String = "US",
)

/**
 * Model with optional fields and different types
 */
@Description("A purchasable product with pricing and inventory info.")
@Schema
data class Product(
    @Description("Unique identifier for the product")
    val id: Long,
    @Description("Human-readable product name")
    val name: String,
    @Description("Optional detailed description of the product")
    val description: String?,
    @Description("Unit price expressed as a decimal number")
    val price: Double,
    @Description("Whether the product is currently in stock")
    val inStock: Boolean = true,
    @Description("List of tags for categorization and search")
    val tags: List<String> = emptyList(),
)

/**
 * Simple class without annotation - should not generate extensions
 */
data class NonAnnotatedClass(
    val value: String,
)

/**
 * Class with custom schema annotation parameter
 */
@Description("A class using a custom schema type value.")
@Schema("custom-schema")
data class CustomSchemaClass(
    @Description("A field included to validate custom schema handling")
    val customField: String,
)

/**
 * Generic class to test KSP with generics
 */
@Description("A generic container that wraps content with optional metadata.")
@Schema
data class Container<T>(
    @Description("The wrapped content value")
    val content: T,
    @Description("Arbitrary metadata key-value pairs")
    val metadata: Map<String, Any> = emptyMap(),
)

/**
 * Enum class with Schema annotation
 */
@Description("Current lifecycle status of an entity.")
@Schema
enum class Status {
    @Description("Entity is active and usable")
    ACTIVE,

    @Description("Entity is inactive or disabled")
    INACTIVE,

    @Description("Entity is pending activation or approval")
    PENDING,
}

/**
 * Class with nested Schema-annotated classes
 */
@Description("An order placed by a customer containing multiple items.")
@Schema(withSchemaObject = true)
data class Order(
    @Description("Unique order identifier")
    val id: String,
    @Description("The customer who placed the order")
    val customer: Person,
    @Description("Destination address for shipment")
    val shippingAddress: Address,
    @Description("List of items included in the order")
    val items: List<Product>,
    @Description("Current status of the order")
    val status: Status,
)

/**
 * Sealed class hierarchy to test polymorphism support
 */
@Description("Represents a geometric shape")
@Schema(withSchemaObject = true)
sealed class Shape {
    @Description("A circular shape")
    @Schema
    data class Circle(
        @Description("Radius of the circle")
        val radius: Double,
    ) : Shape()

    @Description("A rectangular shape")
    @Schema
    data class Rectangle(
        @Description("Width of the rectangle")
        val width: Double,
        @Description("Height of the rectangle")
        val height: Double,
    ) : Shape()

    @Description("A triangular shape")
    @Schema
    data class Triangle(
        @Description("Length of the base")
        val base: Double,
        @Description("Height from base to apex")
        val height: Double,
    ) : Shape()
}
