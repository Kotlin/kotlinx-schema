package kotlinx.schema.generator.json.serialization

import kotlinx.schema.generator.core.AbstractSchemaGenerator
import kotlinx.schema.generator.json.JsonSchemaConfig
import kotlinx.schema.generator.json.TypeGraphToJsonSchemaTransformer
import kotlinx.schema.json.JsonSchema
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.json.Json
import kotlin.reflect.KClass

/**
 * A generator for producing JSON Schema representations from kotlinx.serialization descriptors.
 *
 * This class utilizes kotlinx.serialization introspection to analyze [SerialDescriptor] instances
 * and generate JSON Schema objects. It is built on top of the `AbstractSchemaGenerator` and works
 * with a configurable `JsonSchemaConfig` to define schema generation behavior.
 *
 * @constructor Creates an instance of `SerializationClassJsonSchemaGenerator`.
 * @param config Configuration for generating JSON Schemas, such as formatting details
 * and handling of optional nullable properties. Defaults to [JsonSchemaConfig.Default].
 */
public class SerializationClassJsonSchemaGenerator(
    private val json: Json,
    config: JsonSchemaConfig,
) : AbstractSchemaGenerator<SerialDescriptor, JsonSchema>(
        introspector = SerializationClassSchemaIntrospector(json),
        typeGraphTransformer =
            TypeGraphToJsonSchemaTransformer(
                config = config,
                json = json,
            ),
    ) {
    public constructor() : this(
        json = Json {
            encodeDefaults = false
            classDiscriminator = "type"
            classDiscriminatorMode = kotlinx.serialization.json.ClassDiscriminatorMode.ALL_JSON_OBJECTS
        },
        config = JsonSchemaConfig.Default,
    )

    override fun getRootName(target: SerialDescriptor): String = target.serialName

    override fun targetType(): KClass<SerialDescriptor> = SerialDescriptor::class

    override fun schemaType(): KClass<JsonSchema> = JsonSchema::class

    override fun encodeToString(schema: JsonSchema): String = json.encodeToString(schema)

    public companion object {
        /**
         * A default instance of the [SerializationClassJsonSchemaGenerator] class, preconfigured
         * with the default settings defined in [JsonSchemaConfig.Default].
         *
         * This instance can be used to generate JSON schema representations from
         * kotlinx.serialization descriptors. It simplifies the creation of schemas
         * without requiring explicit configuration.
         */
        public val Default: SerializationClassJsonSchemaGenerator =
            SerializationClassJsonSchemaGenerator(
                json = Json {
                    encodeDefaults = false
                    classDiscriminator = "type"
                    classDiscriminatorMode = kotlinx.serialization.json.ClassDiscriminatorMode.ALL_JSON_OBJECTS
                },
                config = JsonSchemaConfig.Default,
            )
    }
}
