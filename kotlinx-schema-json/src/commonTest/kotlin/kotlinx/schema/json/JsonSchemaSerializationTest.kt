package kotlinx.schema.json

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.matchers.maps.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.Json
import kotlin.test.Test

@Suppress("LongMethod")
internal class JsonSchemaSerializationTest {
    private val jsonParser =
        Json {
            ignoreUnknownKeys = true
            prettyPrint = true
        }

    @Test
    fun `Should de-serialize simple JsonSchema`() {
        // language=json
        val json =
            """
            {
              "name" : "Person",
              "strict" : false,
              "schema" : {
                "type" : "object",
                "properties" : {
                  "name" : {
                    "type" : "string",
                    "description" : "Person's name",
                    "nullable" : false
                  },
                  "age" : {
                    "type" : "integer",
                    "description" : "Person's age"
                  },
                  "weight" : {
                    "type" : ["number", "null"],
                    "description" : "Weight in kilograms"
                  },
                  "height" : {
                    "type" : "number",
                    "description" : "Height in meters"
                  },
                  "married" : {
                    "type" : ["boolean", "null"]
                  }
                },
                "required" : [ "name", "age", "weight", "height", "married"]
              }
            }
            """.trimIndent()

        val schema = deserializeAndSerialize<JsonSchema>(json, jsonParser)

        schema.name shouldBe "Person"
        schema.strict shouldBe false
        schema.schema shouldNotBeNull {
            this.type shouldBe listOf("object")
            this.required shouldBeEqual listOf("name", "age", "weight", "height", "married")
            this.properties shouldNotBeNull {
                shouldHaveSize(5)
                this["name"] as? StringPropertyDefinition shouldNotBeNull {
                    type shouldBe listOf("string")
                }
                this["age"] as? NumericPropertyDefinition shouldNotBeNull {
                    type shouldBe listOf("integer")
                }
                this["weight"] as? NumericPropertyDefinition shouldNotBeNull {
                    type shouldBe listOf("number", "null")
                }
                this["height"] as? NumericPropertyDefinition shouldNotBeNull {
                    type shouldBe listOf("number")
                }
                this["married"] as? BooleanPropertyDefinition shouldNotBeNull {
                    type shouldBe listOf("boolean", "null")
                }
            }
        }
    }

    @Test
    fun `Should de-serialize Schema with id and schema`() {
        // language=json
        val json =
            """
            {
            
                "${'$'}schema": "https://json-schema.org/draft-07/schema", 
                "${'$'}id": "https://example.com/schemas/product", 
                "type" : "object",
                "properties" : {
                  "name" : {
                    "type" : "string",
                    "description" : "Person's name",
                    "nullable" : false
                  }
                },
                "required" : [ "name"]
            }
            """.trimIndent()

        val schema = deserializeAndSerialize<JsonSchemaDefinition>(json, jsonParser)

        schema shouldNotBeNull {
            this.schema shouldBe "https://json-schema.org/draft-07/schema"
            this.id shouldBe "https://example.com/schemas/product"
            this.type shouldBe listOf("object")
            this.required shouldBeEqual listOf("name")
            this.properties shouldNotBeNull {
                shouldHaveSize(1)
                this["name"] as? StringPropertyDefinition shouldNotBeNull {
                    type shouldBe listOf("string")
                }
            }
        }
    }

    @Test
    @Suppress("LongMethod")
    fun `Should deserialize complex JsonSchema`() {
        // language=json
        val json =
            """
            {
              "name": "ComplexSchema",
              "strict": true,
              "description": "A complex schema with various field types",
              "schema": {
                "type": "object",
                "properties": {
                  "id": {
                    "type": "string",
                    "format": "uuid",
                    "description": "Unique identifier"
                  },
                  "email": {
                    "type": "string",
                    "format": "email",
                    "pattern": "^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$",
                    "description": "Email address",
                    "minLength": 5,
                    "maxLength": 100
                  },
                  "age": {
                    "type": "integer",
                    "minimum": 18,
                    "maximum": 100,
                    "description": "Age between 18 and 100"
                  },
                  "score": {
                    "type": "number",
                    "minimum": 0,
                    "maximum": 10,
                    "default": 5,
                    "multipleOf": 0.5
                  },
                  "precision": {
                    "type": "number",
                    "exclusiveMinimum": 0,
                    "exclusiveMaximum": 1,
                    "const": 0.5
                  },
                  "status": {
                    "type": "string",
                    "enum": ["active", "inactive", "pending"],
                    "description": "Current status"
                  },
                  "tags": {
                    "type": "array",
                    "items": {
                      "type": "string"
                    },
                    "description": "List of tags",
                    "minItems": 1,
                    "maxItems": 10,
                    "default": ["default"]
                  },
                  "metadata": {
                    "type": "object",
                    "description": "Metadata about the user",
                    "properties": {
                      "createdAt": {
                        "type": "string",
                        "format": "date-time"
                      },
                      "updatedAt": {
                        "type": "string",
                        "format": "date-time"
                      }
                    },
                    "required": ["createdAt"]
                  },
                  "steps": {
                    "type": "array",
                    "description": "Steps taken by the user",
                    "items": {
                      "type": "object",
                      "properties": {
                        "explanation": { "type": "string" },
                        "output": { "type": "string" }
                      },
                      "required": ["explanation", "output"],
                      "additionalProperties": false
                    }
                  },
                  "nullable_field": {
                    "description": "Nullable field",
                    "type": "string",
                    "nullable": true
                  },
                  "flag": {
                    "type": "boolean",
                    "description": "Boolean flag",
                    "default": true
                  },
                  "constant_flag": {
                    "type": "boolean",
                    "description": "Constant boolean flag",
                    "const": false
                  },
                  "reference": {
                    "${'$'}ref": "#/definitions/ExternalType"
                  }
                },
                "required": ["id", "email", "status"],
                "additionalProperties": false
              }
            }
            """.trimIndent()

        val schema = deserializeAndSerialize<JsonSchema>(json, jsonParser)

        // Basic validation
        schema.name shouldBe "ComplexSchema"
        schema.strict shouldBe true
        schema.description shouldBe "A complex schema with various field types"

        // Schema validation
        val schemaDefinition = schema.schema
        schemaDefinition.type shouldBe listOf("object")
        schemaDefinition.additionalProperties shouldBe kotlinx.serialization.json.JsonPrimitive(false)
        schemaDefinition.required shouldHaveSize 3
        schemaDefinition.required shouldBe listOf("id", "email", "status")

        // Properties validation
        val properties = schemaDefinition.properties
        properties shouldHaveSize 13

        // String with format
        properties["id"] shouldNotBeNull {
            this as StringPropertyDefinition
            type shouldBe listOf("string")
            nullable shouldBe null
            format shouldBe "uuid"
            description shouldBe "Unique identifier"
        }

        // String with a pattern
        properties["email"] shouldNotBeNull {
            this as StringPropertyDefinition
            type shouldBe listOf("string")
            nullable shouldBe null
            format shouldBe "email"
            pattern shouldBe "^[^\\s@]+@[^\\s@]+\\.[^\\s@]+\$"
            description shouldBe "Email address"
            minLength shouldBe 5
            maxLength shouldBe 100
        }

        // Numeric with min/max
        properties["age"] shouldNotBeNull {
            this as NumericPropertyDefinition
            type shouldBe listOf("integer")
            nullable shouldBe null
            minimum shouldBe 18.0
            maximum shouldBe 100.0
            description shouldBe "Age between 18 and 100"
        }

        // Enum field
        properties["status"] shouldNotBeNull {
            this as StringPropertyDefinition
            description shouldBe "Current status"
            enum shouldBe listOf(
                kotlinx.serialization.json.JsonPrimitive("active"),
                kotlinx.serialization.json.JsonPrimitive("inactive"),
                kotlinx.serialization.json.JsonPrimitive("pending")
            )
            nullable shouldBe null
        }

        // Array field
        properties["tags"] shouldNotBeNull {
            this as ArrayPropertyDefinition
            description shouldBe "List of tags"
            nullable shouldBe null
            default.shouldNotBeNull()
            items shouldNotBeNull {
                type shouldBe listOf("array")
                nullable shouldBe null
                description shouldBe "List of tags"
                (this as? StringPropertyDefinition)?.enum.shouldBeNull()
                minItems shouldBe 1u
                maxItems shouldBe 10u
            }
        }

        // Object field with nested properties
        properties["metadata"] shouldNotBeNull {
            this as ObjectPropertyDefinition
            description shouldBe "Metadata about the user"
            nullable shouldBe null
            this.properties.shouldNotBeNull()
            this.properties shouldHaveSize 2
        }

        // Nullable field
        properties["nullable_field"] shouldNotBeNull {
            this as StringPropertyDefinition
            description shouldBe "Nullable field"
            nullable shouldBe true
            enum.shouldBeNull()
            format.shouldBeNull()
            pattern.shouldBeNull()
        }

        // Array of objects
        properties["steps"] shouldNotBeNull {
            this as ArrayPropertyDefinition
            description shouldBe "Steps taken by the user"
            nullable shouldBe null
            items shouldNotBeNull {
                this as ObjectPropertyDefinition
                type shouldBe listOf("object")
                nullable.shouldBeNull()
                description.shouldBeNull()
                this.properties shouldNotBeNull {
                    shouldHaveSize(2)
                    this["explanation"] shouldNotBeNull {
                        this as StringPropertyDefinition
                        type shouldBe listOf("string")
                    }
                    this["output"] shouldNotBeNull {
                        this as StringPropertyDefinition
                        type shouldBe listOf("string")
                    }
                    additionalProperties shouldBe kotlinx.serialization.json.JsonPrimitive(false)
                }
            }
        }

        // Numeric with multipleOf
        properties["score"] shouldNotBeNull {
            this as NumericPropertyDefinition
            type shouldBe listOf("number")
            nullable.shouldBeNull()
            minimum shouldBe 0.0
            maximum shouldBe 10.0
            multipleOf shouldBe 0.5
            default.shouldNotBeNull()
        }

        // Numeric with exclusiveMinimum and exclusiveMaximum
        properties["precision"] shouldNotBeNull {
            this as NumericPropertyDefinition
            type shouldBe listOf("number")
            nullable.shouldBeNull()
            exclusiveMinimum shouldBe 0.0
            exclusiveMaximum shouldBe 1.0
            constValue.shouldNotBeNull()
        }

        // Boolean with default
        properties["flag"] shouldNotBeNull {
            this as BooleanPropertyDefinition
            type shouldBe listOf("boolean")
            nullable.shouldBeNull()
            description shouldBe "Boolean flag"
            default.shouldNotBeNull()
        }

        // Boolean with const
        properties["constant_flag"] shouldNotBeNull {
            this as BooleanPropertyDefinition
            type shouldBe listOf("boolean")
            nullable.shouldBeNull()
            description shouldBe "Constant boolean flag"
            constValue.shouldNotBeNull()
        }

        // Reference property
        properties["reference"] shouldNotBeNull {
            this as ReferencePropertyDefinition
            ref shouldBe "#/definitions/ExternalType"
        }
    }

    @Test
    fun `Should serialize and deserialize polymorphic schema with defs and ref`() {
        // language=json
        val json =
            """
            {
              "name": "Animal",
              "strict": false,
              "schema": {
                "type": "object",
                "additionalProperties": false,
                "description": "Animal hierarchy",
                "oneOf": [
                  {
                    "${'$'}ref": "#/${'$'}defs/Cat"
                  },
                  {
                    "${'$'}ref": "#/${'$'}defs/Dog"
                  }
                ],
                "discriminator": {
                  "propertyName": "type",
                  "mapping": {
                    "Cat": "#/${'$'}defs/Cat",
                    "Dog": "#/${'$'}defs/Dog"
                  }
                },
                "${'$'}defs": {
                  "Cat": {
                    "type": "object",
                    "description": "A cat",
                    "properties": {
                      "name": {
                        "type": "string"
                      },
                      "lives": {
                        "type": "integer"
                      }
                    },
                    "required": ["name"],
                    "additionalProperties": false
                  },
                  "Dog": {
                    "type": "object",
                    "description": "A dog",
                    "properties": {
                      "name": {
                        "type": "string"
                      },
                      "breed": {
                        "type": "string"
                      }
                    },
                    "required": ["name", "breed"],
                    "additionalProperties": false
                  }
                }
              }
            }
            """.trimIndent()

        val schema = deserializeAndSerialize<JsonSchema>(json, jsonParser)

        // Validate basic structure
        schema.name shouldBe "Animal"
        schema.strict shouldBe false

        val schemaDefinition = schema.schema
        schemaDefinition.type shouldBe listOf("object")
        schemaDefinition.description shouldBe "Animal hierarchy"

        // Validate oneOf with $ref
        schemaDefinition.oneOf shouldNotBeNull {
            shouldHaveSize(2)
            this[0] shouldNotBeNull {
                this as ReferencePropertyDefinition
                ref shouldBe "#/\$defs/Cat"
            }
            this[1] shouldNotBeNull {
                this as ReferencePropertyDefinition
                ref shouldBe "#/\$defs/Dog"
            }
        }

        // Validate discriminator
        schemaDefinition.discriminator shouldNotBeNull {
            propertyName shouldBe "type"
            mapping shouldNotBeNull {
                shouldHaveSize(2)
                this["Cat"] shouldBe "#/\$defs/Cat"
                this["Dog"] shouldBe "#/\$defs/Dog"
            }
        }

        // Validate $defs
        schemaDefinition.defs shouldNotBeNull {
            shouldHaveSize(2)
            this["Cat"] shouldNotBeNull {
                this as ObjectPropertyDefinition
                type shouldBe listOf("object")
                description shouldBe "A cat"
                properties shouldNotBeNull {
                    shouldHaveSize(2)
                    this["name"] shouldNotBeNull {
                        this as StringPropertyDefinition
                        type shouldBe listOf("string")
                    }
                    this["lives"] shouldNotBeNull {
                        this as NumericPropertyDefinition
                        type shouldBe listOf("integer")
                    }
                }
                required shouldBe listOf("name")
            }
            this["Dog"] shouldNotBeNull {
                this as ObjectPropertyDefinition
                type shouldBe listOf("object")
                description shouldBe "A dog"
                properties shouldNotBeNull {
                    shouldHaveSize(2)
                    this["name"] shouldNotBeNull {
                        this as StringPropertyDefinition
                        type shouldBe listOf("string")
                    }
                    this["breed"] shouldNotBeNull {
                        this as StringPropertyDefinition
                        type shouldBe listOf("string")
                    }
                }
                required shouldBe listOf("name", "breed")
            }
        }
    }

    @Test
    fun `Should serialize and deserialize nullable polymorphic schema with anyOf`() {
        // language=json
        val json =
            """
            {
              "name": "Container",
              "strict": false,
              "schema": {
                "type": "object",
                "properties": {
                  "animal": {
                    "description": "Optional animal",
                    "anyOf": [
                      {
                        "oneOf": [
                          {
                            "${'$'}ref": "#/${'$'}defs/Cat"
                          },
                          {
                            "${'$'}ref": "#/${'$'}defs/Dog"
                          }
                        ],
                        "discriminator": {
                          "propertyName": "type",
                          "mapping": {
                            "Cat": "#/${'$'}defs/Cat",
                            "Dog": "#/${'$'}defs/Dog"
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
                "${'$'}defs": {
                  "Cat": {
                    "type": "object",
                    "properties": {
                      "name": {
                        "type": "string"
                      }
                    },
                    "required": ["name"],
                    "additionalProperties": false
                  },
                  "Dog": {
                    "type": "object",
                    "properties": {
                      "name": {
                        "type": "string"
                      }
                    },
                    "required": ["name"],
                    "additionalProperties": false
                  }
                }
              }
            }
            """.trimIndent()

        val schema = deserializeAndSerialize<JsonSchema>(json, jsonParser)

        schema.name shouldBe "Container"
        val schemaDefinition = schema.schema

        // Validate properties with anyOf
        schemaDefinition.properties["animal"] shouldNotBeNull {
            this as AnyOfPropertyDefinition
            description shouldBe "Optional animal"
            anyOf shouldHaveSize 2

            // First option should be oneOf with refs
            anyOf[0] shouldNotBeNull {
                this as OneOfPropertyDefinition
                oneOf shouldHaveSize 2
                oneOf[0] shouldNotBeNull {
                    this as ReferencePropertyDefinition
                    ref shouldBe "#/\$defs/Cat"
                }
                discriminator shouldNotBeNull {
                    propertyName shouldBe "type"
                }
            }

            // Second option should be null type
            // Note: {"type": "null"} deserializes as StringPropertyDefinition (fallback)
            anyOf[1] shouldNotBeNull {
                this as StringPropertyDefinition
                type shouldBe listOf("null")
            }
        }

        // Validate $defs
        schemaDefinition.defs shouldNotBeNull {
            shouldHaveSize(2)
        }
    }
}
