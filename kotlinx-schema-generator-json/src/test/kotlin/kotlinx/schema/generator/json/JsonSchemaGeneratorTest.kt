package kotlinx.schema.generator.json

import kotlinx.schema.Description
import kotlinx.schema.generator.core.SchemaGeneratorService
import kotlinx.schema.json.JsonSchema
import kotlin.reflect.KClass
import kotlin.test.Test

class JsonSchemaGeneratorTest {
    @Suppress("unused")
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

    @Description("Personal information")
    data class Person(
        @property:Description("Person's first name")
        val firstName: String,
    )

    @Description("Pet information")
    open class Pet(
        @property:Description("Pet name")
        val name: String,
    )

    @Description("Cat information")
    open class Cat(
        @property:Description("Most cats have 18 toes, 5 on each front foot and 4 on each hind paw")
        val toes: Int,
        name: String,
    ) : Pet(name = name)

    private val generator =
        requireNotNull(
            SchemaGeneratorService.getGenerator(
                KClass::class,
                JsonSchema::class,
            ),
        ) {
            "ReflectionClassJsonSchemaGenerator must be registered"
        }

    @Test
    fun `Should generate schema for simple data class`() {
        val schema = generator.generateSchema(Person::class)

        // language=json
        val expectedSchema = """ 
        {
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
        """

        verifySchema(schema, expectedSchema)
    }

    @Test
    fun `Should generate schema for simple class`() {
        val schema = generator.generateSchema(Pet::class)

        // language=json
        val expectedSchema = """ 
        {
              "description": "Pet information",
              "required": [ "name" ],
              "type": "object",
              "properties": {
                "name": {
                  "type": "string",
                  "description": "Pet name"
                }
              },
              "additionalProperties": false
        }
        """
        verifySchema(schema, expectedSchema)
    }

    @Test
    fun `Should generate schema for simple class hierarchy`() {
        val schema = generator.generateSchema(Cat::class)

        // language=json
        val expectedSchema = """ 
        {
              "description": "Cat information",
              "required": ["toes" , "name"],
              "type": "object",
              "properties": {
                "name": {
                  "type": "string",
                  "description": "Pet name"
                },
                "toes": {
                  "type": "integer",
                  "description": "Most cats have 18 toes, 5 on each front foot and 4 on each hind paw"
                }
              },
              "additionalProperties": false
        }
        """
        verifySchema(schema, expectedSchema)
    }

    @Test
    fun `Should generate schema for data class with various properties`() {
        @Description("A user model")
        data class User(
            @property:Description("The name of the user")
            val name: String,
            val age: Int?,
            val email: String = "n/a",
            val tags: List<String>,
            val attributes: Map<String, Int>?,
        )

        val schema = generator.generateSchema(User::class)

        // language=json
        val expectedSchema = """
        {
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
                  "type": "string",
                  "default": "n/a"
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
        """

        verifySchema(schema, expectedSchema)
    }

    @Test
    fun `Should generate schema with enum property`() {
        val schema = generator.generateSchema(WithEnum::class)

        // language=json
        val expectedSchema = """
        {
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
        """

        verifySchema(schema, expectedSchema)
    }
}
