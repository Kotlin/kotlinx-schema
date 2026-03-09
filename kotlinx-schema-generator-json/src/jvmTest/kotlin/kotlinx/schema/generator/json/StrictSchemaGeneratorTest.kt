package kotlinx.schema.generator.json

import io.kotest.assertions.json.shouldEqualJson
import kotlinx.serialization.json.Json
import kotlin.test.Test

/**
 * Tests for [ReflectionClassJsonSchemaGenerator] with [JsonSchemaConfig.Strict].
 *
 * Strict preset uses `respectDefaultPresence = false` + `requireNullableFields = true`:
 * all fields are required regardless of Kotlin default values, and no `default` or `const`
 * keyword is emitted for required non-constant fields.
 */
class StrictSchemaGeneratorTest {
    private val strictGenerator =
        ReflectionClassJsonSchemaGenerator(
            json = Json { encodeDefaults = false },
            config = JsonSchemaConfig.Strict,
        )

    //region Test cases

    @Test
    fun `all fields required, no default or const emitted for required non-constant fields`() {
        val schema = strictGenerator.generateSchemaString(MixedRequiredOptional::class)

        // language=JSON
        schema shouldEqualJson
            $$"""
            {
              "$schema": "https://json-schema.org/draft/2020-12/schema",
              "$id": "kotlinx.schema.generator.json.MixedRequiredOptional",
              "description": "Mixed required and optional",
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
            """.trimIndent()
    }

    @Test
    fun `simple class with no defaults requires all fields`() {
        val schema = strictGenerator.generateSchemaString(Address::class)

        // language=JSON
        schema shouldEqualJson
            $$"""
            {
              "$schema": "https://json-schema.org/draft/2020-12/schema",
              "$id": "kotlinx.schema.generator.json.Address",
              "description": "Nested object",
              "type": "object",
              "properties": {
                "street": { "type": "string", "description": "Street name" },
                "city":   { "type": "string", "description": "City name"   }
              },
              "required": ["street", "city"],
              "additionalProperties": false
            }
            """.trimIndent()
    }

    //endregion
}
