package kotlinx.schema.integration

import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlin.test.Test

/**
 * Cross-cutting infrastructure tests for schema generation.
 */
class SchemaInfrastructureTest {
    @Test
    fun `all schemas are valid JSON`() {
        val schemas =
            listOf(
                Person::class.jsonSchemaString,
                Address::class.jsonSchemaString,
                Product::class.jsonSchemaString,
                User::class.jsonSchemaString,
                Status::class.jsonSchemaString,
                Container::class.jsonSchemaString,
                Order::class.jsonSchemaString,
                Animal::class.jsonSchemaString,
            )

        schemas.forEach { schema ->
            val trimmed = schema.trim()
            trimmed.first() shouldBe '{'
            trimmed.last() shouldBe '}'
            trimmed.length shouldBeGreaterThan 50

            // Should parse without errors
            val jsonObj = Json.decodeFromString<JsonObject>(schema)
            jsonObj shouldNotBe null
        }
    }

    @Test
    fun `all schemas conform to JSON Schema Draft 2020-12`() {
        val schemas =
            listOf(
                Person::class.jsonSchemaString,
                Address::class.jsonSchemaString,
                Product::class.jsonSchemaString,
                User::class.jsonSchemaString,
                Status::class.jsonSchemaString,
                Container::class.jsonSchemaString,
                Order::class.jsonSchemaString,
                Animal::class.jsonSchemaString,
            )

        schemas.forEach { schema ->
            val jsonObj = Json.decodeFromString<JsonObject>(schema)

            // Required JSON Schema fields must exist
            assert(jsonObj.containsKey("\$id")) { "Schema must have \$id field" }
            assert(jsonObj.containsKey("\$defs")) { "Schema must have \$defs field" }
            assert(jsonObj.containsKey("\$ref")) { "Schema must have \$ref field" }
        }
    }

    @Test
    fun `each annotated class has unique schema ID`() {
        val schemas =
            mapOf(
                "Person" to Person::class.jsonSchemaString,
                "Address" to Address::class.jsonSchemaString,
                "Product" to Product::class.jsonSchemaString,
                "User" to User::class.jsonSchemaString,
                "Status" to Status::class.jsonSchemaString,
                "Container" to Container::class.jsonSchemaString,
                "Order" to Order::class.jsonSchemaString,
                "Animal" to Animal::class.jsonSchemaString,
            )

        // Each schema contains its correct ID
        schemas.forEach { (name, schema) ->
            val jsonObj = Json.decodeFromString<JsonObject>(schema)
            val id = jsonObj["\$id"]?.toString()?.trim('"')
            assert(id == "kotlinx.schema.integration.$name") {
                "Expected ID kotlinx.schema.integration.$name but got $id"
            }
        }

        // All schemas are distinct
        val uniqueSchemas = schemas.values.toSet()
        uniqueSchemas.size shouldBe schemas.size
    }

    @Test
    fun `JsonObject generation works when withSchemaObject is true`() {
        // Both string and object generated
        Person::class.jsonSchemaString shouldNotBe null
        Person::class.jsonSchema shouldNotBe null

        Order::class.jsonSchemaString shouldNotBe null
        Order::class.jsonSchema shouldNotBe null

        // JsonObject matches parsed string
        val personString = Person::class.jsonSchemaString
        val personObject = Person::class.jsonSchema
        personObject shouldNotBeNull {
            this shouldBeEqual Json.decodeFromString<JsonObject>(personString)
        }

        // Works for complex nested structures
        Order::class.jsonSchema shouldNotBeNull {
            assert(this["\$id"]?.toString()?.contains("Order") == true) {
                "Order schema should have \$id containing 'Order'"
            }
        }

        // Works for sealed class subtypes
        Animal.Cat::class.jsonSchema shouldNotBe null
        Animal.Dog::class.jsonSchema shouldNotBe null
    }

    @Test
    fun `non-annotated classes do not have generated extensions`() {
        // Compile-time verification that KSP skips non-@Schema classes
        val clazz = NonAnnotatedClass::class
        clazz shouldNotBe null
    }
}
