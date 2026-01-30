package kotlinx.schema.integration.functions

import io.kotest.assertions.json.shouldEqualJson
import kotlin.test.Test

/**
 * Integration tests for instance (member) function schema generation.
 *
 * Tests verify that:
 * - Normal instance functions generate correct schemas
 * - Suspend instance functions generate correct schemas
 * - Schemas don't include implicit 'this' parameter
 * - Multiple instance functions can coexist in same class
 */
class InstanceFunctionsTest {
    @Test
    fun `UserService registerUser generates correct schema`() {
        val schema = UserService::class.registerUserJsonSchemaString()

        // language=json
        schema shouldEqualJson
            """
            {
              "type": "function",
              "name": "registerUser",
              "description": "Registers a new user in the system",
              "strict": true,
              "parameters": {
                "type": "object",
                "properties": {
                  "username": {
                    "type": "string",
                    "description": "Username for the new account"
                  },
                  "email": {
                    "type": "string",
                    "description": "Email address"
                  },
                  "age": {
                    "type": ["integer", "null"],
                    "description": "User's age"
                  },
                  "sendWelcomeEmail": {
                    "type": "boolean",
                    "description": "Whether to send welcome email"
                  }
                },
                "required": ["username", "email", "age", "sendWelcomeEmail"],
                "additionalProperties": false
              }
            }
            """.trimIndent()
    }

    @Test
    fun `UserService authenticateUser suspend function generates correct schema`() {
        val schema = UserService::class.authenticateUserJsonSchemaString()

        // language=json
        schema shouldEqualJson
            """
            {
              "type": "function",
              "name": "authenticateUser",
              "description": "Authenticates a user asynchronously",
              "strict": true,
              "parameters": {
                "type": "object",
                "properties": {
                  "identifier": {
                    "type": "string",
                    "description": "Username or email"
                  },
                  "password": {
                    "type": "string",
                    "description": "Password"
                  },
                  "rememberMe": {
                    "type": "boolean",
                    "description": "Remember session"
                  }
                },
                "required": ["identifier", "password", "rememberMe"],
                "additionalProperties": false
              }
            }
            """.trimIndent()
    }

    @Test
    fun `function with complex type parameter`() {
        val schema = ProductRepository::class.saveProductJsonSchemaString()

        // language=json
        schema shouldEqualJson
            """
            {
              "type": "function",
              "name": "saveProduct",
              "description": "Saves a product to the database asynchronously",
              "strict": true,
              "parameters": {
                "type": "object",
                "properties": {
                  "product": {
                    "type": "object",
                    "description": "Product to save",
                    "properties": {
                      "id": {
                        "type": "integer",
                        "description": "Unique identifier for the product"
                      },
                      "name": {
                        "type": "string",
                        "description": "Human-readable product name"
                      },
                      "description": {
                        "type": ["string", "null"],
                        "description": "Optional detailed description of the product"
                      },
                      "price": {
                        "type": "number",
                        "description": "Unit price expressed as a decimal number"
                      },
                      "inStock": {
                        "type": "boolean",
                        "description": "Whether the product is currently in stock"
                      },
                      "tags": {
                        "type": "array",
                        "description": "List of tags for categorization and search",
                        "items": {
                          "type": "string"
                        }
                      }
                    },
                    "required": ["id", "name", "description", "price", "inStock", "tags"],
                    "additionalProperties": false
                  }
                },
                "required": ["product"],
                "additionalProperties": false
              }
            }
            """.trimIndent()
    }
}
