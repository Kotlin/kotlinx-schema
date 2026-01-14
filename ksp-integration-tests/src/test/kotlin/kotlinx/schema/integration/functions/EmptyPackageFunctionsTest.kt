package kotlinx.schema.integration.functions

import EmptyPackageFunctionClass
import EmptyPackageFunctionObject
import classFunctionJsonSchema
import classFunctionJsonSchemaString
import companionFunctionJsonSchema
import companionFunctionJsonSchemaString
import emptyPackageTopLevelGetRandomJsonSchema
import emptyPackageTopLevelGetRandomJsonSchemaString
import io.kotest.assertions.json.shouldEqualJson
import kotlinx.serialization.json.Json
import objectFunctionJsonSchema
import objectFunctionJsonSchemaString
import kotlin.test.Test

class EmptyPackageFunctionsTest {
    @Test
    fun `Should generate top-level function schema in empty package`() {
        val schema = emptyPackageTopLevelGetRandomJsonSchema()
        val schemaString = emptyPackageTopLevelGetRandomJsonSchemaString()

        Json.encodeToString(schema) shouldEqualJson schemaString
        schemaString shouldEqualJson
            """
            {
              "type": "function",
              "name": "emptyPackageTopLevelGetRandom",
              "description": "Generates a random integer using a seeded random number generator.",
              "strict": true,
              "parameters": {
                "type": "object",
                "properties": {
                    "seed": {
                        "type": [
                          "integer",
                          "null"
                        ],
                        "description": "Random number generator seeded"
                    }
                },
                "required": [
                  "seed"
                ],
                "additionalProperties": false
              }
            }
            """.trimIndent()
    }

    @Test
    fun `Should generate class function schema in empty package`() {
        val schema = EmptyPackageFunctionClass::class.classFunctionJsonSchema()
        val schemaString = EmptyPackageFunctionClass::class.classFunctionJsonSchemaString()

        Json.encodeToString(schema) shouldEqualJson schemaString
        schemaString shouldEqualJson
            """
            {"type":"function","name":"classFunction","description":"","strict":true,
            "parameters":{"type":"object","properties":{},"required":[],"additionalProperties":false}}
            """.trimIndent()
    }

    @Test
    fun `Should generate object function schema in empty package`() {
        val schema = EmptyPackageFunctionObject::class.objectFunctionJsonSchema()
        val schemaString = EmptyPackageFunctionObject::class.objectFunctionJsonSchemaString()

        Json.encodeToString(schema) shouldEqualJson schemaString
        schemaString shouldEqualJson
            """
            {"type":"function","name":"objectFunction","description":"","strict":true,
            "parameters":{"type":"object","properties":{},"required":[],"additionalProperties":false}}
            """.trimIndent()
    }

    @Test
    fun `Should generate companion object function schema in empty package`() {
        val schema = EmptyPackageFunctionClass.Companion::class.companionFunctionJsonSchema()
        val schemaString = EmptyPackageFunctionClass.Companion::class.companionFunctionJsonSchemaString()

        Json.encodeToString(schema) shouldEqualJson schemaString
        schemaString shouldEqualJson
            """
            {"type":"function","name":"companionFunction","description":"","strict":true,
            "parameters":{"type":"object","properties":{},"required":[],"additionalProperties":false}}
            """.trimIndent()
    }
}
