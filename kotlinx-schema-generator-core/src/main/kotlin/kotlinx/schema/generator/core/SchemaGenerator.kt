package kotlinx.schema.generator.core

public interface SchemaGenerator<T : Any, R> {
    /**
     * Generates a JSON object representing the schema of the input target.
     *
     * @param target the object for which the JSON schema will be generated
     * @return a JsonObject representing the schema of the provided object
     */
    public fun generateSchema(target: T): R

    /**
     * Serializes the schema of the provided object into a JSON-formatted string.
     *
     * @param target the object for which the schema will be generated
     * @return a JSON string representing the schema of the provided object
     */
    public fun generateSchemaString(target: T): String
}
