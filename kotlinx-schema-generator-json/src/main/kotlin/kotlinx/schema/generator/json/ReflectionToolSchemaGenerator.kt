package kotlinx.schema.generator.json

import kotlinx.schema.generator.core.AbstractSchemaGenerator
import kotlinx.schema.generator.reflect.ReflectionFunctionIntrospector
import kotlinx.schema.json.ToolSchema
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonObject
import kotlin.reflect.KCallable
import kotlin.reflect.KClass

/**
 * Generates [ToolSchema] from Kotlin functions using reflection.
 *
 * This generator analyzes function parameters at runtime and produces tool schemas
 * suitable for LLM function calling APIs.
 *
 * ## Example
 * ```kotlin
 * fun greet(name: String, age: Int = 0): String = "Hello, $name!"
 *
 * val generator = ReflectionToolSchemaGenerator.Default
 * val schema = generator.generateSchema(::greet)
 * ```
 */
public class ReflectionToolSchemaGenerator
    @JvmOverloads
    public constructor(
        json: Json = Json,
    ) : AbstractSchemaGenerator<KCallable<*>, ToolSchema>(
            introspector = ReflectionFunctionIntrospector,
            typeGraphTransformer = TypeGraphToToolSchemaTransformer(json),
        ) {
        override fun getRootName(target: KCallable<*>): String = target.name

        override fun targetType(): KClass<KCallable<*>> = KCallable::class

        override fun schemaType(): KClass<ToolSchema> = ToolSchema::class

        override fun encodeToString(schema: ToolSchema): String = Json.encodeToString(schema)

        public fun encodeToJsonObject(schema: ToolSchema): JsonObject = Json.encodeToJsonElement(schema).jsonObject

        public companion object {
            /**
             * A default instance of [ReflectionToolSchemaGenerator] with default configuration.
             *
             * This instance can be used to generate tool schemas from Kotlin functions
             * without requiring explicit instantiation.
             */
            public val Default: ReflectionToolSchemaGenerator = ReflectionToolSchemaGenerator()
        }
    }
