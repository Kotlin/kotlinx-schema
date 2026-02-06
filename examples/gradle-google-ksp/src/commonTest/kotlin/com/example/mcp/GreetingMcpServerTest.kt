package com.example.mcp

import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test

class GreetingMcpServerTest {
    @Test
    fun shouldCreateMcpServer() {
        // given
        val parametersSchema =
            greetJsonSchema()
                .jsonObject["parameters"]
                ?.jsonObject

        val resultSchema = GreetingResponse::class.jsonSchema.jsonObject

        // when
        createMcpServer() shouldNotBeNull {
            // then
            tools["greet"]?.tool shouldNotBeNull {
                name shouldBe "greet"
                inputSchema.properties shouldNotBeNull {
                    this shouldBe
                        parametersSchema
                            ?.get("properties")
                            ?.jsonObject
                }
                inputSchema.required shouldNotBeNull {
                    this shouldBe
                        parametersSchema
                            ?.get("required")
                            ?.jsonArray
                            ?.map { it.jsonPrimitive.content }
                }
                outputSchema?.properties shouldNotBeNull {
                    this shouldBe resultSchema.jsonObject["properties"]
                }
                outputSchema?.required shouldNotBeNull {
                    this shouldBe
                        resultSchema.jsonObject["required"]
                            ?.jsonArray
                            ?.map { it.jsonPrimitive.content }
                }
            }
        }
    }
}
