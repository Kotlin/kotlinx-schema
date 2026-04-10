package kotlinx.schema.generator.json

import kotlinx.schema.generator.json.serialization.SerializationClassJsonSchemaGenerator
import kotlinx.schema.json.JsonSchema
import kotlinx.serialization.serializer

/**
 * Generates a JSON Schema for the given `@Serializable` type [T], using the provided [generator].
 */
public inline fun <reified T> jsonSchemaOf(
    generator: SerializationClassJsonSchemaGenerator = SerializationClassJsonSchemaGenerator.Default,
): JsonSchema = generator.generateSchema(serializer<T>().descriptor)
