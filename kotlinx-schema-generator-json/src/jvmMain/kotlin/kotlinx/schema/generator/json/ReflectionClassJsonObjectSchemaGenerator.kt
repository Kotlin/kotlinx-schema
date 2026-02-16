package kotlinx.schema.generator.json

import kotlinx.schema.generator.core.AbstractSchemaGenerator
import kotlinx.schema.generator.reflect.ReflectionClassIntrospector
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlin.reflect.KClass

/**
 * A generator class for creating JSON object schemas using reflection-based introspection.
 *
 * This class utilizes Kotlin's reflection capabilities to inspect the structure
 * of a target class and generate a corresponding JSON schema representation.
 * It relies on the `Json` serializer and a customizable configuration defined
 * in [JsonSchemaConfig].
 *
 * @constructor Accepts a `Json` instance and a `JsonSchemaConfig` for custom schema generation.
 * @property json The `Json` instance used for serializing schema objects.
 * @param config Configuration settings for schema customization.
 */
public class ReflectionClassJsonObjectSchemaGenerator(
    private val json: Json,
    config: JsonSchemaConfig,
) : AbstractSchemaGenerator<KClass<out Any>, JsonObject>(
        introspector = ReflectionClassIntrospector,
        typeGraphTransformer =
            TypeGraphToJsonObjectSchemaTransformer(
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

    override fun schemaType(): KClass<JsonObject> = JsonObject::class

    override fun encodeToString(schema: JsonObject): String = json.encodeToString(schema)

    public companion object {
        /**
         * A default instance of [ReflectionClassJsonObjectSchemaGenerator] with preconfigured settings.
         *
         * This instance is initialized with the default [JsonSchemaConfig.Default] configuration
         * and a `Json` instance where `encodeDefaults` is set to `false`. It provides a ready-to-use
         * setup for generating JSON object schemas using reflection-based class inspection.
         */
        public val Default: ReflectionClassJsonObjectSchemaGenerator =
            ReflectionClassJsonObjectSchemaGenerator(
                json = Json { encodeDefaults = false },
                config = JsonSchemaConfig.Default,
            )
    }
}
