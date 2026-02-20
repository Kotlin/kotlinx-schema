package kotlinx.schema.generator.json

import kotlinx.schema.generator.core.AbstractSchemaGenerator
import kotlinx.schema.generator.reflect.ReflectionClassIntrospector
import kotlinx.schema.json.JsonSchema
import kotlinx.serialization.json.Json
import kotlin.reflect.KClass

/**
 * A generator for producing JSON Schema representations of Kotlin classes using reflection.
 *
 * This class utilizes reflection-based introspection to analyze Kotlin `KClass` definitions
 * and generate JSON Schema objects. It is built on top of the `AbstractSchemaGenerator` and works
 * with a configurable `JsonSchemaConfig` to define schema generation behavior.
 *
 * @constructor Creates an instance of `ReflectionClassJsonSchemaGenerator`.
 * @param config Configuration for generating JSON Schemas, such as formatting details
 * and handling of optional nullable properties. Defaults to [JsonSchemaConfig.Default].
 */
public class ReflectionClassJsonSchemaGenerator(
    private val json: Json,
    config: JsonSchemaConfig,
) : AbstractSchemaGenerator<KClass<out Any>, JsonSchema, Unit>(
        introspector = ReflectionClassIntrospector,
        typeGraphTransformer =
            TypeGraphToJsonSchemaTransformer(
                config = config,
                json = json,
            ),
    ) {
    public constructor() : this(
        json = Json { encodeDefaults = false },
        config = JsonSchemaConfig.Default,
    )

    override fun getRootName(target: KClass<out Any>): String = target.qualifiedName ?: target.simpleName ?: "Anonymous"

    override fun targetType(): KClass<KClass<out Any>> = KClass::class

    override fun schemaType(): KClass<JsonSchema> = JsonSchema::class

    override fun encodeToString(schema: JsonSchema): String = json.encodeToString(schema)

    public companion object {
        /**
         * A default instance of the [ReflectionClassJsonSchemaGenerator] class, preconfigured
         * with the default settings defined in [JsonSchemaConfig.Default].
         *
         * This instance can be used to generate JSON schema representations of Kotlin
         * objects using reflection-based introspection. It simplifies the creation
         * of schemas without requiring explicit configuration.
         */
        public val Default: ReflectionClassJsonSchemaGenerator =
            ReflectionClassJsonSchemaGenerator(
                json = Json { encodeDefaults = false },
                config = JsonSchemaConfig.Default,
            )
    }
}
