package kotlinx.schema.generator.json

import kotlinx.schema.generator.core.AbstractSchemaGenerator
import kotlinx.schema.generator.reflect.ReflectionFunctionIntrospector
import kotlinx.schema.json.FunctionCallingSchema
import kotlinx.serialization.json.Json
import kotlin.reflect.KCallable
import kotlin.reflect.KClass

/**
 * Generates [FunctionCallingSchema] from Kotlin functions using reflection.
 *
 * This generator analyzes function parameters at runtime and produces tool schemas
 * suitable for LLM function calling APIs.
 *
 * ## Example
 * ```kotlin
 * fun greet(name: String, age: Int = 0): String = "Hello, $name!"
 *
 * val generator = ReflectionFunctionCallingSchemaGenerator.Default
 * val schema = generator.generateSchema(::greet)
 * ```
 */
public class ReflectionFunctionCallingSchemaGenerator
    @JvmOverloads
    public constructor(
        private val json: Json = Json,
    ) : AbstractSchemaGenerator<KCallable<*>, FunctionCallingSchema>(
            introspector = ReflectionFunctionIntrospector,
            typeGraphTransformer = TypeGraphToFunctionCallingSchemaTransformer(json),
        ) {
        override fun getRootName(target: KCallable<*>): String = target.name

        override fun targetType(): KClass<KCallable<*>> = KCallable::class

        override fun schemaType(): KClass<FunctionCallingSchema> = FunctionCallingSchema::class

        override fun encodeToString(schema: FunctionCallingSchema): String = json.encodeToString(schema)

        public companion object {
            /**
             * A default instance of [ReflectionFunctionCallingSchemaGenerator] with default configuration.
             *
             * This instance can be used to generate tool schemas from Kotlin functions
             * without requiring explicit instantiation.
             */
            public val Default: ReflectionFunctionCallingSchemaGenerator = ReflectionFunctionCallingSchemaGenerator()
        }
    }
