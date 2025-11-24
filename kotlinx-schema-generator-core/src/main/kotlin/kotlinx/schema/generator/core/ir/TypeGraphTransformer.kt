package kotlinx.schema.generator.core.ir

/**
 * Converts a [TypeGraph] intermediate representation to a target schema format.
 *
 * Implementations of this interface transform the language-agnostic IR produced by
 * [SchemaIntrospector] into specific schema formats like JSON Schema, OpenAPI, Avro, etc.
 *
 * Example:
 * ```kotlin
 * class MySchemaTransformer : TypeGraphTransformer<MySchema> {
 *     override fun transform(graph: TypeGraph, rootName: String): MySchema {
 *         // Convert graph to MySchema format
 *     }
 * }
 * ```
 */
public interface TypeGraphTransformer<R> {
    public fun transform(
        graph: TypeGraph,
        rootName: String,
    ): R
}
