package kotlinx.schema.generator.core.ir

/**
 * Converts a [TypeGraph] intermediate representation to a target schema format.
 *
 * Implementations of this interface transform the language-agnostic IR produced by
 * [SchemaIntrospector] into specific schema formats like JSON Schema, OpenAPI, Avro, etc.
 *
 * Example:
 * ```kotlin
 * data class MyTransformerConfig(val verbose: Boolean)
 *
 * class MySchemaTransformer : TypeGraphTransformer<MySchema, MyTransformerConfig>(
 *     val config: MyTransformerConfig
 * ) {
 *     override fun transform(graph: TypeGraph, rootName: String): MySchema {
 *         // Convert graph to MySchema format
 *     }
 * }
 * ```
 *
 * @param R The type of the resulting schema format returned after the transformation.
 * @param C The type of the configuration used to guide the transformation process.
 */
public interface TypeGraphTransformer<R, C> {
    /**
     * The configuration object defining transformation options and rules.
     */
    public val config: C

    /**
     * Transforms a given [TypeGraph] into a target schema format using the specified configuration.
     *
     * This function processes the intermediate representation of types captured in the
     * [TypeGraph] and converts it into a format determined by the implementation with respect
     * of the [config].
     * The resulting schema is rooted at the type identified by [rootName].
     *
     * @param graph The input [TypeGraph] containing the root type and type definitions to be transformed.
     * @param rootName The name of the root type in the schema being generated.
     * @return The result of the transformation in the target schema format.
     */
    public fun transform(
        graph: TypeGraph,
        rootName: String,
    ): R
}

/**
 * Abstract base class for implementing type graph transformations into target schema formats.
 *
 * This class provides an abstract foundation for transforming a [TypeGraph], which represents
 * the intermediate representation of types, into specific formats such as JSON Schema, OpenAPI, Avro, etc.
 * Subclasses are expected to provide the implementation for transforming the type graph while utilizing
 * the configuration provided via the [config] property.
 *
 * @param R The result type produced by the transformation.
 * @param C The type of the configuration object,
 * defining the transformation-specific parameters and rules.
 * @property config The configuration object used to customize the transformation process.
 */
public abstract class AbstractTypeGraphTransformer<R, C>(
    override val config: C,
) : TypeGraphTransformer<R, C>
