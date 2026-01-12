package kotlinx.schema.integration.type

import kotlinx.schema.Description
import kotlinx.schema.Schema

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
