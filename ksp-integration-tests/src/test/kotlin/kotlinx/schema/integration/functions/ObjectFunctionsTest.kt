package kotlinx.schema.integration.functions

import io.kotest.assertions.json.shouldEqualJson
import kotlin.test.Test

/**
 * Integration tests for object (singleton) function schema generation.
 *
 * Tests verify that:
 * - Normal object functions generate correct schemas
 * - Suspend object functions generate correct schemas
 * - Multiple objects with functions can coexist
 * - Object functions are accessible as top-level functions
 */
class ObjectFunctionsTest {
    @Test
    fun `ConfigurationManager loadConfig generates correct schema`() {
        val schema = loadConfigJsonSchemaString()

        // language=json
        schema shouldEqualJson
            """
            {
              "type": "function",
              "name": "loadConfig",
              "description": "Loads configuration from a file",
              "strict": true,
              "parameters": {
                "type": "object",
                "properties": {
                  "filePath": {
                    "type": "string",
                    "description": "Configuration file path"
                  },
                  "createIfMissing": {
                    "type": "boolean",
                    "description": "Whether to create file if it doesn't exist"
                  },
                  "defaults": {
                    "type": ["object", "null"],
                    "additionalProperties": {
                      "type": "string"
                    },
                    "description": "Default values to use"
                  }
                },
                "required": ["filePath", "createIfMissing", "defaults"],
                "additionalProperties": false
              }
            }
            """.trimIndent()
    }

    @Test
    fun `ConfigurationManager validateConfig suspend function generates correct schema`() {
        val schema = validateConfigJsonSchemaString()

        // language=json
        schema shouldEqualJson
            """
            {
              "type": "function",
              "name": "validateConfig",
              "description": "Validates configuration against schema asynchronously",
              "strict": true,
              "parameters": {
                "type": "object",
                "properties": {
                  "config": {
                    "type": "object",
                    "additionalProperties": {
                      "type": "string"
                    },
                    "description": "Configuration to validate"
                  },
                  "schemaVersion": {
                    "type": "string",
                    "description": "Schema version"
                  }
                },
                "required": ["config", "schemaVersion"],
                "additionalProperties": false
              }
            }
            """.trimIndent()
    }

    @Test
    fun `LogManager writeLog generates correct schema`() {
        val schema = writeLogJsonSchemaString()

        // language=json
        schema shouldEqualJson
            """
            {
              "type": "function",
              "name": "writeLog",
              "description": "Writes a log entry",
              "strict": true,
              "parameters": {
                "type": "object",
                "properties": {
                  "level": {
                    "type": "string",
                    "description": "Log level (DEBUG, INFO, WARN, ERROR)"
                  },
                  "message": {
                    "type": "string",
                    "description": "Log message"
                  },
                  "exception": {
                    "type": ["string", "null"],
                    "description": "Optional exception details"
                  },
                  "tags": {
                    "type": "array",
                    "items": {
                      "type": "string"
                    },
                    "description": "Additional context tags"
                  }
                },
                "required": ["level", "message", "exception", "tags"],
                "additionalProperties": false
              }
            }
            """.trimIndent()
    }

    @Test
    fun `LogManager archiveLogs suspend function generates correct schema`() {
        val schema = archiveLogsJsonSchemaString()

        // language=json
        schema shouldEqualJson
            """
            {
              "type": "function",
              "name": "archiveLogs",
              "description": "Archives old logs asynchronously",
              "strict": true,
              "parameters": {
                "type": "object",
                "properties": {
                  "beforeTimestamp": {
                    "type": "integer",
                    "description": "Archive logs older than this timestamp"
                  },
                  "compressionFormat": {
                    "type": "string",
                    "description": "Compression format"
                  }
                },
                "required": ["beforeTimestamp", "compressionFormat"],
                "additionalProperties": false
              }
            }
            """.trimIndent()
    }
}
