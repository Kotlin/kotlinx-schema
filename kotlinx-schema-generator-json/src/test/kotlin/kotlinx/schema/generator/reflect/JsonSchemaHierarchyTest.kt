package kotlinx.schema.generator.reflect

import io.kotest.assertions.json.shouldEqualJson
import kotlinx.schema.Description
import kotlinx.schema.generator.core.SchemaGeneratorService
import kotlinx.schema.json.JsonSchema
import kotlinx.schema.json.encodeToString
import kotlinx.serialization.json.Json
import kotlin.reflect.KClass
import kotlin.test.Test

@Suppress("LongMethod", "unused")
class JsonSchemaHierarchyTest {
    @Description("Represents an animal")
    sealed class Animal {
        @Description("Animal's name")
        abstract val name: String

        @Description("Represents a dog")
        data class Dog(
            override val name: String,
            @property:Description("Dog's breed")
            val breed: String,
            @property:Description("Trained or not")
            val isTrained: Boolean = false,
        ) : Animal()

        @Description("Represents a cat")
        data class Cat(
            override val name: String,
            @property:Description("Cat's color")
            val color: String,
            @property:Description("Lives left")
            val lives: Int = 9,
        ) : Animal()
    }

    private val json =
        Json {
            prettyPrint = true
        }

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
    fun `Should generate schema for sealed hierarchy`() {
        val schema = generator.generateSchema(Animal::class)

        // language=json
        val expectedSchema = """
        {
            "name": "${Animal::class.qualifiedName}",
            "strict": false,
            "schema": {
              "type": "object",
              "additionalProperties": false,
              "description": "Represents an animal",
              "oneOf": [
                {
                  "type": "object",
                  "description": "Represents a cat",
                  "properties": {
                    "name": {
                      "type": "string"
                    },
                    "color": {
                      "type": "string",
                      "description": "Cat's color"
                    },
                    "lives": {
                      "type": "integer",
                      "description": "Lives left"
                    }
                  },
                  "required": ["name", "color"],
                  "additionalProperties": false
                },
                {
                  "type": "object",
                  "description": "Represents a dog",
                  "properties": {
                    "name": {
                      "type": "string"
                    },
                    "breed": {
                      "type": "string",
                      "description": "Dog's breed"
                    },
                    "isTrained": {
                      "type": "boolean",
                      "description": "Trained or not"
                    }
                  },
                  "required": ["name", "breed"],
                  "additionalProperties": false
                }
              ],
              "discriminator": {
                "propertyName": "type",
                "mapping": {
                  "Cat": "Cat",
                  "Dog": "Dog"
                }
              }
            }
        }
        """
        val actualSchema = schema.encodeToString(json)

        actualSchema shouldEqualJson expectedSchema
    }

    @Description("Container with nullable animal")
    data class AnimalContainer(
        @property:Description("Optional animal")
        val animal: Animal?,
    )

    @Test
    fun `Should generate schema for nullable sealed hierarchy`() {
        val schema = generator.generateSchema(AnimalContainer::class)

        // language=json
        val expectedSchema = """
        {
            "name": "${AnimalContainer::class.qualifiedName}",
            "strict": false,
            "schema": {
              "type": "object",
              "description": "Container with nullable animal",
              "properties": {
                "animal": {
                  "description": "Optional animal",
                  "anyOf": [
                    {
                      "oneOf": [
                        {
                          "type": "object",
                          "description": "Represents a cat",
                          "properties": {
                            "name": {
                              "type": "string"
                            },
                            "color": {
                              "type": "string",
                              "description": "Cat's color"
                            },
                            "lives": {
                              "type": "integer",
                              "description": "Lives left"
                            }
                          },
                          "required": ["name", "color"],
                          "additionalProperties": false
                        },
                        {
                          "type": "object",
                          "description": "Represents a dog",
                          "properties": {
                            "name": {
                              "type": "string"
                            },
                            "breed": {
                              "type": "string",
                              "description": "Dog's breed"
                            },
                            "isTrained": {
                              "type": "boolean",
                              "description": "Trained or not"
                            }
                          },
                          "required": ["name", "breed"],
                          "additionalProperties": false
                        }
                      ],
                      "discriminator": {
                        "propertyName": "type",
                        "mapping": {
                          "Cat": "Cat",
                          "Dog": "Dog"
                        }
                      }
                    },
                    {
                      "type": "null"
                    }
                  ]
                }
              },
              "required": ["animal"],
              "additionalProperties": false
            }
        }
        """
        val actualSchema = schema.encodeToString(json)

        actualSchema shouldEqualJson expectedSchema
    }
}
