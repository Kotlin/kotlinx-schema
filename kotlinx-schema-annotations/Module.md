# Module kotlinx-schema-annotations

Core annotations for marking classes and functions for schema generation.

Provides [@Schema] and [@Description] annotations recognized by both compile-time (KSP) and runtime (reflection) generators.

**Platform Support:** Multiplatform (Common, JVM, JS, Native, Wasm) • Kotlin 2.2+

## Annotations

- [@Schema] - marks declarations for schema generation
- [@Description] - adds human-readable descriptions to schemas

## Example

```kotlin
@Schema
@Description("User account information")
data class User(
    @Description("Unique user identifier") val id: Long,
    @Description("User's email address") val email: String
)
```

# Package kotlinx.schema

Core annotations for JSON Schema generation.
