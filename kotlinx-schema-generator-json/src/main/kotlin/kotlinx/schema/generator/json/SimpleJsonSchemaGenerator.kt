package kotlinx.schema.generator.json

import kotlinx.schema.generator.json.internal.BasicJsonSchemaGenerator
import kotlinx.schema.generator.json.internal.StandardJsonSchemaGenerator
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.serializer
import kotlin.reflect.KClass

/**
 * A utility class for generating JSON schema representations of Kotlin objects.
 */
public object SimpleJsonSchemaGenerator : JsonSchemaGenerator<KClass<out Any>> {
//    private val standardGenerator = StandardJsonSchemaGenerator.Default
    private val basicGenerator = BasicJsonSchemaGenerator.Default

    @OptIn(InternalSerializationApi::class)
    public override fun generateSchema(target: KClass<out Any>): JsonObject =
        basicGenerator.generate(
            json = Json.Default,
            name = target.simpleName!!,
            serializer = target.serializer(),
            descriptionOverrides = emptyMap(),
            excludedProperties = emptySet(),
        )
}
