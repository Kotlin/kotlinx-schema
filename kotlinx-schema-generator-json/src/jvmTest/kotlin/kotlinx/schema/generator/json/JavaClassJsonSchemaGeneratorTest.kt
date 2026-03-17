@file:Suppress("LongMethod")

package kotlinx.schema.generator.json

import io.kotest.assertions.json.shouldEqualJson
import kotlinx.schema.generator.test.JavaTestClass
import kotlinx.serialization.json.Json
import kotlin.test.Test

class JavaClassJsonSchemaGeneratorTest {
    private val generator =
        ReflectionClassJsonSchemaGenerator(
            json = Json { prettyPrint = true },
            config = JsonSchemaConfig.Default,
        )

    @Test
    fun `Should generate JsonSchema from Java class`() {
        // External Java class
        val actualSchema = generator.generateSchemaString(JavaTestClass::class)

        // language=JSON
        val expectedSchema =
            $$"""
            {
              "$schema": "https://json-schema.org/draft/2020-12/schema",
              "$id": "kotlinx.schema.generator.test.JavaTestClass",
              "description": "Class Description",
              "type": "object",
              "properties": {
                "stringProperty": {
                  "type": "string",
                  "description": "A string property"
                },
                "intProperty": {
                  "type": "integer",
                  "description": "An int property"
                },
                "longProperty": {
                  "type": "integer",
                  "description": "A long property"
                },
                "doubleProperty": {
                  "type": "number",
                  "description": "A double property"
                },
                "floatProperty": {
                  "type": "number",
                  "description": "A float property"
                },
                "booleanNullableProperty": {
                  "type": "boolean",
                  "description": "A nullable boolean property"
                },
                "nullableProperty": {
                  "type": "string",
                  "description": "A nullable string property"
                },
                "listProperty": {
                  "type": "array",
                  "description": "A list of strings",
                  "items": {
                    "type": "string"
                  }
                },
                "mapProperty": {
                  "type": "object",
                  "description": "A map of integers",
                  "additionalProperties": {
                    "type": "integer"
                  }
                },
                "nestedProperty": {
                  "$ref": "#/$defs/kotlinx.schema.generator.test.JavaTestClass.NestedProperty",
                  "description": "A nested property"
                },
                "nestedListProperty": {
                  "type": "array",
                  "description": "A list of nested properties",
                  "items": {
                    "$ref": "#/$defs/kotlinx.schema.generator.test.JavaTestClass.NestedProperty"
                  }
                },
                "nestedMapProperty": {
                  "type": "object",
                  "description": "A map of nested properties",
                  "additionalProperties": {
                    "$ref": "#/$defs/kotlinx.schema.generator.test.JavaTestClass.NestedProperty"
                  }
                },
                "enumProperty": {
                  "$ref": "#/$defs/kotlinx.schema.generator.test.JavaTestClass.TestEnum",
                  "description": "An enum property"
                },
                "recordProperty": {
                  "$ref": "#/$defs/kotlinx.schema.generator.test.JavaTestClass.ProblemDescription",
                  "description": "Simple record property"
                }
              },
              "additionalProperties": false,
              "required": [
                "stringProperty",
                "intProperty",
                "longProperty",
                "doubleProperty",
                "floatProperty",
                "booleanNullableProperty",
                "nullableProperty",
                "listProperty",
                "mapProperty",
                "nestedProperty",
                "nestedListProperty",
                "nestedMapProperty",
                "enumProperty",
                "recordProperty"
              ],
              "$defs": {
                "kotlinx.schema.generator.test.JavaTestClass.NestedProperty": {
                  "type": "object",
                  "description": "Nested property class",
                  "properties": {
                    "foo": { "type": "string", "description": "Nested foo property" },
                    "bar": { "type": "integer" }
                  },
                  "required": ["foo", "bar"],
                  "additionalProperties": false
                },
                "kotlinx.schema.generator.test.JavaTestClass.TestEnum": {
                  "type": "string",
                  "enum": ["One", "Two"]
                },
                "kotlinx.schema.generator.test.JavaTestClass.ProblemDescription": {
                  "type": "object",
                  "description": "Record description",
                  "properties": {
                    "description": {
                      "type": "string",
                      "description": "String property"
                    }
                  },
                  "required": [
                    "description"
                  ],
                  "additionalProperties": false
                }
              }
            }
            """.trimIndent()

        actualSchema shouldEqualJson expectedSchema
    }
}
