package kotlinx.schema.integration.functions

import io.kotest.assertions.json.shouldEqualJson
import kotlin.test.Test

/**
 * Integration tests for top-level function schema generation.
 *
 * Tests verify that:
 * - Normal top-level functions generate correct schemas
 * - Suspend top-level functions generate correct schemas
 * - Function names, descriptions, and parameters are correctly captured
 * - Default values are properly indicated
 * - Nullable parameters are correctly represented
 */
class TopLevelFunctionsTest {
    @Test
    fun `greetPerson generates correct function schema`() {
        val schema = greetPersonJsonSchemaString()

        // language=json
        schema shouldEqualJson
            """
            {
              "type": "function",
              "name": "greetPerson",
              "description": "Sends a greeting message to a person",
              "strict": true,
              "parameters": {
                "type": "object",
                "properties": {
                  "name": {
                    "type": "string",
                    "description": "Name of the person to greet"
                  },
                  "greeting": {
                    "type": "string",
                    "description": "Optional greeting prefix (e.g., 'Hello', 'Hi')"
                  }
                },
                "required": ["name", "greeting"],
                "additionalProperties": false
              }
            }
            """.trimIndent()
    }

    @Test
    fun `calculateSum generates correct function schema`() {
        val schema = calculateSumJsonSchemaString()

        // language=json
        schema shouldEqualJson
            """
            {
              "type": "function",
              "name": "calculateSum",
              "description": "Calculates the sum of two numbers",
              "strict": true,
              "parameters": {
                "type": "object",
                "properties": {
                  "a": {
                    "type": "integer",
                    "description": "First number"
                  },
                  "b": {
                    "type": "integer",
                    "description": "Second number"
                  }
                },
                "required": ["a", "b"],
                "additionalProperties": false
              }
            }
            """.trimIndent()
    }

    @Test
    fun `fetchUserData suspend function generates correct schema`() {
        val schema = fetchUserDataJsonSchemaString()

        // language=json
        schema shouldEqualJson
            """
            {
              "type": "function",
              "name": "fetchUserData",
              "description": "Fetches user data asynchronously from a remote service",
              "strict": true,
              "parameters": {
                "type": "object",
                "properties": {
                  "userId": {
                    "type": "integer",
                    "description": "User ID to fetch"
                  },
                  "includeDetails": {
                    "type": "boolean",
                    "description": "Whether to include detailed profile information"
                  }
                },
                "required": ["userId", "includeDetails"],
                "additionalProperties": false
              }
            }
            """.trimIndent()
    }

    @Test
    fun `processItems suspend function generates correct schema`() {
        val schema = processItemsJsonSchemaString()

        // language=json
        schema shouldEqualJson
            """
            {
              "type": "function",
              "name": "processItems",
              "description": "Processes a list of items asynchronously",
              "strict": true,
              "parameters": {
                "type": "object",
                "properties": {
                  "items": {
                    "type": "array",
                    "items": {
                      "type": "string"
                    },
                    "description": "List of items to process"
                  },
                  "mode": {
                    "type": "string",
                    "description": "Processing mode"
                  }
                },
                "required": ["items", "mode"],
                "additionalProperties": false
              }
            }
            """.trimIndent()
    }

    @Test
    fun `searchProducts with nullable parameters generates correct schema`() {
        val schema = searchProductsJsonSchemaString()

        // language=json
        schema shouldEqualJson
            """
            {
              "type": "function",
              "name": "searchProducts",
              "description": "Searches for products with various filters",
              "strict": true,
              "parameters": {
                "type": "object",
                "properties": {
                  "query": {
                    "type": "string",
                    "description": "Search query string"
                  },
                  "minPrice": {
                    "type": ["number", "null"],
                    "description": "Minimum price filter"
                  },
                  "maxPrice": {
                    "type": ["number", "null"],
                    "description": "Maximum price filter"
                  },
                  "categories": {
                    "type": "array",
                    "items": {
                      "type": "string"
                    },
                    "description": "Category filters"
                  },
                  "includeOutOfStock": {
                    "type": "boolean",
                    "description": "Whether to include out-of-stock items"
                  }
                },
                "required": ["query", "minPrice", "maxPrice", "categories", "includeOutOfStock"],
                "additionalProperties": false
              }
            }
            """.trimIndent()
    }

    @Test
    fun `updateConfiguration suspend function with map parameter generates correct schema`() {
        val schema = updateConfigurationJsonSchemaString()

        // language=json
        schema shouldEqualJson
            """
            {
              "type": "function",
              "name": "updateConfiguration",
              "description": "Updates configuration settings",
              "strict": true,
              "parameters": {
                "type": "object",
                "properties": {
                  "key": {
                    "type": "string",
                    "description": "Configuration key"
                  },
                  "value": {
                    "type": "string",
                    "description": "Configuration value"
                  },
                  "metadata": {
                    "type": ["object", "null"],
                    "additionalProperties": {
                      "type": "string"
                    },
                    "description": "Optional metadata"
                  }
                },
                "required": ["key", "value", "metadata"],
                "additionalProperties": false
              }
            }
            """.trimIndent()
    }
}
