package kotlinx.schema.ksp.generator

import kotlinx.schema.generator.core.ir.SchemaIntrospector
import kotlinx.schema.generator.core.ir.TypeGraphTransformer
import kotlinx.serialization.KSerializer

/**
 * Configuration for [UnifiedKspSchemaGenerator].
 *
 * This data class encapsulates all the configuration needed to create a schema generator,
 * making the generator itself reusable with different introspectors and transformers.
 *
 * @param T The type of declaration to introspect (e.g., KSClassDeclaration, KSFunctionDeclaration)
 * @param R The type of schema representation to generate (e.g., JsonObject, FunctionCallingSchema)
 * @property introspector The introspector that analyzes the declaration and builds a type graph
 * @property transformer The transformer that converts the type graph to the target schema representation
 * @property serializer The kotlinx.serialization serializer for the schema type R
 * @property jsonPrettyPrint Whether to pretty-print the JSON output
 * @property jsonEncodeDefaults Whether to encode default values in the JSON output
 */
internal data class KspSchemaGeneratorConfig<T : Any, R : Any>(
    val introspector: SchemaIntrospector<T>,
    val transformer: TypeGraphTransformer<R, *>,
    val serializer: KSerializer<R>,
    val jsonPrettyPrint: Boolean,
    val jsonEncodeDefaults: Boolean,
)
