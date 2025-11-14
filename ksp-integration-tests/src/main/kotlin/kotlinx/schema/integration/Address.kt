package kotlinx.schema.integration

import kotlinx.schema.Description
import kotlinx.schema.Schema

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
