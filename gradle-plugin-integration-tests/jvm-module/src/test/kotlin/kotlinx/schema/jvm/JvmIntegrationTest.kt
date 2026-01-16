@file:Suppress("JsonStandardCompliance")

package kotlinx.schema.jvm

import io.kotest.assertions.json.shouldEqualJson
import kotlinx.serialization.json.Json
import kotlin.test.Test

/**
 * Integration tests that verify KSP-generated extension properties work correctly
 */
class JvmIntegrationTest {
    @Test
    fun `Person class should have generated jsonSchema`() {
        // This tests that KSP successfully generated the extension property
        val schema = Person::class.jsonSchema
        val schemaString = Person::class.jsonSchemaString

        schemaString shouldEqualJson
            $$"""
            {
                "$id": "kotlinx.schema.jvm.Person",
                  "$defs": {
                    "kotlinx.schema.jvm.Person": {
                      "type": "object",
                      "properties": {
                        "name": {
                          "type": "string",
                          "description": "Full name"
                        },
                        "email": {
                          "type": "string",
                          "description": "Email address"
                        }
                      },
                      "required": [
                        "name",
                        "email"
                      ],
                      "additionalProperties": false,
                      "description": "Simple Person model for testing"
                    }
                  },
                  "$ref": "#/$defs/kotlinx.schema.jvm.Person"
            }
            """.trimIndent()

        Json.encodeToString(schema) shouldEqualJson schemaString
    }

    @Test
    fun `Company class should have generated jsonSchema`() {
        // This tests that KSP successfully generated the extension property
        val schema = Company::class.jsonSchema
        val schemaString = Company::class.jsonSchemaString

        schemaString shouldEqualJson
            $$"""
            {
              "$id": "kotlinx.schema.jvm.Company",
              "$defs": {
                "kotlinx.schema.jvm.Company": {
                  "type": "object",
                  "properties": {
                    "name": {
                      "type": "string",
                      "description": "Company name"
                    },
                    "employeeCount": {
                      "type": "integer",
                      "description": "Number of employees"
                    }
                  },
                  "required": [
                    "name",
                    "employeeCount"
                  ],
                  "additionalProperties": false,
                  "description": "Simple Company model for testing"
                }
              },
              "$ref": "#/$defs/kotlinx.schema.jvm.Company"
            }
            """.trimIndent()

        Json.encodeToString(schema) shouldEqualJson schemaString
    }
}
