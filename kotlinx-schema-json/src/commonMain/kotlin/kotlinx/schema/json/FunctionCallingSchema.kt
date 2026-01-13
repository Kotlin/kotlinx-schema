package kotlinx.schema.json

import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonObject

/**
 * FunctionCallingSchema describes a function/tool call in LLM function calling APIs.
 *
 * This follows the [OpenAI function calling format](https://platform.openai.com/docs/guides/function-calling)
 * where a tool is defined as:
 * - type: Always "function"
 * - name: The name of the function
 * - description: What the function does
 * - strict: Whether to enforce strict schema adherence
 * - parameters: The function's parameter schema
 */
@Serializable
@OptIn(ExperimentalSerializationApi::class)
public data class FunctionCallingSchema(
    @EncodeDefault
    public val type: String = "function",
    public val name: String,
    /**
     * Optional title. Useful for MCP Tool definition
     */
    public val title: String? = null,
    public val description: String? = null,
    /**
     * See [Strict mode](https://platform.openai.com/docs/guides/function-calling?strict-mode=disabled#strict-mode)
     */
    @EncodeDefault
    public val strict: Boolean? = true,
    public val parameters: ObjectPropertyDefinition,
)

/**
 * Encodes the given [FunctionCallingSchema] instance into a [JsonObject] representation.
 *
 * @param json The [Json] instance to use for serialization. Defaults to [Json] instance with default configuration.
 * @return A [JsonObject] representing the serialized [FunctionCallingSchema].
 */
public fun FunctionCallingSchema.encodeToJsonObject(json: Json = Json): JsonObject =
    json.encodeToJsonElement(this).jsonObject

/**
 * Encodes the [FunctionCallingSchema] instance into its JSON string representation.
 *
 * @param json The [Json] instance to use for serialization. Defaults to [Json] instance with default configuration.
 * @return The JSON string representation of the [FunctionCallingSchema] instance.
 */
public fun FunctionCallingSchema.encodeToString(json: Json = Json): String = json.encodeToString(this)
