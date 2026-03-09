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
        val expectedSchema =
            $$"""
        {
          "$schema": "https://json-schema.org/draft/2020-12/schema",
          "$id": "kotlinx.schema.generator.json.JsonSchemaHierarchyTest.Animal",
          "description": "Represents an animal",
          "type": "object",
          "additionalProperties": false,
          "oneOf": [
            {
              "$ref": "#/$defs/kotlinx.schema.generator.json.JsonSchemaHierarchyTest.Animal.Cat"
            },
            {
              "$ref": "#/$defs/kotlinx.schema.generator.json.JsonSchemaHierarchyTest.Animal.Dog"
            }
          ],
          "$defs": {
            "kotlinx.schema.generator.json.JsonSchemaHierarchyTest.Animal.Cat": {
              "type": "object",
              "description": "Represents a cat",
              "properties": {
                "type": {
                  "type": "string",
                  "const": "kotlinx.schema.generator.json.JsonSchemaHierarchyTest.Animal.Cat"
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
                  "description": "Lives left"
                }
              },
              "required": [
                "type",
                "name",
                "color",
                "lives"
              ],
              "additionalProperties": false
            },
            "kotlinx.schema.generator.json.JsonSchemaHierarchyTest.Animal.Dog": {
              "type": "object",
              "description": "Represents a dog",
              "properties": {
                "type": {
                  "type": "string",
                  "const": "kotlinx.schema.generator.json.JsonSchemaHierarchyTest.Animal.Dog"
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
                  "description": "Trained or not"
                }
              },
              "required": [
                "type",
                "name",
                "breed",
                "isTrained"
              ],
              "additionalProperties": false
            }
          }
        }
            """.trimIndent()
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
        val expectedSchema =
            $$"""
        {
          "$schema": "https://json-schema.org/draft/2020-12/schema",
          "$id": "kotlinx.schema.generator.json.JsonSchemaHierarchyTest.AnimalContainer",
          "description": "Container with nullable animal",
          "type": "object",
          "properties": {
            "animal": {
              "anyOf": [
                {
                  "oneOf": [
                    {
                      "$ref": "#/$defs/kotlinx.schema.generator.json.JsonSchemaHierarchyTest.Animal.Cat"
                    },
                    {
                      "$ref": "#/$defs/kotlinx.schema.generator.json.JsonSchemaHierarchyTest.Animal.Dog"
                    }
                  ]
                },
                {
                  "type": "null"
                }
              ],
              "description": "Optional animal"
            }
          },
          "additionalProperties": false,
          "required": [
            "animal"
          ],
          "$defs": {
            "kotlinx.schema.generator.json.JsonSchemaHierarchyTest.Animal.Cat": {
              "type": "object",
              "description": "Represents a cat",
              "properties": {
                "type": {
                      "type": "string",
                      "const": "kotlinx.schema.generator.json.JsonSchemaHierarchyTest.Animal.Cat"
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
                  "description": "Lives left"
                }
              },
              "required": [
                "type",
                "name",
                "color",
                "lives"
              ],
              "additionalProperties": false
            },
            "kotlinx.schema.generator.json.JsonSchemaHierarchyTest.Animal.Dog": {
              "type": "object",
              "description": "Represents a dog",
              "properties": {
                "type": {
                  "type": "string",
                  "const": "kotlinx.schema.generator.json.JsonSchemaHierarchyTest.Animal.Dog"
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
                  "description": "Trained or not"
                }
              },
              "required": [
                "type",
                "name",
                "breed",
                "isTrained"
              ],
              "additionalProperties": false
            }
          }
        }
            """.trimIndent()

        verifySchema(schema, expectedSchema)
    }

    @Test
    @Suppress("LongMethod")
    fun `Should generate schema with discriminator when includeDiscriminator is true`() {
        val json = kotlinx.serialization.json.Json { encodeDefaults = false }
        val generatorWithDiscriminator =
            ReflectionClassJsonSchemaGenerator(
                json = json,
                config = JsonSchemaConfig.OpenAPI,
            )

        val schema = generatorWithDiscriminator.generateSchema(Animal::class)

        // The OpenAPI discriminator mapping key MUST equal the "type" const value in each subtype.
        // Both the mapping key and the const value use the fully qualified class name so that
        // clients can dispatch correctly. The mapping value is the $ref path to the $defs entry.
        // language=json
        val expectedSchema = $$"""
        {
              "$schema": "https://json-schema.org/draft/2020-12/schema",
              "$id": "kotlinx.schema.generator.json.JsonSchemaHierarchyTest.Animal",
              "type": "object",
              "additionalProperties": false,
              "description": "Represents an animal",
              "oneOf": [
                {
                  "$ref": "#/$defs/kotlinx.schema.generator.json.JsonSchemaHierarchyTest.Animal.Cat"
                },
                {
                  "$ref": "#/$defs/kotlinx.schema.generator.json.JsonSchemaHierarchyTest.Animal.Dog"
                }
              ],
              "discriminator": {
                "propertyName": "type",
                "mapping": {
                  "kotlinx.schema.generator.json.JsonSchemaHierarchyTest.Animal.Cat": "#/$defs/kotlinx.schema.generator.json.JsonSchemaHierarchyTest.Animal.Cat",
                  "kotlinx.schema.generator.json.JsonSchemaHierarchyTest.Animal.Dog": "#/$defs/kotlinx.schema.generator.json.JsonSchemaHierarchyTest.Animal.Dog"
                }
              },
              "$defs": {
                "kotlinx.schema.generator.json.JsonSchemaHierarchyTest.Animal.Cat": {
                  "type": "object",
                  "description": "Represents a cat",
                  "properties": {
                    "type": {
                      "type": "string",
                      "const": "kotlinx.schema.generator.json.JsonSchemaHierarchyTest.Animal.Cat"
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
                "kotlinx.schema.generator.json.JsonSchemaHierarchyTest.Animal.Dog": {
                  "type": "object",
                  "description": "Represents a dog",
                  "properties": {
                    "type": {
                      "type": "string",
                      "const": "kotlinx.schema.generator.json.JsonSchemaHierarchyTest.Animal.Dog"
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
        """
        verifySchema(schema, expectedSchema)
    }
}
