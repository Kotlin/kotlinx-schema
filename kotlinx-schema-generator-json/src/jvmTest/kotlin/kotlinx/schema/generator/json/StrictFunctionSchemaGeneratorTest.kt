@file:Suppress("FunctionOnlyReturningConstant", "UnusedParameter", "unused")

package kotlinx.schema.generator.json

import io.kotest.assertions.json.shouldEqualJson
import kotlinx.schema.Description
import kotlinx.serialization.json.Json
import kotlin.test.Test

/**
 * Tests for [ReflectionFunctionCallingSchemaGenerator] with [FunctionCallingSchemaConfig.Strict].
 *
 * Strict preset uses `respectDefaultPresence = false` + `requireNullableFields = true` + `strictMode = true`:
 * all parameters are required regardless of Kotlin default values, no `default` or `const` keyword
 * is emitted for required non-constant parameters, and `"strict": true` is included in the output.
 */
class StrictFunctionSchemaGeneratorTest {
    private val strictGenerator =
        ReflectionFunctionCallingSchemaGenerator(
            json = Json { encodeDefaults = false },
            config = FunctionCallingSchemaConfig.Strict,
        )

    //region Test fixtures

    object MixedParamsObject {
        @Suppress("LongParameterList")
        @Description("Mixed required and optional parameters")
        fun mixedParams(
            @Description("Required string")
            req1: String,
            @Description("Required int")
            req2: Int,
            @Description("Optional string")
            opt1: String? = null,
            @Description("Optional int")
            opt2: Int? = null,
            @Description("Default string")
            def1: String = "default",
            @Description("Default int")
            def2: Int = 42,
        ): String = "$req1 $req2"
    }

    object SimpleParams {
        @Description("Simple function with no defaults")
        fun greet(
            @Description("First name")
            firstName: String,
            @Description("Last name")
            lastName: String,
        ): String = "$firstName $lastName"
    }

    //endregion

    //region Test cases

    @Test
    fun `all parameters required, no default or const emitted for required non-constant parameters`() {
        val schema = strictGenerator.generateSchemaString(MixedParamsObject::mixedParams)

        // language=JSON
        schema shouldEqualJson
            $$"""
            {
              "type": "function",
              "name": "mixedParams",
              "description": "Mixed required and optional parameters",
              "strict": true,
              "parameters": {
                "type": "object",
                "properties": {
                  "req1": { "type": "string",            "description": "Required string" },
                  "req2": { "type": "integer",           "description": "Required int"    },
                  "opt1": { "type": ["string",  "null"], "description": "Optional string" },
                  "opt2": { "type": ["integer", "null"], "description": "Optional int"    },
                  "def1": { "type": "string",            "description": "Default string"  },
                  "def2": { "type": "integer",           "description": "Default int"     }
                },
                "required": ["req1", "req2", "opt1", "opt2", "def1", "def2"],
                "additionalProperties": false
              }
            }
            """.trimIndent()
    }

    @Test
    fun `simple function with no defaults requires all parameters`() {
        val schema = strictGenerator.generateSchemaString(SimpleParams::greet)

        // language=JSON
        schema shouldEqualJson
            $$"""
            {
              "type": "function",
              "name": "greet",
              "description": "Simple function with no defaults",
              "strict": true,
              "parameters": {
                "type": "object",
                "properties": {
                  "firstName": { "type": "string", "description": "First name" },
                  "lastName":  { "type": "string", "description": "Last name"  }
                },
                "required": ["firstName", "lastName"],
                "additionalProperties": false
              }
            }
            """.trimIndent()
    }

    //endregion
}
