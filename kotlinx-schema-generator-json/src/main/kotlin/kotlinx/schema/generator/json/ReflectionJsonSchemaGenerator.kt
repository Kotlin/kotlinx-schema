package kotlinx.schema.generator.json

import kotlinx.schema.generator.core.AbstractSchemaGenerator
import kotlinx.schema.generator.reflect.ReflectionIntrospector
import kotlinx.schema.json.JsonSchema
import kotlinx.serialization.json.Json
import kotlin.reflect.KClass

public class ReflectionJsonSchemaGenerator
    @JvmOverloads
    public constructor(
        jsonSchemaConfig: JsonSchemaConfig = JsonSchemaConfig.DEFAULT,
    ) : AbstractSchemaGenerator<KClass<out Any>, JsonSchema>(
            introspector = ReflectionIntrospector,
            emitter = JsonSchemaEmitter(config = jsonSchemaConfig),
        ) {
        override fun getRootName(target: KClass<out Any>): String = target.qualifiedName ?: "Anonymous"

        override fun targetType(): KClass<KClass<out Any>> = KClass::class

        override fun schemaType(): KClass<JsonSchema> = JsonSchema::class

        override fun encodeToString(schema: JsonSchema): String = Json.encodeToString(schema)
    }
