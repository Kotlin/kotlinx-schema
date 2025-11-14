package kotlinx.schema.integration

import kotlinx.schema.Description
import kotlinx.schema.Schema

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
