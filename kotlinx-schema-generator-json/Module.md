# Module kotlinx-schema-generator-json

Runtime JSON Schema generation from Kotlin classes and functions.

Concrete implementations of schema generators using Kotlin reflection (JVM) and kotlinx-serialization (Multiplatform).

**Platform Support:**
- **Multiplatform**: Transformers and serialization-based generators (Common)
- **JVM only**: Reflection-based generators (require kotlin-reflect)

## Generators

### Reflection-Based (JVM Only)

- [ReflectionClassJsonSchemaGenerator] - generates schemas from any KClass via reflection
- [ReflectionFunctionCallingSchemaGenerator] - generates function calling schemas from KCallable

### Serialization-Based (Multiplatform)

- [SerializationClassJsonSchemaGenerator] - generates schemas from @Serializable classes (works on JVM, Native, JS, Wasm)

## Examples

### Reflection Generator (JVM)

```kotlin
// Class schema generation
val generator = ReflectionClassJsonSchemaGenerator.Default
val schema: JsonObject = generator.generateSchema(User::class)

// Function calling schema generation
val funcGenerator = ReflectionFunctionCallingSchemaGenerator.Default
val funcSchema = funcGenerator.generateSchema(::myFunction)
```

### Serialization Generator (Multiplatform)

```kotlin
import kotlinx.serialization.Serializable
import kotlinx.schema.generator.json.serialization.SerializationClassJsonSchemaGenerator

@Serializable
data class User(val name: String, val email: String? = null)

val generator = SerializationClassJsonSchemaGenerator.Default
val schema = generator.generateSchema(User.serializer().descriptor)
```

## Features

**Reflection Generators (JVM only):**
- Analyze third-party classes without source modification
- Extract default values from data class properties
- Recognize foreign annotations (Jackson, Koog, LangChain4j)
- OpenAI/Anthropic function calling format
- Sealed class hierarchies with `oneOf`

**Serialization Generators (Multiplatform):**
- Works on all kotlinx-serialization targets (JVM, Native, JS, Wasm)
- Consistent with kotlinx-serialization behavior

## Limitations

**Reflection Generators:**
- Function parameter defaults cannot be extracted (data class property defaults work)
- JVM only

**Serialization Generators:**
- Requires `@Serializable` annotation
- Cannot extract actual default values (only detects presence)
- Cannot extract descriptions, because of the `SerialDescriptor` limitation 

# Package kotlinx.schema.generator.json

Reflection-based JSON Schema generators and configuration.

# Package kotlinx.schema.generator.json.serialization

Schema generation from kotlinx-serialization descriptors.
