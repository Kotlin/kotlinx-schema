@file:Suppress("JsonStandardCompliance")

package kotlinx.schema.integration.kdoc

import io.kotest.assertions.json.shouldEqualJson
import kotlin.test.Test

/**
 * Integration tests for KDoc parameter and property description extraction.
 *
 * Tests verify that:
 * - Function parameters with @param KDoc tags get descriptions
 * - Class properties with @property KDoc tags get descriptions
 * - Multi-line parameter descriptions are correctly extracted
 * - Descriptions fallback from annotations -> property KDoc -> class KDoc
 */
class KDocParametersTest {
    @Test
    fun `searchUsers function extracts param descriptions from KDoc`() {
        val schema = searchUsersJsonSchemaString()

        // language=json
        schema shouldEqualJson
            """
            {
              "type": "function",
              "name": "searchUsers",
              "description": "Searches for users by name and age.",
              "strict": true,
              "parameters": {
                "type": "object",
                "properties": {
                  "name": {
                    "type": "string",
                    "description": "User name to search for"
                  },
                  "age": {
                    "type": ["integer", "null"],
                    "description": "User age filter"
                  },
                  "includeInactive": {
                    "type": "boolean",
                    "description": "Whether to include inactive users in results"
                  }
                },
                "required": ["name", "age", "includeInactive"],
                "additionalProperties": false
              }
            }
            """.trimIndent()
    }

    @Test
    fun `createUser function extracts multi-line param descriptions from KDoc`() {
        val schema = createUserJsonSchemaString()

        // language=json
        schema shouldEqualJson
            """
            {
              "type": "function",
              "name": "createUser",
              "description": "Creates a new user account.\nThis function creates a new user with the provided details.\nIt validates the email and ensures the username is unique.",
              "strict": true,
              "parameters": {
                "type": "object",
                "properties": {
                  "username": {
                    "type": "string",
                    "description": "Unique username for the account"
                  },
                  "email": {
                    "type": "string",
                    "description": "User's email address.\nMust be a valid email format."
                  },
                  "age": {
                    "type": "integer",
                    "description": "User's age in years"
                  }
                },
                "required": ["username", "email", "age"],
                "additionalProperties": false
              }
            }
            """.trimIndent()
    }

    @Test
    fun `UserProfile class extracts property descriptions from KDoc`() {
        val schema = UserProfile::class.jsonSchemaString

        // language=json
        schema shouldEqualJson
            $$"""
            {
              "$schema": "https://json-schema.org/draft/2020-12/schema",
              "$id": "kotlinx.schema.integration.kdoc.UserProfile",
              "description": "User profile with basic information.",
              "type": "object",
              "properties": {
                "id": {
                  "type": "string",
                  "description": "Unique user identifier"
                },
                "name": {
                  "type": "string",
                  "description": "Full name of the user"
                },
                "email": {
                  "type": "string",
                  "description": "Email address for notifications"
                }
              },
              "required": ["id", "name", "email"],
              "additionalProperties": false
            }
            """.trimIndent()
    }

    @Test
    fun `AppConfig class extracts property descriptions from KDoc with defaults`() {
        val schema = AppConfig::class.jsonSchemaString

        // language=json
        schema shouldEqualJson
            $$"""
            {
              "$schema": "https://json-schema.org/draft/2020-12/schema",
              "$id": "kotlinx.schema.integration.kdoc.AppConfig",
              "description": "Configuration for the application.",
              "type": "object",
              "properties": {
                "apiKey": {
                  "type": "string",
                  "description": "API key for authentication"
                },
                "timeout": {
                  "type": "integer",
                  "description": "Connection timeout in milliseconds"
                },
                "retries": {
                  "type": "integer",
                  "description": "Number of retry attempts"
                }
              },
              "required": ["apiKey", "timeout", "retries"],
              "additionalProperties": false
            }
            """.trimIndent()
    }

    @Test
    fun `Product class extracts descriptions from @param KDoc tags`() {
        val schema = Product::class.jsonSchemaString

        // language=json
        schema shouldEqualJson
            $$"""
            {
              "$schema": "https://json-schema.org/draft/2020-12/schema",
              "$id": "kotlinx.schema.integration.kdoc.Product",
              "description": "Product information.",
              "type": "object",
              "properties": {
                "name": {
                  "type": "string",
                  "description": "Product name"
                },
                "price": {
                  "type": "number",
                  "description": "Price in USD"
                },
                "category": {
                  "type": "string",
                  "description": "Product category"
                }
              },
              "required": ["name", "price", "category"],
              "additionalProperties": false
            }
            """.trimIndent()
    }

    @Test
    fun `updateUserSettings suspend function extracts param descriptions from KDoc`() {
        val schema = updateUserSettingsJsonSchemaString()

        // language=json
        schema shouldEqualJson
            """
            {
              "type": "function",
              "name": "updateUserSettings",
              "description": "Updates user settings.",
              "strict": true,
              "parameters": {
                "type": "object",
                "properties": {
                  "userId": {
                    "type": "string",
                    "description": "ID of the user to update"
                  },
                  "settings": {
                    "type": "object",
                    "additionalProperties": {
                      "type": "string"
                    },
                    "description": "Map of settings to update"
                  }
                },
                "required": ["userId", "settings"],
                "additionalProperties": false
              }
            }
            """.trimIndent()
    }

    @Test
    fun `ApiResponse demonstrates description fallback chain`() {
        val schema = ApiResponse::class.jsonSchemaString

        // language=json
        schema shouldEqualJson
            $$"""
            {
              "$schema": "https://json-schema.org/draft/2020-12/schema",
              "$id": "kotlinx.schema.integration.kdoc.ApiResponse",
              "description": "Response with mixed description sources.\nDemonstrates fallback chain: annotation > @param tag > @property tag",
              "type": "object",
              "properties": {
                "message": {
                  "type": "string",
                  "description": "Response message from annotation"
                },
                "statusCode": {
                  "type": "integer",
                  "description": "HTTP status code"
                },
                "data": {
                  "type": "string",
                  "description": "Response payload from @property tag"
                }
              },
              "required": ["message", "statusCode", "data"],
              "additionalProperties": false
            }
            """.trimIndent()
    }

    @Test
    fun `FieldsExample extracts descriptions from field KDoc and class KDoc`() {
        val schema = FieldsExample::class.jsonSchemaString

        // language=json
        schema shouldEqualJson
            $$"""
            {
              "$schema": "https://json-schema.org/draft/2020-12/schema",
              "$id": "kotlinx.schema.integration.kdoc.FieldsExample",
              "description": "Class with properties declared in body (fields).\nDemonstrates field-level KDoc extraction.",
              "type": "object",
              "properties": {
                "id": {
                  "type": "string",
                  "description": "Field with its own KDoc."
                },
                "name": {
                  "type": "string"
                },
                "computed": {
                  "type": "integer",
                  "description": "Computed from class KDoc @property tag"
                }
              },
              "required": ["id", "name", "computed"],
              "additionalProperties": false
            }
            """.trimIndent()
    }
}
