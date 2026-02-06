@file:Suppress("MatchingDeclarationName")

package com.example.mcp

import io.ktor.server.cio.CIO
import io.ktor.server.engine.embeddedServer
import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.server.ServerOptions
import io.modelcontextprotocol.kotlin.sdk.server.mcp
import io.modelcontextprotocol.kotlin.sdk.types.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.types.Implementation
import io.modelcontextprotocol.kotlin.sdk.types.ServerCapabilities
import io.modelcontextprotocol.kotlin.sdk.types.ToolSchema
import kotlinx.schema.Schema
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.time.Clock

/**
 * Response to a greeting request.
 *
 * @property message Generated greeting message.
 * @property timestamp The timestamp at which the greeting was generated, in milliseconds since the epoch.
 */
@Schema
@Serializable
internal data class GreetingResponse(
    val message: String,
    val timestamp: Long,
)

/**
 * Generates a greeting response with a personalized message.
 *
 * @param firstName The first name of the individual to be greeted.
 * @param lastName The optional last name of the individual to be greeted.
 * @return A `GreetingResponse` containing the request ID, greeting message, and generation timestamp.
 */
@Schema
internal fun greet(
    firstName: String,
    lastName: String? = null,
): GreetingResponse =
    GreetingResponse(
        message = if (lastName != null) "Hello, $firstName $lastName" else "Hello, $firstName",
        timestamp = Clock.System.now().toEpochMilliseconds(),
    )

internal fun createMcpServer(): Server {
    val server =
        Server(
            Implementation(
                name = "Greeting MCP Server",
                version = "1.0",
            ),
            ServerOptions(
                capabilities =
                    ServerCapabilities(
                        tools = ServerCapabilities.Tools(),
                    ),
            ),
        )

    // parameters
    val functionSchema = greetJsonSchema()
    val toolName = requireNotNull(functionSchema["name"]?.jsonPrimitive?.content)
    val toolDescription = requireNotNull(functionSchema["description"]?.jsonPrimitive?.content)
    val paramsSchema = requireNotNull(functionSchema.jsonObject["parameters"]?.jsonObject)
    // response
    val responseSchema = GreetingResponse::class.jsonSchema

    server.addTool(
        name = toolName,
        description = toolDescription,
        inputSchema =
            ToolSchema(
                properties = paramsSchema["properties"]?.jsonObject,
                required = paramsSchema["required"]?.jsonArray?.map { it.jsonPrimitive.content },
            ),
        outputSchema =
            ToolSchema(
                properties = responseSchema["properties"]?.jsonObject,
                required = responseSchema["required"]?.jsonArray?.map { it.jsonPrimitive.content },
            ),
    ) { request ->
        val result =
            greet(
                firstName =
                    requireNotNull(
                        request.params.arguments
                            ?.get("firstName")
                            ?.jsonPrimitive
                            ?.content,
                    ),
                lastName =
                    request.params.arguments
                        ?.get("lastName")
                        ?.jsonPrimitive
                        ?.contentOrNull,
            )

        CallToolResult(
            structuredContent = Json.encodeToJsonElement(result).jsonObject,
            content = emptyList(),
        )
    }

    return server
}

fun main() {
    embeddedServer(CIO, host = "127.0.0.1", port = 3001) {
        mcp {
            return@mcp createMcpServer()
        }
    }.start(wait = true)
}
