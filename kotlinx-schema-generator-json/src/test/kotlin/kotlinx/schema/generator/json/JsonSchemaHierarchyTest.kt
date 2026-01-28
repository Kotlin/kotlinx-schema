package kotlinx.schema.generator.json

import kotlinx.schema.Description
import kotlinx.schema.generator.core.SchemaGeneratorService
import kotlinx.schema.json.JsonSchema
import kotlin.reflect.KClass
import kotlin.test.Test

class JsonSchemaHierarchyTest {
    @Description("Represents an animal")
    @Suppress("unused")
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
    @Suppress("LongMethod")
    fun `Should generate schema for sealed hierarchy`() {
        val schema = generator.generateSchema(Animal::class)

        // language=json
        val expectedSchema = $$"""
        {
            "name": "$${Animal::class.qualifiedName}",
            "strict": false,
            "schema": {
              "type": "object",
              "additionalProperties": false,
              "description": "Represents an animal",
              "oneOf": [
                {
                  "$ref": "#/$defs/Animal.Cat"
                },
                {
                  "$ref": "#/$defs/Animal.Dog"
                }
              ],
              "discriminator": {
                "propertyName": "type",
                "mapping": {
                  "Cat": "#/$defs/Animal.Cat",
                  "Dog": "#/$defs/Animal.Dog"
                }
              },
              "$defs": {
                "Animal.Cat": {
                  "type": "object",
                  "description": "Represents a cat",
                  "properties": {
                    "type": {
                      "type": "string",
                      "default": "Animal.Cat"
                    },
                    "name": {
                      "type": "string",
                      "description": "Animal's name"
                    },
                    "color": {
                      "type": "string",
                      "description": "Cat's color"
                    },
                    "lives": {
                      "type": "integer",
                      "description": "Lives left",
                      "default": 9
                    }
                  },
                  "required": ["type", "name", "color"],
                  "additionalProperties": false
                },
                "Animal.Dog": {
                  "type": "object",
                  "description": "Represents a dog",
                  "properties": {
                    "type": {
                      "type": "string",
                      "default": "Animal.Dog"
                    },
                    "name": {
                      "type": "string",
                      "description": "Animal's name"
                    },
                    "breed": {
                      "type": "string",
                      "description": "Dog's breed"
                    },
                    "isTrained": {
                      "type": "boolean",
                      "description": "Trained or not",
                      "default": false
                    }
                  },
                  "required": ["type", "name", "breed"],
                  "additionalProperties": false
                }
              }
            }
        }
        """
        verifySchema(schema, expectedSchema)
    }

    @Description("Container with nullable animal")
    data class AnimalContainer(
        @property:Description("Optional animal")
        val animal: Animal?,
    )

    @Test
    @Suppress("LongMethod")
    fun `Should generate schema for nullable sealed hierarchy`() {
        val schema = generator.generateSchema(AnimalContainer::class)

        // language=json
        val expectedSchema = $$"""
        {
            "name": "$${AnimalContainer::class.qualifiedName}",
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
                          "$ref": "#/$defs/Animal.Cat"
                        },
                        {
                          "$ref": "#/$defs/Animal.Dog"
                        }
                      ],
                      "discriminator": {
                        "propertyName": "type",
                        "mapping": {
                          "Cat": "#/$defs/Animal.Cat",
                          "Dog": "#/$defs/Animal.Dog"
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
              "additionalProperties": false,
              "$defs": {
                "Animal.Cat": {
                  "type": "object",
                  "description": "Represents a cat",
                  "properties": {
                    "type": {
                      "type": "string",
                      "default": "Animal.Cat"
                    },
                    "name": {
                      "type": "string",
                      "description": "Animal's name"
                    },
                    "color": {
                      "type": "string",
                      "description": "Cat's color"
                    },
                    "lives": {
                      "type": "integer",
                      "description": "Lives left",
                      "default": 9
                    }
                  },
                  "required": ["type", "name", "color"],
                  "additionalProperties": false
                },
                "Animal.Dog": {
                  "type": "object",
                  "description": "Represents a dog",
                  "properties": {
                    "type": {
                      "type": "string",
                      "default": "Animal.Dog"
                    },
                    "name": {
                      "type": "string",
                      "description": "Animal's name"
                    },
                    "breed": {
                      "type": "string",
                      "description": "Dog's breed"
                    },
                    "isTrained": {
                      "type": "boolean",
                      "description": "Trained or not",
                      "default": false
                    }
                  },
                  "required": ["type", "name", "breed"],
                  "additionalProperties": false
                }
              }
            }
        }
        """

        verifySchema(schema, expectedSchema)
    }
}
