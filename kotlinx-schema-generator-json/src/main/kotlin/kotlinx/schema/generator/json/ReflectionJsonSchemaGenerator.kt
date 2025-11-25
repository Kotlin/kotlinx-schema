package kotlinx.schema.generator.json

import kotlinx.schema.generator.core.AbstractSchemaGenerator
import kotlinx.schema.generator.reflect.ReflectionIntrospector
import kotlinx.schema.json.JsonSchema
import kotlin.reflect.KClass

public class ReflectionJsonSchemaGenerator
    @JvmOverloads
    public constructor(
        private val jsonSchemaConfig: JsonSchemaConfig = JsonSchemaConfig.Default,
    ) : AbstractSchemaGenerator<KClass<out Any>, JsonSchema>(
            introspector = ReflectionIntrospector,
            typeGraphTransformer = TypeGraphToJsonSchemaTransformer(config = jsonSchemaConfig),
        ) {
        override fun getRootName(target: KClass<out Any>): String = target.qualifiedName ?: "Anonymous"

        override fun targetType(): KClass<KClass<out Any>> = KClass::class

        override fun schemaType(): KClass<JsonSchema> = JsonSchema::class

        override fun encodeToString(schema: JsonSchema): String = jsonSchemaConfig.json.encodeToString(schema)

        public companion object {
            /**
             * A default instance of the [ReflectionJsonSchemaGenerator] class, preconfigured
             * with the default settings defined in [JsonSchemaConfig.Default].
             *
             * This instance can be used to generate JSON schema representations of Kotlin
             * objects using reflection-based introspection. It simplifies the creation
             * of schemas without requiring explicit configuration.
             */
            public val Default: ReflectionJsonSchemaGenerator = ReflectionJsonSchemaGenerator()
        }
    }
