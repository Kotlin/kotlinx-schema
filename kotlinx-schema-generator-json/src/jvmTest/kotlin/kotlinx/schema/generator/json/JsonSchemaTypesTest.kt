package kotlinx.schema.generator.json

import io.kotest.assertions.json.shouldEqualJson
import kotlinx.schema.Schema
import kotlinx.schema.generator.core.SchemaGenerator
import kotlinx.schema.json.JsonSchema
import kotlinx.serialization.json.Json
import kotlin.reflect.KClass
import kotlin.test.Test

class JsonSchemaTypesTest {
    private val generator: SchemaGenerator<KClass<out Any>, JsonSchema> =
        ReflectionClassJsonSchemaGenerator(
            json = Json { encodeDefaults = false },
            config = JsonSchemaConfig.Default,
        )

    // Primitive Types Tests

    @Test
    fun `Should handle all numeric types correctly`() {
        val schema = generator.generateSchemaString(WithNumericTypes::class)

        schema shouldEqualJson
            // language=JSON
            $$"""
            {
              "$schema": "https://json-schema.org/draft/2020-12/schema",
              "$id": "kotlinx.schema.generator.json.WithNumericTypes",
              "description": "Class with numeric types",
              "type": "object",
              "properties": {
                "intVal": {
                  "type": "integer",
                  "description": "Int value"
                },
                "longVal": {
                  "type": "integer",
                  "description": "Long value"
                },
                "floatVal": {
                  "type": "number",
                  "description": "Float value"
                },
                "doubleVal": {
                  "type": "number",
                  "description": "Double value"
                },
                "nullableInt": {
                  "type": [
                    "integer",
                    "null"
                  ],
                  "description": "Nullable int"
                },
                "nullableLong": {
                  "type": [
                    "integer",
                    "null"
                  ],
                  "description": "Nullable long"
                },
                "nullableFloat": {
                  "type": [
                    "number",
                    "null"
                  ],
                  "description": "Nullable float"
                },
                "nullableDouble": {
                  "type": [
                    "number",
                    "null"
                  ],
                  "description": "Nullable double"
                }
              },
              "required": [
                "intVal",
                "longVal",
                "floatVal",
                "doubleVal"
              ],
              "additionalProperties": false
            }
            """
    }

    @Test
    fun `Should handle enum types`() {
        val schema = generator.generateSchemaString(WithEnum::class)

        schema shouldEqualJson
            // language=JSON
            $$"""
            {
              "$schema": "https://json-schema.org/draft/2020-12/schema",
              "$id": "kotlinx.schema.generator.json.WithEnum",
              "description": "Class with enum",
              "type": "object",
              "properties": {
                "status": {
                  "type": "string",
                  "description": "Status",
                  "enum": [
                    "ACTIVE",
                    "INACTIVE",
                    "PENDING"
                  ]
                },
                "optStatus": {
                  "type": [
                    "string",
                    "null"
                  ],
                  "description": "Optional status",
                  "enum": [
                    "ACTIVE",
                    "INACTIVE",
                    "PENDING"
                  ]
                }
              },
              "required": [
                "status"
              ],
              "additionalProperties": false
            }
            """
    }

    // Collection Types Tests

    @Test
    fun `Should handle collection types`() {
        val schema = generator.generateSchemaString(WithCollections::class)

        schema shouldEqualJson
            // language=JSON
            $$"""
            {
              "$schema": "https://json-schema.org/draft/2020-12/schema",
              "$id": "kotlinx.schema.generator.json.WithCollections",
              "description": "Class with collections",
              "type": "object",
              "properties": {
                "items": {
                  "type": "array",
                  "description": "String list",
                  "items": {
                    "type": "string"
                  }
                },
                "data": {
                  "type": "object",
                  "description": "String to int map",
                  "additionalProperties": {
                    "type": "integer"
                  }
                },
                "optList": {
                  "type": [
                    "array",
                    "null"
                  ],
                  "description": "Nullable list",
                  "items": {
                    "type": "string"
                  }
                },
                "optMap": {
                  "type": [
                    "object",
                    "null"
                  ],
                  "description": "Nullable map",
                  "additionalProperties": {
                    "type": "string"
                  }
                }
              },
              "required": [
                "items",
                "data"
              ],
              "additionalProperties": false
            }
            """
    }

    @Test
    fun `Should handle list of nested objects`() {
        val schema = generator.generateSchemaString(ListOfNested::class)

        schema shouldEqualJson
            // language=JSON
            $$"""
            {
              "$schema": "https://json-schema.org/draft/2020-12/schema",
              "$id": "kotlinx.schema.generator.json.ListOfNested",
              "description": "List of nested",
              "type": "object",
              "properties": {
                "items": {
                  "type": "array",
                  "description": "Items",
                  "items": {
                    "type": "object",
                    "description": "Nested object",
                    "properties": {
                      "street": {
                        "type": "string",
                        "description": "Street name"
                      },
                      "city": {
                        "type": "string",
                        "description": "City name"
                      }
                    },
                    "required": [
                      "street",
                      "city"
                    ],
                    "additionalProperties": false
                  }
                },
                "optionalItems": {
                  "type": [
                    "array",
                    "null"
                  ],
                  "description": "Optional items",
                  "items": {
                    "type": "object",
                    "description": "Nested object",
                    "properties": {
                      "street": {
                        "type": "string",
                        "description": "Street name"
                      },
                      "city": {
                        "type": "string",
                        "description": "City name"
                      }
                    },
                    "required": [
                      "street",
                      "city"
                    ],
                    "additionalProperties": false
                  }
                }
              },
              "required": [
                "items"
              ],
              "additionalProperties": false
            }
            """
    }

    @Test
    fun `Should handle map of nested objects`() {
        val schema = generator.generateSchemaString(MapOfNested::class)

        schema shouldEqualJson
            // language=JSON
            $$"""
            {
              "$schema": "https://json-schema.org/draft/2020-12/schema",
              "$id": "kotlinx.schema.generator.json.MapOfNested",
              "description": "Map of nested",
              "type": "object",
              "properties": {
                "data": {
                  "type": "object",
                  "description": "Data",
                  "additionalProperties": {
                    "type": "object",
                    "description": "Nested object",
                    "properties": {
                      "street": {
                        "type": "string",
                        "description": "Street name"
                      },
                      "city": {
                        "type": "string",
                        "description": "City name"
                      }
                    },
                    "required": [
                      "street",
                      "city"
                    ],
                    "additionalProperties": false
                  }
                },
                "optionalData": {
                  "type": [
                    "object",
                    "null"
                  ],
                  "description": "Optional data",
                  "additionalProperties": {
                    "type": "object",
                    "description": "Nested object",
                    "properties": {
                      "street": {
                        "type": "string",
                        "description": "Street name"
                      },
                      "city": {
                        "type": "string",
                        "description": "City name"
                      }
                    },
                    "required": [
                      "street",
                      "city"
                    ],
                    "additionalProperties": false
                  }
                }
              },
              "required": [
                "data"
              ],
              "additionalProperties": false
            }
            """
    }

    // Nested Object Tests

    @Test
    fun `Should handle nested objects`() {
        val schema = generator.generateSchemaString(WithNested::class)

        schema shouldEqualJson
            // language=JSON
            $$"""
            {
              "$schema": "https://json-schema.org/draft/2020-12/schema",
              "$id": "kotlinx.schema.generator.json.WithNested",
              "description": "Class with nested object",
              "type": "object",
              "properties": {
                "name": {
                  "type": "string",
                  "description": "Name"
                },
                "address": {
                  "type": "object",
                  "description": "Address",
                  "properties": {
                    "street": {
                      "type": "string",
                      "description": "Street name"
                    },
                    "city": {
                      "type": "string",
                      "description": "City name"
                    }
                  },
                  "required": [
                    "street",
                    "city"
                  ],
                  "additionalProperties": false
                },
                "optAddress": {
                  "type": [
                    "object",
                    "null"
                  ],
                  "description": "Optional address",
                  "properties": {
                    "street": {
                      "type": "string",
                      "description": "Street name"
                    },
                    "city": {
                      "type": "string",
                      "description": "City name"
                    }
                  },
                  "required": [
                    "street",
                    "city"
                  ],
                  "additionalProperties": false
                }
              },
              "required": [
                "name",
                "address"
              ],
              "additionalProperties": false
            }
            """
    }

    @Test
    fun `Should handle deeply nested structures`() {
        val schema = generator.generateSchemaString(DeepNested::class)

        schema shouldEqualJson
            // language=JSON
            $$"""
            {
              "$schema": "https://json-schema.org/draft/2020-12/schema",
              "$id": "kotlinx.schema.generator.json.DeepNested",
              "description": "Deep nested structure",
              "type": "object",
              "properties": {
                "level1": {
                  "type": "object",
                  "description": "Level 1",
                  "properties": {
                    "level2": {
                      "type": "object",
                      "description": "Level 2",
                      "properties": {
                        "value": {
                          "type": "integer",
                          "description": "Value"
                        },
                        "level3": {
                          "type": "object",
                          "description": "Level 3",
                          "properties": {
                            "value": {
                              "type": "string",
                              "description": "Value"
                            }
                          },
                          "required": [
                            "value"
                          ],
                          "additionalProperties": false
                        },
                        "optional": {
                          "type": [
                            "string",
                            "null"
                          ],
                          "description": "Optional value"
                        }
                      },
                      "required": [
                        "value",
                        "level3"
                      ],
                      "additionalProperties": false
                    },
                    "value": {
                      "type": "string",
                      "description": "Value"
                    }
                  },
                  "required": [
                    "level2",
                    "value"
                  ],
                  "additionalProperties": false
                }
              },
              "required": [
                "level1"
              ],
              "additionalProperties": false
            }
            """
    }

    // Required Fields Tests

    @Test
    fun `Should correctly distinguish required vs optional vs default`() {
        val schema = generator.generateSchemaString(MixedRequiredOptional::class)

        schema shouldEqualJson
            // language=JSON
            $$"""
            {
              "$schema": "https://json-schema.org/draft/2020-12/schema",
              "$id": "kotlinx.schema.generator.json.MixedRequiredOptional",
              "description": "Mixed required and optional",
              "type": "object",
              "properties": {
                "req1": {
                  "type": "string",
                  "description": "Required string"
                },
                "req2": {
                  "type": "integer",
                  "description": "Required int"
                },
                "opt1": {
                  "type": [
                    "string",
                    "null"
                  ],
                  "description": "Optional string"
                },
                "opt2": {
                  "type": [
                    "integer",
                    "null"
                  ],
                  "description": "Optional int"
                },
                "def1": {
                  "type": "string",
                  "description": "Default string",
                  "default": "default"
                },
                "def2": {
                  "type": "integer",
                  "description": "Default int",
                  "default": 42
                }
              },
              "required": [
                "req1",
                "req2"
              ],
              "additionalProperties": false
            }
            """
    }

    @Test
    fun `Should handle class with all optional fields`() {
        val schema = generator.generateSchemaString(EmptyClass::class)

        schema shouldEqualJson
            // language=JSON
            $$"""
            {
              "$schema": "https://json-schema.org/draft/2020-12/schema",
              "$id": "kotlinx.schema.generator.json.EmptyClass",
              "description": "Empty class",
              "type": "object",
              "properties": {
                "dummy": {
                  "type": [
                    "string",
                    "null"
                  ],
                  "default": "ignored"
                }
              },
              "additionalProperties": false
            }
            """
    }

    @Test
    fun `Should handle class with single required field`() {
        val schema = generator.generateSchemaString(SingleRequired::class)

        schema shouldEqualJson
            // language=JSON
            $$"""
            {
              "$schema": "https://json-schema.org/draft/2020-12/schema",
              "$id": "kotlinx.schema.generator.json.SingleRequired",
              "description": "Single required field",
              "type": "object",
              "properties": {
                "value": {
                  "type": "string",
                  "description": "Only field"
                }
              },
              "required": [
                "value"
              ],
              "additionalProperties": false
            }
            """
    }

    // Description Preservation Tests

    @Test
    fun `Should preserve descriptions through transformations`() {
        val schema = generator.generateSchemaString(MixedRequiredOptional::class)

        schema shouldEqualJson
            // language=JSON
            $$"""
            {
              "$schema": "https://json-schema.org/draft/2020-12/schema",
              "$id": "kotlinx.schema.generator.json.MixedRequiredOptional",
              "description": "Mixed required and optional",
              "type": "object",
              "properties": {
                "req1": {
                  "type": "string",
                  "description": "Required string"
                },
                "req2": {
                  "type": "integer",
                  "description": "Required int"
                },
                "opt1": {
                  "type": [
                    "string",
                    "null"
                  ],
                  "description": "Optional string"
                },
                "opt2": {
                  "type": [
                    "integer",
                    "null"
                  ],
                  "description": "Optional int"
                },
                "def1": {
                  "type": "string",
                  "description": "Default string",
                  "default": "default"
                },
                "def2": {
                  "type": "integer",
                  "description": "Default int",
                  "default": 42
                }
              },
              "required": [
                "req1",
                "req2"
              ],
              "additionalProperties": false
            }
            """
    }

    // Nullable Optional Fields Tests

    @Test
    fun `Should handle nullable optional fields with default config`() {
        val schema = generator.generateSchemaString(PersonWithOptionals::class)

        schema shouldEqualJson
            // language=JSON
            $$"""
            {
              "$schema": "https://json-schema.org/draft/2020-12/schema",
              "$id": "kotlinx.schema.generator.json.PersonWithOptionals",
              "description": "Person with various optional fields",
              "type": "object",
              "properties": {
                "name": {
                  "type": "string",
                  "description": "Name"
                },
                "age": {
                  "type": [
                    "integer",
                    "null"
                  ],
                  "description": "Age"
                },
                "email": {
                  "type": [
                    "string",
                    "null"
                  ],
                  "description": "Email"
                },
                "score": {
                  "type": [
                    "number",
                    "null"
                  ],
                  "description": "Score"
                },
                "active": {
                  "type": [
                    "boolean",
                    "null"
                  ],
                  "description": "Active"
                }
              },
              "required": [
                "name"
              ],
              "additionalProperties": false
            }
            """
    }

    @Schema
    @Suppress("MayBeConstant")
    data object ObjectWithProps {
        val foo = "bar"
        val num = 42
    }

    @Test
    fun `should handle data object schema with constant values`() {
        val schema = ReflectionClassJsonSchemaGenerator().generateSchemaString(ObjectWithProps::class)
        schema shouldEqualJson """
            {
              "${'$'}schema": "https://json-schema.org/draft/2020-12/schema",
              "${'$'}id": "kotlinx.schema.generator.json.JsonSchemaTypesTest.ObjectWithProps",
              "type": "object",
              "properties": {
                "foo": {
                  "type": "string",
                  "const": "bar"
                },
                "num": {
                  "type": "integer",
                  "const": 42
                }
              },
              "required": [
                "foo",
                "num"
              ],
              "additionalProperties": false
            }
            """
    }
}
