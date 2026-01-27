package kotlinx.schema.generator.json

import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.shouldBe
import kotlin.test.Test

/**
 * Tests for JsonSchemaTransformerConfig options.
 * Focuses on configuration-specific behavior.
 */
class JsonSchemaConfigTest {
    @Test
    fun `respectDefaultPresence true should use default presence for required fields`() {
        val config =
            JsonSchemaTransformerConfig(
                strictSchemaFlag = false,
                respectDefaultPresence = true,
                requireNullableFields = true, // ignored when respectDefaultPresence=true
            )
        val transformer = TypeGraphToJsonSchemaTransformer(config)
        val typeGraph =
            kotlinx.schema.generator.reflect.ReflectionIntrospector
                .introspect(PersonWithOptionals::class)
        val schema = transformer.transform(typeGraph, "PersonWithOptionals")

        val required = schema.schema.required

        // Only properties without defaults should be required
        required.size shouldBe 1
        required shouldContainAll listOf("name")
    }

    @Test
    fun `requireNullableFields true should include all fields in required`() {
        val config =
            JsonSchemaTransformerConfig(
                strictSchemaFlag = false,
                respectDefaultPresence = false,
                requireNullableFields = true,
            )
        val transformer = TypeGraphToJsonSchemaTransformer(config)
        val typeGraph =
            kotlinx.schema.generator.reflect.ReflectionIntrospector
                .introspect(PersonWithOptionals::class)
        val schema = transformer.transform(typeGraph, "PersonWithOptionals")

        val required = schema.schema.required

        // All properties should be in required array
        required.size shouldBe 5
        required shouldContainAll listOf("name", "age", "email", "score", "active")
    }

    @Test
    fun `requireNullableFields false should only include non-nullable fields in required`() {
        val config =
            JsonSchemaTransformerConfig(
                strictSchemaFlag = false,
                respectDefaultPresence = false,
                requireNullableFields = false,
            )
        val transformer = TypeGraphToJsonSchemaTransformer(config)
        val typeGraph =
            kotlinx.schema.generator.reflect.ReflectionIntrospector
                .introspect(PersonWithOptionals::class)
        val schema = transformer.transform(typeGraph, "PersonWithOptionals")

        val required = schema.schema.required

        // Only non-nullable properties should be required
        // PersonWithOptionals has only 'name' as non-nullable
        required.size shouldBe 1
        required shouldContainAll listOf("name")
    }

    @Test
    fun `strictSchemaFlag true should set strict flag in output`() {
        val config =
            JsonSchemaTransformerConfig(
                strictSchemaFlag = true,
                respectDefaultPresence = false,
                requireNullableFields = true,
            )
        val transformer = TypeGraphToJsonSchemaTransformer(config)
        val typeGraph =
            kotlinx.schema.generator.reflect.ReflectionIntrospector
                .introspect(PersonWithOptionals::class)
        val schema = transformer.transform(typeGraph, "PersonWithOptionals")

        schema.strict shouldBe true
    }

    @Test
    fun `strictSchemaFlag false should set strict flag to false in output`() {
        val config =
            JsonSchemaTransformerConfig(
                strictSchemaFlag = false,
                respectDefaultPresence = false,
                requireNullableFields = true,
            )
        val transformer = TypeGraphToJsonSchemaTransformer(config)
        val typeGraph =
            kotlinx.schema.generator.reflect.ReflectionIntrospector
                .introspect(PersonWithOptionals::class)
        val schema = transformer.transform(typeGraph, "PersonWithOptionals")

        schema.strict shouldBe false
    }
}
