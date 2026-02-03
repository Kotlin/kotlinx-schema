package kotlinx.schema.generator.json

import kotlinx.schema.generator.core.SchemaGenerator
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject

/**
 * A utility class for generating JSON schema representations of Kotlin objects.
 */
public interface JsonSchemaGenerator<T : Any> : SchemaGenerator<T, JsonObject> {
    /**
     * Generates a JSON object representing the schema of the input target.
     *
     * @param target the object for which the JSON schema will be generated
     * @return a JsonObject representing the schema of the provided object
     */
    public override fun generateSchema(target: T): JsonObject

    /**
     * Serializes the schema of the provided object into a JSON-formatted string.
     *
     * @param target the object for which the schema will be generated
     * @return a JSON string representing the schema of the provided object
     */
    public override fun generateSchemaString(target: T): String = encodeToString(generateSchema(target))

    override fun encodeToString(schema: JsonObject): String = Json.encodeToString(schema)
}
