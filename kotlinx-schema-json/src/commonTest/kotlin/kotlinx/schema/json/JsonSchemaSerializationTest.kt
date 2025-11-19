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
            this.type shouldBe "object"
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
            this.type shouldBe "object"
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
        schemaDefinition.type shouldBe "object"
        schemaDefinition.additionalProperties shouldBe false
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
            enum shouldBe listOf("active", "inactive", "pending")
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
                    additionalProperties shouldBe false
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
}
