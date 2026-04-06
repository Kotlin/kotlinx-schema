package kotlinx.schema.integration.type

import io.kotest.assertions.json.shouldEqualJson
import kotlin.test.Test

/**
 * Tests for @SchemaIgnore on sealed subtypes — ignored subtypes must not appear
 * in the polymorphic oneOf schema or $defs.
 */
class EventSchemaTest {
    @Test
    fun `sealed class excludes @SchemaIgnore and @JsonIgnoreType subtypes from oneOf schema`() {
        val schema = Event::class.jsonSchemaString

        // language=json
        schema shouldEqualJson
            $$"""
            {
              "$schema": "https://json-schema.org/draft/2020-12/schema",
              "$id": "kotlinx.schema.integration.type.Event",
              "description": "An application event",
              "type": "object",
              "additionalProperties": false,
              "oneOf": [
                {
                  "$ref": "#/$defs/kotlinx.schema.integration.type.Event.Click"
                },
                {
                  "$ref": "#/$defs/kotlinx.schema.integration.type.Event.PageView"
                }
              ],
              "$defs": {
                "kotlinx.schema.integration.type.Event.Click": {
                  "type": "object",
                  "description": "User clicked on an element",
                  "properties": {
                    "type": {
                      "type": "string",
                      "const": "kotlinx.schema.integration.type.Event.Click"
                    },
                    "timestamp": {
                      "type": "integer"
                    },
                    "x": {
                      "type": "integer",
                      "description": "X coordinate"
                    },
                    "y": {
                      "type": "integer",
                      "description": "Y coordinate"
                    }
                  },
                  "required": [
                    "type",
                    "timestamp",
                    "x",
                    "y"
                  ],
                  "additionalProperties": false
                },
                "kotlinx.schema.integration.type.Event.PageView": {
                  "type": "object",
                  "description": "Page was viewed",
                  "properties": {
                    "type": {
                      "type": "string",
                      "const": "kotlinx.schema.integration.type.Event.PageView"
                    },
                    "timestamp": {
                      "type": "integer"
                    },
                    "url": {
                      "type": "string",
                      "description": "Page URL"
                    }
                  },
                  "required": [
                    "type",
                    "timestamp",
                    "url"
                  ],
                  "additionalProperties": false
                }
              }
            }
            """.trimIndent()
    }
}
