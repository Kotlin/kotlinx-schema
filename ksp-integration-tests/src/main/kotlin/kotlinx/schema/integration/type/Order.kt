package kotlinx.schema.integration.type

import kotlinx.schema.Description
import kotlinx.schema.Schema

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
