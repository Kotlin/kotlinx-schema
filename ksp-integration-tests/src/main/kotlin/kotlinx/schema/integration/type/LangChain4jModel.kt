package kotlinx.schema.integration.type

import kotlinx.schema.Description
import kotlinx.schema.Schema

/**
 * Model with LangChain4j @P annotations to test description extraction.
 * LangChain4j uses @P annotation for parameter descriptions in tool definitions.
 */
@Description("A purchasable product using LangChain4j annotations.")
@Schema
data class LangChain4jModel(
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
