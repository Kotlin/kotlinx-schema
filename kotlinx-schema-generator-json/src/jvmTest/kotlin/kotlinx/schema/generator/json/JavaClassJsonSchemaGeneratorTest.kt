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
        val actualSchema = generator.generateSchemaString(JavaTestClass.CLASS.kotlin)

        // language=JSON
        val expectedSchema =
            $$"""
            {
              "$schema": "https://json-schema.org/draft/2020-12/schema",
              "$id": "kotlinx.schema.generator.test.JavaTestClass",
              "type": "object",
              "properties": {
                "stringProperty": {
                  "type": "string",
                  "description": "A string property"
                },
                "intProperty": {
                  "type": "integer"
                },
                "longProperty": {
                  "type": "integer"
                },
                "doubleProperty": {
                  "type": "number"
                },
                "floatProperty": {
                  "type": "number"
                },
                "booleanNullableProperty": {
                  "type": "boolean"
                },
                "nullableProperty": {
                  "type": "string"
                },
                "listProperty": {
                  "type": "array",
                  "items": {
                    "type": "string"
                  }
                },
                "mapProperty": {
                  "type": "object",
                  "additionalProperties": {
                    "type": "integer"
                  }
                },
                "nestedProperty": {
                  "type": "object",
                  "description": "Nested property class",
                  "properties": {
                    "foo": {
                      "type": "string",
                      "description": "Nested foo property"
                    },
                    "bar": {
                      "type": "integer"
                    }
                  },
                  "required": [
                    "foo",
                    "bar"
                  ],
                  "additionalProperties": false
                },
                "nestedListProperty": {
                  "type": "array",
                  "items": {
                    "type": "object",
                    "description": "Nested property class",
                    "properties": {
                      "foo": {
                        "type": "string",
                        "description": "Nested foo property"
                      },
                      "bar": {
                        "type": "integer"
                      }
                    },
                    "required": [
                      "foo",
                      "bar"
                    ],
                    "additionalProperties": false
                  }
                },
                "nestedMapProperty": {
                  "type": "object",
                  "additionalProperties": {
                    "type": "object",
                    "description": "Nested property class",
                    "properties": {
                      "foo": {
                        "type": "string",
                        "description": "Nested foo property"
                      },
                      "bar": {
                        "type": "integer"
                      }
                    },
                    "required": [
                      "foo",
                      "bar"
                    ],
                    "additionalProperties": false
                  }
                },
                "enumProperty": {
                  "type": "string",
                  "enum": [
                    "One",
                    "Two"
                  ]
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
                "enumProperty"
              ]
            } 
            """.trimIndent()

        actualSchema shouldEqualJson expectedSchema
    }
}
