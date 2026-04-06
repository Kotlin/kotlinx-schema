package kotlinx.schema.integration.type

import kotlinx.schema.Schema
import kotlinx.schema.SerialDescription
import kotlinx.serialization.Serializable

@Serializable
@Schema
@SerialDescription("A product described with @SerialDescription")
data class SerialDescribedProduct(
    @SerialDescription("Unique product identifier")
    val id: Long,
    @SerialDescription("Human-readable product name")
    val name: String,
    val price: Double,
)
