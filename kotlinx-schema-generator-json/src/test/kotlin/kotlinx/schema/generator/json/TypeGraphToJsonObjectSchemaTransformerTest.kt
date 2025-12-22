package kotlinx.schema.generator.json

import io.kotest.matchers.shouldBe
import kotlinx.schema.generator.reflect.ReflectionIntrospector
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlin.test.Test

class TypeGraphToJsonObjectSchemaTransformerTest {
    @Test
    fun `should handle nullable types in JsonObject`() {
        val transformer = TypeGraphToJsonObjectSchemaTransformer()
        val typeGraph = ReflectionIntrospector.introspect(NullablePerson::class)
        val json = transformer.transform(typeGraph, "NullablePerson")

        val defs = json[$$"$defs"] as JsonObject
        val personDef =
            defs[
                "kotlinx.schema.generator.json.TypeGraphToJsonObjectSchemaTransformerTest.NullablePerson",
            ] as JsonObject
        val properties = personDef["properties"] as JsonObject
        val ageProp = properties["age"] as JsonObject
        val typeArray = ageProp["type"] as JsonArray
        typeArray.size shouldBe 2
        typeArray.shouldBe(JsonArray(listOf(JsonPrimitive("integer"), JsonPrimitive("null"))))
    }

    @Test
    fun `should handle enums in JsonObject`() {
        val transformer = TypeGraphToJsonObjectSchemaTransformer()
        val typeGraph = ReflectionIntrospector.introspect(WithEnum::class)
        val json = transformer.transform(typeGraph, "WithEnum")

        val defs = json[$$"$defs"] as JsonObject
        val statusDef = defs["kotlinx.schema.generator.json.Status"] as JsonObject
        statusDef["type"] shouldBe JsonPrimitive("string")
        statusDef["enum"] shouldBe
            JsonArray(
                listOf(
                    JsonPrimitive("ACTIVE"),
                    JsonPrimitive("INACTIVE"),
                    JsonPrimitive("PENDING"),
                ),
            )
    }

    @Test
    fun `should handle collections in JsonObject`() {
        val transformer = TypeGraphToJsonObjectSchemaTransformer()
        val typeGraph = ReflectionIntrospector.introspect(WithCollections::class)
        val json = transformer.transform(typeGraph, "WithCollections")

        val defs = json[$$"$defs"] as JsonObject
        val collDef = defs["kotlinx.schema.generator.json.WithCollections"] as JsonObject
        val properties = collDef["properties"] as JsonObject
        (properties["items"] as JsonObject)["type"] shouldBe JsonPrimitive("array")
        (properties["data"] as JsonObject)["type"] shouldBe JsonPrimitive("object")
    }

    @Test
    fun `should handle polymorphism in JsonObject`() {
        val transformer = TypeGraphToJsonObjectSchemaTransformer()
        val typeGraph = ReflectionIntrospector.introspect(JsonSchemaHierarchyTest.AnimalContainer::class)
        val json = transformer.transform(typeGraph, "AnimalContainer")

        val defs = json[$$"$defs"] as JsonObject
        val animalDef = defs["kotlinx.schema.generator.json.JsonSchemaHierarchyTest.Animal"] as JsonObject
        val oneOf = animalDef["oneOf"] as JsonArray
        oneOf.size shouldBe 2
        // Subtype IDs are simple names in this case
        val refs = oneOf.map { (it as JsonObject)[$$"$ref"].toString() }.sorted()
        refs.shouldBe(listOf($$"\"#/$defs/Cat\"", $$"\"#/$defs/Dog\""))
    }

    data class SimplePerson(
        val name: String,
        val age: Int,
    )

    data class NullablePerson(
        val name: String,
        val age: Int?,
    )
}
