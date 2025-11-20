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

```kotlin
import kotlinx.schema.json.*

val userSchema = jsonSchema {
    name = "User"
    strict = true
    description = "User profile schema"

    schema {
        required("id", "email", "name")
        additionalProperties = false

        property("id") {
            string {
                format = "uuid"
                description = "Unique user identifier"
            }
        }

        property("email") {
            string {
                format = "email"
                minLength = 5
                maxLength = 100
            }
        }

        property("name") {
            string {
                description = "User's full name"
            }
        }

        property("age") {
            integer {
                minimum = 0.0
                maximum = 150.0
            }
        }

        property("tags") {
            array {
                description = "User tags"
                items {
                    string()
                }
            }
        }
    }
}
```

### Serialization and deserialization

```kotlin
import kotlinx.serialization.json.Json

val json = Json { prettyPrint = true }

// Serialize to JSON string
val jsonString = json.encodeToString(JsonSchema.serializer(), userSchema)

// Deserialize from JSON string
val schema = json.decodeFromString(JsonSchema.serializer(), jsonString)
```

### Working with nested objects

```kotlin
val productSchema = jsonSchema {
    name = "Product"

    schema {
        property("metadata") {
            obj {
                description = "Product metadata"
                required("createdAt")

                property("createdAt") {
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

### Array of objects

```kotlin
val stepsSchema = jsonSchema {
    name = "ProcessSteps"

    schema {
        property("steps") {
            array {
                items {
                    obj {
                        required("explanation", "output")

                        property("explanation") {
                            string { description = "Step explanation" }
                        }

                        property("output") {
                            string { description = "Step output" }
                        }
                    }
                }
            }
        }
    }
}
```

### Default values and constraints

```kotlin
val configSchema = jsonSchema {
    name = "Configuration"

    schema {
        property("enabled") {
            boolean {
                description = "Feature enabled"
                defaultValue(true)
            }
        }

        property("maxRetries") {
            integer {
                defaultValue(3)
                minimum = 0.0
                maximum = 10.0
            }
        }

        property("status") {
            string {
                enum = listOf("active", "inactive", "pending")
                defaultValue("active")
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

- `required(vararg fields)` - Mark fields as required
- `nullable = true` - Allow null values
- `defaultValue(value)` - Set default value
- `constValue(value)` - Set constant value
- `enum = listOf(...)` - Enumerate allowed values
- `minimum`, `maximum` - Numeric bounds
- `minLength`, `maxLength` - String length constraints
- `minItems`, `maxItems` - Array size constraints
- `additionalProperties` - Allow/disallow additional properties

## DSL Safety

The DSL uses Kotlin's `@DslMarker` annotation to prevent scope pollution and ensure type-safe schema construction.
Builder classes have internal constructors, enforcing DSL usage through the `jsonSchema { }` entry point.
