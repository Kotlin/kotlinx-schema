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
              "$schema": "https://json-schema.org/draft/2020-12/schema",
              "$id": "kotlinx.schema.jvm.Person",
              "description": "Simple Person model for testing",
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
              "additionalProperties": false
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
              "$schema": "https://json-schema.org/draft/2020-12/schema",
              "$id": "kotlinx.schema.jvm.Company",
              "description": "Simple Company model for testing",
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
              "additionalProperties": false
            }
            """.trimIndent()

        Json.encodeToString(schema) shouldEqualJson schemaString
    }
}
