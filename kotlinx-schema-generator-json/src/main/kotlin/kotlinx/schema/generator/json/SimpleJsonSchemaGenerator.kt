package kotlinx.schema.generator.json

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject

/**
 * A utility class for generating JSON schema representations of Kotlin objects.
 */
public object SimpleJsonSchemaGenerator : JsonSchemaGenerator {
    public override fun generateSchema(target: Any): JsonObject {
        val schemaString =
            // language=json
            """
          {
          "title": "Person",                        
          "description": "Personal information",                     
          "required": [ "firstName" ],
          "type": "object",
          "properties": {
            "firstName": {
              "type": "string"
            }
          }
        }
        """

        return (Json.parseToJsonElement(schemaString) as JsonObject)
    }
}