# Module kotlinx-schema-ksp

Compile-time JSON Schema generation via Kotlin Symbol Processing (KSP).

KSP symbol processor that analyzes [@Schema]-annotated declarations at compile time, generating extension properties with zero runtime overhead.

**Platform Support:** Multiplatform (generates platform-agnostic code) • Build-time only • Kotlin 2.2+ • KSP 2.0+

## Generated Extensions

### For Classes

```kotlin
@Schema
data class User(val name: String)

// Generated:
val KClass<User>.jsonSchemaString: String
val KClass<User>.jsonSchema: JsonObject // if withSchemaObject = true
```

### For Functions

```kotlin
@Schema
fun greet(name: String): String

// Generated (top-level):
fun greetJsonSchemaString(): String
fun greetJsonSchema(): FunctionCallingSchema // if withSchemaObject = true
```

## Configuration

KSP processor options:
- `kotlinx.schema.rootPackage` - filter generation to specific package
- `kotlinx.schema.generator.strictMode` - enable strict JSON Schema validation

See [KSP Configuration Guide](https://github.com/Kotlin/kotlinx-schema/blob/main/docs/ksp.md).

## Features

- Zero runtime overhead (compile-time generation)
- Multiplatform (Common, JVM, JS, Native, Wasm)
- KDoc comments as descriptions
- Sealed class hierarchies with `oneOf`
- Function calling schema generation

## Limitations

- Requires [@Schema] annotation
- Default values tracked but not extracted ([KSP limitation](https://github.com/google/ksp/issues/1868))
- Only processes source code you own

# Package kotlinx.schema.ksp

KSP symbol processor implementation.

# Package kotlinx.schema.ksp.codegen

Code generation for schema extensions.

# Package kotlinx.schema.ksp.introspection

KSP-based type introspection.
