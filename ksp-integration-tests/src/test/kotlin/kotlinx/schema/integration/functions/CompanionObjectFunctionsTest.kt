package kotlinx.schema.integration.functions

import io.kotest.assertions.json.shouldEqualJson
import kotlin.test.Test

/**
 * Integration tests for companion object function schema generation.
 *
 * Tests verify that:
 * - Normal companion object functions generate correct schemas
 * - Suspend companion object functions generate correct schemas
 * - Multiple companion objects with functions can coexist
 * - Factory pattern functions work correctly
 */
class CompanionObjectFunctionsTest {
    @Test
    fun `DatabaseConnection create generates correct schema`() {
        val schema = DatabaseConnection.Companion::class.createJsonSchemaString()

        // language=json
        schema shouldEqualJson
            """
            {
              "type": "function",
              "name": "create",
              "description": "Creates a new database connection",
              "strict": true,
              "parameters": {
                "type": "object",
                "properties": {
                  "host": {
                    "type": "string",
                    "description": "Database host"
                  },
                  "port": {
                    "type": "integer",
                    "description": "Database port"
                  },
                  "database": {
                    "type": "string",
                    "description": "Database name"
                  },
                  "timeout": {
                    "type": "integer",
                    "description": "Connection timeout in seconds"
                  }
                },
                "required": ["host", "port", "database", "timeout"],
                "additionalProperties": false
              }
            }
            """.trimIndent()
    }

    @Test
    fun `DatabaseConnection connectAsync suspend function generates correct schema`() {
        val schema = DatabaseConnection.Companion::class.connectAsyncJsonSchemaString()

        // language=json
        schema shouldEqualJson
            """
            {
              "type": "function",
              "name": "connectAsync",
              "description": "Establishes a database connection asynchronously",
              "strict": true,
              "parameters": {
                "type": "object",
                "properties": {
                  "host": {
                    "type": "string",
                    "description": "Database host"
                  },
                  "port": {
                    "type": "integer",
                    "description": "Database port"
                  },
                  "username": {
                    "type": "string",
                    "description": "Username"
                  },
                  "password": {
                    "type": "string",
                    "description": "Password"
                  },
                  "options": {
                    "type": ["object", "null"],
                    "additionalProperties": {
                      "type": "string"
                    },
                    "description": "Connection options"
                  }
                },
                "required": ["host", "port", "username", "password", "options"],
                "additionalProperties": false
              }
            }
            """.trimIndent()
    }

    @Test
    fun `ApiClient build generates correct schema`() {
        val schema = ApiClient.Companion::class.buildJsonSchemaString()

        // language=json
        schema shouldEqualJson
            """
            {
              "type": "function",
              "name": "build",
              "description": "Builds an API client with configuration",
              "strict": true,
              "parameters": {
                "type": "object",
                "properties": {
                  "baseUrl": {
                    "type": "string",
                    "description": "API base URL"
                  },
                  "apiKey": {
                    "type": "string",
                    "description": "API key for authentication"
                  },
                  "timeout": {
                    "type": "integer",
                    "description": "Request timeout in milliseconds"
                  },
                  "debug": {
                    "type": "boolean",
                    "description": "Whether to enable debug logging"
                  },
                  "headers": {
                    "type": "object",
                    "additionalProperties": {
                      "type": "string"
                    },
                    "description": "Custom headers"
                  }
                },
                "required": ["baseUrl", "apiKey", "timeout", "debug", "headers"],
                "additionalProperties": false
              }
            }
            """.trimIndent()
    }

    @Test
    fun `ApiClient initializeAsync suspend function generates correct schema`() {
        val schema = ApiClient.Companion::class.initializeAsyncJsonSchemaString()

        // language=json
        schema shouldEqualJson
            """
            {
              "type": "function",
              "name": "initializeAsync",
              "description": "Initializes API client asynchronously with health check",
              "strict": true,
              "parameters": {
                "type": "object",
                "properties": {
                  "baseUrl": {
                    "type": "string",
                    "description": "API base URL"
                  },
                  "apiKey": {
                    "type": "string",
                    "description": "API key"
                  },
                  "verifySSL": {
                    "type": "boolean",
                    "description": "Whether to verify SSL certificates"
                  }
                },
                "required": ["baseUrl", "apiKey", "verifySSL"],
                "additionalProperties": false
              }
            }
            """.trimIndent()
    }

    @Test
    fun `DataValidator validateAsync suspend function generates correct schema`() {
        val schema = DataValidator.Companion::class.validateAsyncJsonSchemaString()

        // language=json
        schema shouldEqualJson
            """
            {
              "type": "function",
              "name": "validateAsync",
              "description": "Validates data asynchronously with remote API checks",
              "strict": true,
              "parameters": {
                "type": "object",
                "properties": {
                  "data": {
                    "type": "object",
                    "additionalProperties": {
                      "type": "string"
                    },
                    "description": "Data to validate"
                  },
                  "endpoints": {
                    "type": "array",
                    "items": {
                      "type": "string"
                    },
                    "description": "Remote validation endpoints"
                  },
                  "timeout": {
                    "type": "integer",
                    "description": "Timeout for remote checks in milliseconds"
                  }
                },
                "required": ["data", "endpoints", "timeout"],
                "additionalProperties": false
              }
            }
            """.trimIndent()
    }
}
