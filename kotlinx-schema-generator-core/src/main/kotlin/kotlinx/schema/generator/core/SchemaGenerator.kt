package kotlinx.schema.generator.core

import kotlin.reflect.KClass

public interface SchemaGenerator<T : Any, R : Any> {
    /**
     * Returns the type of the target for which the schema is being generated.
     *
     * @return the [KClass] of the target type
     */
    public fun targetType(): KClass<T>

    /**
     * Returns the type of the schema representation being generated.
     *
     * @return the [KClass] of the schema representation type
     */
    public fun schemaType(): KClass<R>

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

    /**
     * Serializes the given schema representation into a string.
     *
     * @param schema the schema representation to be serialized
     * @return a string representing the provided schema
     */
    public fun encodeToString(schema: R): String
}
