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
        val schema = registerUserJsonSchemaString()

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
        val schema = authenticateUserJsonSchemaString()

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
    fun `ProductRepository saveProduct suspend function generates correct schema`() {
        val schema = saveProductJsonSchemaString()

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
                  "id": {
                    "type": ["integer", "null"],
                    "description": "Product ID (null for new products)"
                  },
                  "name": {
                    "type": "string",
                    "description": "Product name"
                  },
                  "price": {
                    "type": "number",
                    "description": "Product price"
                  },
                  "stock": {
                    "type": "integer",
                    "description": "Stock quantity"
                  }
                },
                "required": ["id", "name", "price", "stock"],
                "additionalProperties": false
              }
            }
            """.trimIndent()
    }
}
