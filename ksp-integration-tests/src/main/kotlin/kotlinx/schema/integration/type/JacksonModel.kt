package kotlinx.schema.integration.type

import com.fasterxml.jackson.annotation.JsonClassDescription
import com.fasterxml.jackson.annotation.JsonPropertyDescription
import kotlinx.schema.Schema

/**
 * Model with Jackson @JsonClassDescription and @JsonPropertyDescription annotations
 * to test description extraction
 */
@JsonClassDescription("A purchasable product using Jackson annotations.")
@Schema
data class JacksonModel(
    @JsonPropertyDescription("Unique identifier for the product")
    val id: Long,
    @JsonPropertyDescription("Human-readable product name")
    val name: String,
    @JsonPropertyDescription("Optional detailed description of the product")
    val description: String?,
    @JsonPropertyDescription("Unit price expressed as a decimal number")
    val price: Double,
    @JsonPropertyDescription("Whether the product is currently in stock")
    val inStock: Boolean = true,
    @JsonPropertyDescription("List of tags for categorization and search")
    val tags: List<String> = emptyList(),
)

@Schema
@Suppress("FunctionOnlyReturningConstant")
fun createJacksonModel(): JacksonModel? = null
