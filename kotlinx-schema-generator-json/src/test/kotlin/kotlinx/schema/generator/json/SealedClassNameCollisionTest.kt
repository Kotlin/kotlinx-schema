package kotlinx.schema.generator.json

import io.kotest.assertions.json.shouldEqualJson
import kotlinx.schema.Description
import kotlinx.schema.generator.core.SchemaGeneratorService
import kotlinx.schema.json.JsonSchema
import kotlinx.serialization.json.Json
import org.junitpioneer.jupiter.Issue
import kotlin.reflect.KClass
import kotlin.test.Test

/**
 * Tests for handling name collisions in sealed class hierarchies.
 *
 * When multiple sealed classes have inner classes with the same name (e.g., "Unknown"),
 * the generated JSON Schema definitions must use qualified names like "ParentClass.ChildClass"
 * to avoid collisions in the $defs section.
 */
@Issue("https://github.com/Kotlin/kotlinx-schema/issues/113")
class SealedClassNameCollisionTest {
    @Description("Result type A")
    @Suppress("unused")
    sealed class ResultA {
        @Description("Success result for A")
        data class Success(
            val value: String,
        ) : ResultA()

        @Description("Unknown error for A")
        data class Unknown(
            val code: Int,
        ) : ResultA()
    }

    @Description("Result type B")
    @Suppress("unused")
    sealed class ResultB {
        @Description("Success result for B")
        data class Success(
            val data: Int,
        ) : ResultB()

        @Description("Unknown error for B")
        data class Unknown(
            val message: String,
        ) : ResultB()
    }

    @Description("Container with both result types")
    data class ApiResponse(
        val resultA: ResultA,
        val resultB: ResultB,
    )

    private val generator =
        requireNotNull(
            SchemaGeneratorService.getGenerator(KClass::class, JsonSchema::class),
        ) {
            "ReflectionClassJsonSchemaGenerator must be registered"
        }

    private val json =
        Json {
            prettyPrint = true
            encodeDefaults = false
        }

    @Test
    @Suppress("LongMethod")
    fun `Should use qualified names to avoid name collisions in sealed hierarchies`() {
        // When - generate schema for ApiResponse which uses two sealed hierarchies
        //        where both have "Success" and "Unknown" inner classes
        val schema = generator.generateSchema(ApiResponse::class)

        // Then - all 4 definitions should be present with qualified names
        //       (not 2 due to collision: "Success" and "Unknown")
        json.encodeToString(schema) shouldEqualJson
            $$"""
            {
              "name": "$${ApiResponse::class.qualifiedName}",
              "strict": false,
              "schema": {
                "type": "object",
                "properties": {
                  "resultA": {
                    "oneOf": [
                      { "$ref": "#/$defs/ResultA.Success" },
                      { "$ref": "#/$defs/ResultA.Unknown" }
                    ],
                    "discriminator": {
                      "propertyName": "type",
                      "mapping": {
                        "Success": "#/$defs/ResultA.Success",
                        "Unknown": "#/$defs/ResultA.Unknown"
                      }
                    },
                    "description": "Result type A"
                  },
                  "resultB": {
                    "oneOf": [
                      { "$ref": "#/$defs/ResultB.Success" },
                      { "$ref": "#/$defs/ResultB.Unknown" }
                    ],
                    "discriminator": {
                      "propertyName": "type",
                      "mapping": {
                        "Success": "#/$defs/ResultB.Success",
                        "Unknown": "#/$defs/ResultB.Unknown"
                      }
                    },
                    "description": "Result type B"
                  }
                },
                "required": ["resultA", "resultB"],
                "additionalProperties": false,
                "description": "Container with both result types",
                "$defs": {
                  "ResultA.Success": {
                    "type": "object",
                    "description": "Success result for A",
                    "properties": {
                      "type": { "type": "string", "default": "ResultA.Success" },
                      "value": { "type": "string" }
                    },
                    "required": ["type", "value"],
                    "additionalProperties": false
                  },
                  "ResultA.Unknown": {
                    "type": "object",
                    "description": "Unknown error for A",
                    "properties": {
                      "type": { "type": "string", "default": "ResultA.Unknown" },
                      "code": { "type": "integer" }
                    },
                    "required": ["type", "code"],
                    "additionalProperties": false
                  },
                  "ResultB.Success": {
                    "type": "object",
                    "description": "Success result for B",
                    "properties": {
                      "type": { "type": "string", "default": "ResultB.Success" },
                      "data": { "type": "integer" }
                    },
                    "required": ["type", "data"],
                    "additionalProperties": false
                  },
                  "ResultB.Unknown": {
                    "type": "object",
                    "description": "Unknown error for B",
                    "properties": {
                      "type": { "type": "string", "default": "ResultB.Unknown" },
                      "message": { "type": "string" }
                    },
                    "required": ["type", "message"],
                    "additionalProperties": false
                  }
                }
              }
            }
            """.trimIndent()
    }
}
