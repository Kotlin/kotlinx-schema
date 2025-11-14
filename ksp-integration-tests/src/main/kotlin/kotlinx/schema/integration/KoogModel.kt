package kotlinx.schema.integration

import ai.koog.agents.core.tools.annotations.LLMDescription
import kotlinx.schema.Schema

/**
 * Model with optional fields and different types
 */
@LLMDescription(description = "A purchasable product with pricing and inventory info.")
@Schema
data class KoogModel(
    @LLMDescription(description = "Unique identifier for the product")
    val id: Long,
    @LLMDescription("Human-readable product name")
    val name: String,
    @LLMDescription("Optional detailed description of the product")
    val description: String?,
    @LLMDescription("Unit price expressed as a decimal number")
    val price: Double,
    @LLMDescription(description = "Whether the product is currently in stock")
    val inStock: Boolean = true,
    @LLMDescription("List of tags for categorization and search")
    val tags: List<String> = emptyList(),
)
