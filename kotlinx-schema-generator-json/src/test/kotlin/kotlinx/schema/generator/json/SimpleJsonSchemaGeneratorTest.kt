package kotlinx.schema.generator.json

import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import kotlinx.schema.Description
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlin.test.Test

class SimpleJsonSchemaGeneratorTest {
    @Serializable
    @Description("Personal information")
    data class Person(
        @property:Description("Person's first name")
        val firstName: String,
    )

    private val personSchema =
        Json.decodeFromString<JsonObject>(
            // language=json
            """
          {
          "title": "Person",                        
          "description": "Personal information",                     
          "required": [ "firstName" ],
          "type": "object",
          "properties": {
            "firstName": {
              "type": "string"
            }
          }
        }
        """,
        )

    @Test
    fun generateJsonSchema() {
        val schema = SimpleJsonSchemaGenerator.generateSchema(Person::class)

        println("schema = $schema")
        println("schema = $personSchema")

        schema shouldBe personSchema
    }
}