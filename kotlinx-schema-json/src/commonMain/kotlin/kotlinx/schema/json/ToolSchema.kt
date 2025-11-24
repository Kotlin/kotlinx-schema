package kotlinx.schema.json

import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable

/**
 * ToolSchema describes a method call.
 */
@Serializable
@OptIn(ExperimentalSerializationApi::class)
public data class ToolSchema(
    @EncodeDefault
    public val properties: Map<String, PropertyDefinition> = emptyMap(),
    @EncodeDefault
    public val required: List<String> = emptyList(),
) {
    @EncodeDefault
    val type: String = "object"
}
