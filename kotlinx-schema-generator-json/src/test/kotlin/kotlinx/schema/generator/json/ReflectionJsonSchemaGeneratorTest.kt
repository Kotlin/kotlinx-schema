package kotlinx.schema.generator.json

import io.kotest.assertions.json.shouldEqualJson
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import kotlinx.schema.Description
import kotlinx.schema.json.ArrayPropertyDefinition
import kotlinx.schema.json.NumericPropertyDefinition
import kotlinx.schema.json.ObjectPropertyDefinition
import kotlinx.schema.json.StringPropertyDefinition
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.test.Test

class ReflectionJsonSchemaGeneratorTest {
    @Description("Available colors")
    enum class Color {
        RED,
        GREEN,
        BLUE,
    }

    data class WithEnum(
        @property:Description("The color of the rainbow")
        val color: Color,
    )

    private val json =
        Json {
            prettyPrint = true
        }

    // language=json

    @Test
    fun generateJsonSchema() {
        @Description("Personal information")
        data class Person(
            @property:Description("Person's first name")
            val firstName: String,
        )

        val schema =
            ReflectionJsonSchemaGenerator().generateSchema(Person::class)

        // language=json
        val expectedSchema = """ 
        {
            "name": "${Person::class.qualifiedName}",
            "strict": false,
            "schema": {
              "description": "Personal information",
              "required": [ "firstName" ],
              "type": "object",
              "properties": {
                "firstName": {
                  "type": "string",
                  "description": "Person's first name"
                }
              },
              "additionalProperties": false
            }
        }
        """
        val actualSchemaString = json.encodeToString(schema)
        println("Expected schema = $expectedSchema")
        println("Actual schema = $actualSchemaString")
    }

    @Test
    fun generateJsonSchema_forUserWithVariousTypes() {
        @Description("A user model")
        data class User(
            @property:Description("The name of the user")
            val name: String,
            val age: Int?,
            val email: String = "n/a",
            val tags: List<String>,
            val attributes: Map<String, Int>?,
        )

        val schema = ReflectionJsonSchemaGenerator().generateSchema(User::class)

        // language=json
        val expectedSchema = """
        {
            "name": "Anonymous",
            "strict": false,
            "schema": {
              "description": "A user model",
              "required": [ "name", "age", "tags", "attributes" ],
              "type": "object",
              "properties": {
                "name": {
                  "type": "string",
                  "description": "The name of the user"
                },
                "age": {
                  "type": "integer"
                },
                "email": {
                  "type": "string"
                },
                "tags": {
                  "type": "array",
                  "items": {
                    "type": "string"
                  }
                },
                "attributes": {
                  "type": "object",
                  "additionalProperties": {
                    "type": "integer"
                  }
                }
              },
              "additionalProperties": false
            }
        }
        """

        val actualSchemaString = json.encodeToString(schema)

        actualSchemaString shouldEqualJson expectedSchema
    }

    @Test
    fun generateJsonSchema_forEnumProperty() {
        val schema = ReflectionJsonSchemaGenerator().generateSchema(WithEnum::class)

        // language=json
        val expectedSchema = """
        {
            "name": "${WithEnum::class.qualifiedName}",
            "strict": false,
            "schema": {
              "required": [ "color" ],
              "type": "object",
              "properties": {
                "color": {
                  "type": "string",
                  "description": "The color of the rainbow",
                  "enum": ["RED", "GREEN", "BLUE"]
                }
              },
              "additionalProperties": false
            }
        }
        """

        val actualSchemaString = json.encodeToString(schema)

        actualSchemaString shouldEqualJson expectedSchema
    }
}
