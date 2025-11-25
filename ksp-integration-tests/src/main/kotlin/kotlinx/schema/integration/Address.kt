package kotlinx.schema.integration

import kotlinx.schema.Description
import kotlinx.schema.Schema

/**
 * A postal address for deliveries and billing.
 *
 * @author Konstantin Pavlov
 * @see [String]
 * @since 1.0
 * @param street This `@param` description should be ignored
 * @property street This `@property` description should be ignored
 */
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
