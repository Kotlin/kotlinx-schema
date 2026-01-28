package kotlinx.schema.generator.json

import io.kotest.assertions.json.shouldEqualJson
import kotlinx.serialization.json.Json
import kotlin.test.Test

class SealedHierarchyTest {
    @Test
    fun testExampleA() {
        val json =
            Json {
                encodeDefaults = false
                prettyPrint = true
            }
        val schema =
            ReflectionClassJsonSchemaGenerator(
                json =
                json,
                config = JsonSchemaConfig.Strict,
            ).generateSchema(ExampleA::class)
        val actual = json.encodeToString(schema)
        actual shouldEqualJson
            $$"""
                {
                "name": "kotlinx.schema.generator.json.ExampleA",
                "strict": true,
                "schema": {
                   "$schema": "https://json-schema.org/draft/2020-12/schema",
                   "$id": "kotlinx.schema.generator.json.ExampleA",
                   "type": "object",
                   "additionalProperties": false,
                   "oneOf": [
                     {
                       "$ref": "#/$defs/ExampleB"
                     }
                   ],
                   "$defs": {
                     "ExampleB": {
                       "type": "object",
                       "properties": {
                         "type": {
                           "const": "ExampleB",
                           "type": "string"
                         },
                         "someProp": {
                           "const": "default",
                           "type": "string"
                         }
                       },
                       "required": [
                         "type",
                         "someProp"
                       ],
                       "additionalProperties": false
                     }
                   }
                 }
             }
            """.trimIndent()
    }
}

@Suppress("unused")
sealed class ExampleA(
    val someProp: String,
) {
    data object ExampleB : ExampleA("default")
}
