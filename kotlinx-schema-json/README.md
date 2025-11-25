# kotlinx-schema-json

Kotlin Multiplatform library providing type-safe models and DSL for building JSON Schema definitions with serialization
support.

## Features

- **Type-safe data models** for JSON Schema Draft 2020-12
- **Kotlin DSL** for declarative schema construction
- **Kotlinx Serialization** integration for JSON serialization/deserialization
- **Property types**: string, number, integer, boolean, array, object, reference
- **Constraints**: required fields, additional properties, min/max, enum, const, nullable
- **Nested schemas** with full object and array support

## Installation

```kotlin
dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-schema-json:$version")
}
```

## Usage

### Building schemas with DSL

The DSL provides a type-safe way to build JSON Schema definitions.

#### Basic Example

```kotlin
val schema = jsonSchema {
    name = "UserEmail"
    strict = false
    schema {
        property("email") {
            required = true
            string {
                description = "Email address"
                format = "email"
            }
        }
    }
}
```

Produces:
```json
{
  "name": "UserEmail",
  "strict": false,
  "schema": {
    "type": "object",
    "properties": {
      "email": {
        "type": "string",
        "description": "Email address",
        "format": "email"
      }
    },
    "required": ["email"]
  }
}
```

#### Comprehensive Example

This example demonstrates all available DSL features in a single schema:

```kotlin
val schema = jsonSchema {
    name = "UserProfile"
    strict = true
    description = "Complete user profile schema"

    schema {
        // Schema metadata
        id = "https://example.com/schemas/user-profile"
        schema = "https://json-schema.org/draft-07/schema"
        additionalProperties = false

        // Required string with format constraint and description
        property("id") {
            required = true
            string {
                format = "uuid"
                description = "Unique identifier"
            }
        }

        // Required string with format and length constraints
        property("email") {
            required = true
            string {
                format = "email"
                description = "Email address"
                minLength = 5
                maxLength = 100
            }
        }

        // Integer with numeric constraints
        property("age") {
            integer {
                description = "Person's age"
                minimum = 0.0
                maximum = 150.0
            }
        }

        // Number with multipleOf constraint
        property("score") {
            number {
                description = "User score"
                minimum = 0.0
                maximum = 100.0
                multipleOf = 0.5
            }
        }

        // String enum
        property("status") {
            string {
                description = "Current status"
                enum = listOf("active", "inactive", "pending")
            }
        }

        // Boolean with a default value
        property("verified") {
            boolean {
                description = "Email verification status"
                default = false
            }
        }

        // Nullable property
        property("nickname") {
            string {
                description = "Optional nickname"
                nullable = true
            }
        }

        // Constant value
        property("apiVersion") {
            string {
                description = "API version"
                constValue = "v1.0"
            }
        }

        // Array of strings with size constraints
        property("tags") {
            array {
                description = "User tags"
                minItems = 1
                maxItems = 10
                ofString()
            }
        }

        // Nested object with required fields
        property("metadata") {
            obj {
                description = "User metadata"
                property("createdAt") {
                    required = true
                    string {
                        format = "date-time"
                    }
                }
                property("updatedAt") {
                    string {
                        format = "date-time"
                    }
                }
            }
        }

        // Array of objects
        property("activities") {
            array {
                description = "User activity log"
                ofObject {
                    additionalProperties = false
                    property("action") {
                        required = true
                        string {
                            description = "Action performed"
                        }
                    }
                    property("timestamp") {
                        required = true
                        string {
                            format = "date-time"
                            description = "When action occurred"
                        }
                    }
                }
            }
        }

        // Schema reference
        property("address") {
            reference("#/definitions/Address")
        }
    }
}
```

This comprehensive example includes:
- **Required fields**: `id`, `email` (with `required = true`)
- **String constraints**: `format`, `minLength`, `maxLength`, `enum`, `pattern`
- **Numeric constraints**: `minimum`, `maximum`, `multipleOf`, `exclusiveMinimum`, `exclusiveMaximum`
- **Default values**: Direct assignment `default = value` (automatically wrapped in `JsonPrimitive`)
- **Nullable properties**: Using `nullable = true`
- **Constant values**: Direct assignment `constValue = value` (automatically wrapped in `JsonPrimitive`)
- **Arrays**: With `minItems`, `maxItems`, and typed `items`
- **Nested objects**: Using `obj { }` with nested properties
- **Array of objects**: Complex array items with their own schemas
- **Schema references**: Using `reference()` for reusable schemas
- **Schema metadata**: `$id`, `$schema`, `strict`, `description`, `additionalProperties`

### Serialization and deserialization

```kotlin
import kotlinx.serialization.json.Json

val json = Json { prettyPrint = true }

// Serialize to JSON string
val jsonString = json.encodeToString(userSchema)

// Deserialize from JSON string
val schema = json.decodeFromString(jsonString)
```

### Working with nested objects

```kotlin
val productSchema = jsonSchema {
    name = "Product"

    schema {
        property("metadata") {
            obj {
                description = "Product metadata"

                property("createdAt") {
                    required = true
                    string { format = "date-time" }
                }

                property("updatedAt") {
                    string { format = "date-time" }
                }
            }
        }
    }
}
```

## API Overview

### Property Types

- `string { }` - String properties with `format`, `enum`, `pattern`, `minLength`, `maxLength`
- `integer { }` - Integer properties with constraints
- `number { }` - Numeric properties with constraints
- `boolean { }` - Boolean properties
- `array { }` - Array properties with `items` definition
- `obj { }` - Nested object properties
- `reference(ref)` - Schema references (`$ref`)

### Constraints

- `required = true` - Mark a property as required (set within the property block)
- `nullable = true` - Allow null values
- `default = value` - Set default value (automatically wrapped in `JsonPrimitive`, or use `JsonElement` directly)
- `constValue = value` - Set constant value (automatically wrapped in `JsonPrimitive`, or use `JsonElement` directly)
- `enum = listOf(...)` - Enumerate allowed values
- `minimum`, `maximum` - Numeric bounds
- `minLength`, `maxLength` - String length constraints
- `minItems`, `maxItems` - Array size constraints
- `additionalProperties` - Allow/disallow additional properties

**Note**: The legacy `required(vararg fields: String)` function is still available for backward compatibility, but using `required = true` within property blocks is the recommended approach.

## DSL Safety

The DSL uses Kotlin's `@DslMarker` annotation to prevent scope pollution and ensure type-safe schema construction.
Builder classes have internal constructors, enforcing DSL usage through the `jsonSchema { }` entry point.

## Function Calling Schema for LLM APIs

For LLM function calling APIs (OpenAI, Anthropic), use `FunctionCallingSchema` to represent function/tool definitions:

```kotlin
import kotlinx.schema.json.*

val schema = FunctionCallingSchema(
    name = "search",
    description = "Search for items in the database",
    parameters = ParametersDefinition(
        properties = mapOf(
            "query" to StringPropertyDefinition(
                type = listOf("string"),
                description = "Search query"
            ),
            "limit" to NumericPropertyDefinition(
                type = listOf("integer"),
                description = "Max results"
            )
        ),
        required = listOf("query", "limit")
    )
)
```

This produces the following JSON schema compatible with OpenAI's function calling format:

```json
{
  "type": "function",
  "name": "search",
  "description": "Search for items in the database",
  "strict": true,
  "parameters": {
    "type": "object",
    "properties": {
      "query": {
        "type": "string",
        "description": "Search query"
      },
      "limit": {
        "type": "integer",
        "description": "Max results"
      }
    },
    "required": ["query", "limit"],
    "additionalProperties": false
  }
}
```

### OpenAI Structured Outputs Compatibility

Function calling schemas follow OpenAI's [structured outputs requirements](https://platform.openai.com/docs/guides/function-calling):
- Functions must have a `name` and `description`
- All fields must be in the `required` array
- Nullable fields use union types: `["string", "null"]`
- The `nullable: true` flag is not used (it's ignored by OpenAI models)
- `additionalProperties` is set to `false` by default

Example with nullable parameter:
```kotlin
val schema = FunctionCallingSchema(
    name = "updateProfile",
    description = "Update user profile information",
    parameters = ParametersDefinition(
        properties = mapOf(
            "name" to StringPropertyDefinition(
                type = listOf("string"),
                description = "User's full name"
            ),
            "bio" to StringPropertyDefinition(
                type = listOf("string", "null"),
                description = "Optional biography"
            )
        ),
        required = listOf("name", "bio")
    )
)
```

### Runtime Generation from Functions

For generating function calling schemas from Kotlin functions at runtime, use `ReflectionFunctionCallingSchemaGenerator` in the `kotlinx-schema-generator-json` module:

```kotlin
import kotlinx.schema.Description
import kotlinx.schema.generator.json.ReflectionFunctionCallingSchemaGenerator

@Description("Search for users by name")
fun searchUsers(
    @Description("Name to search for") query: String,
    @Description("Maximum number of results") limit: Int = 10
): List<User> = TODO()

val generator = ReflectionFunctionCallingSchemaGenerator.Default
val schema = generator.generateSchema(::searchUsers)
```

This automatically generates a `FunctionCallingSchema` with the function name, description, 
and properly typed parameters:

```json
{
    "type": "function",
    "name": "searchUsers",
    "description": "Search for users by name",
    "strict": true,
    "parameters": {
        "properties": {
            "query": {
                "type": "string",
                "description": "Name to search for"
            },
            "limit": {
                "type": "integer",
                "description": "Maximum number of results"
            }
        },
        "required": [
            "query",
            "limit"
        ],
        "additionalProperties": false,
        "type": "object"
    }
}
```
