# Module kotlinx-schema-json

Type-safe JSON Schema models and DSL compliant with [JSON Schema Draft 2020-12](https://json-schema.org/draft/2020-12/schema).

Provides Kotlin models for programmatic JSON Schema construction with full kotlinx-serialization support.

**Platform Support:** Multiplatform (Common, JVM, JS, Native, Wasm) • Kotlin 2.2+ • Requires kotlinx-serialization-json

## Key Classes

- [JsonSchema] - root schema model with type-safe property definitions
- [FunctionCallingSchema] - OpenAI/Anthropic function calling format
- DSL builders - fluent API for schema construction

## Example

```kotlin
val schema = jsonSchema {
    name = "User"
    schema {
        property("id") {
            required = true
            string { format = "uuid" }
        }
        property("role") {
            oneOf {
                discriminator(propertyName = "type") {
                    "admin" mappedTo "#/definitions/AdminRole"
                }
            }
        }
    }
}
```

## Features

- Type-safe property definitions (string, number, integer, boolean, array, object)
- Polymorphism support (`oneOf`, `anyOf`, `allOf` with discriminators)
- Type-safe enums using native Kotlin collections
- Full kotlinx-serialization integration

## Related Specifications

- [JSON Schema Draft 2020-12](https://json-schema.org/draft/2020-12/schema)
- [OpenAI Function Calling](https://platform.openai.com/docs/guides/function-calling)
- [Anthropic Structured Outputs](https://platform.claude.com/docs/en/build-with-claude/structured-outputs)

# Package kotlinx.schema.json

JSON Schema models, DSL builders, and function calling schema support.

# Package kotlinx.schema.json.serializers

Kotlinx-serialization serializers for JSON Schema types.
