package kotlinx.schema.generator.json

import kotlinx.schema.generator.json.serialization.SerializationClassJsonSchemaGenerator
import kotlinx.schema.json.JsonSchema
import kotlinx.serialization.serializer

/**
 * Generates a JSON Schema for the `@Serializable` type [T] via its compile-time [serializer].
 *
 * Mirrors kotlinx.serialization's `serializer<T>()`. [T] must be `@Serializable`; a
 * non-serializable type still compiles but throws at runtime. Pass a custom [generator] to
 * configure schema generation; when you need the schema as a `String`, prefer
 * `generator.generateSchemaString(serializer<T>().descriptor)` so that encoding uses the
 * generator's own `Json`.
 */
public inline fun <reified T> jsonSchemaOf(
    generator: SerializationClassJsonSchemaGenerator = SerializationClassJsonSchemaGenerator.Default,
): JsonSchema = generator.generateSchema(serializer<T>().descriptor)
