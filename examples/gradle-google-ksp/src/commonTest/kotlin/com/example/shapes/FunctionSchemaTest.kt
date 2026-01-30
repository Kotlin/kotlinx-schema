package com.example.shapes

import io.kotest.assertions.json.shouldEqualJson
import io.kotest.assertions.json.shouldEqualSpecifiedJson
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlin.test.Test

@Suppress("LongMethod")
class FunctionSchemaTest {
    @Test
    fun `should generate function schema`() {
        val functionCallSchema: JsonObject = sayHelloJsonSchema()
        val functionCallSchemaString: String = sayHelloJsonSchemaString()
        functionCallSchemaString shouldEqualJson
            """
            {
    "type": "function",
    "name": "sayHello",
    "description": "Greets the user with a personalized message.",
    "strict": true,
    "parameters": {
        "type": "object",
        "properties": {
            "name": {
                "type": [
                    "string",
                    "null"
                ],
                "description": "Name to greet"
            }
        },
        "required": [
            "name"
        ],
        "additionalProperties": false
    }
}
            """.trimIndent()

        Json.encodeToString(functionCallSchema) shouldEqualJson functionCallSchemaString
    }
}
