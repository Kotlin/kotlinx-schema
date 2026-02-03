# Module kotlinx-schema-generator-json

Runtime JSON Schema generation from Kotlin classes and functions.

Concrete implementations of schema generators for JVM runtime analysis via Kotlin reflection and kotlinx-serialization.

**Platform Support:** JVM only (requires Kotlin reflection) • Kotlin 2.2+

## Generators

- [ReflectionClassJsonSchemaGenerator] - generates schemas from any KClass via reflection
- [ReflectionFunctionCallingSchemaGenerator] - generates function calling schemas from KCallable
- [SerializationClassJsonSchemaGenerator] - generates schemas from @Serializable classes

## Example

```kotlin
// Class schema generation
val generator = ReflectionClassJsonSchemaGenerator.Default
val schema: JsonObject = generator.generateSchema(User::class)

// Function schema generation
val funcGenerator = ReflectionFunctionCallingSchemaGenerator.Default
val funcSchema = funcGenerator.generateSchema(::myFunction)
```

## Features

- Analyze third-party classes without source modification
- Extract default values from data class properties
- Recognize foreign annotations (Jackson, LangChain4j, Koog)
- OpenAI/Anthropic function calling format
- Sealed class hierarchies with `oneOf`

## Limitations

- KDoc annotations not available at runtime
- Function parameter defaults cannot be extracted (data class property defaults work)

# Package kotlinx.schema.generator.json

Reflection-based JSON Schema generators and configuration.

# Package kotlinx.schema.generator.json.serialization

Schema generation from kotlinx-serialization descriptors.
