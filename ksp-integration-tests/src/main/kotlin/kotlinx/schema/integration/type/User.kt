package kotlinx.schema.integration.type

import kotlinx.schema.Description
import kotlinx.schema.Schema

/**
 * User model matching the reflection test User for feature parity validation
 */
@Description("A user model")
@Schema
data class User(
    @Description("The name of the user")
    val name: String,
    val age: Int?,
    val email: String = "n/a",
    val tags: List<String>,
    val attributes: Map<String, Int>?,
)
