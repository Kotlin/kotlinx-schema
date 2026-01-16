# KSP Example: Geometric Shapes

Shows how **kotlinx-schema-ksp** automatically generates JSON schemas from Kotlin classes.

## What This Demonstrates

- **Sealed classes** → `oneOf` schemas (polymorphism)
- **Default values** → optional fields
- **Class KDoc** → schema descriptions
- **@Description** → property descriptions (recommended)

Use cases: LLM function calling, API docs, form generation, validation.

## Quick Start

```bash
./gradlew clean build  # KSP generates schemas
./gradlew test         # See schemas in action
```

## How It Works

### 1. Annotate Your Classes

```kotlin
import kotlinx.schema.Schema
import kotlinx.schema.Description

/**
 * A circle defined by its radius.
 */
@Schema
data class Circle(
    val name: String,

    @Description("Radius in units (must be positive)")
    val radius: Double,

    val color: String = "#FF5733"  // Defaults = optional
)
```

**KDoc vs @Description:**
- **Class-level KDoc** is automatically extracted
- **@Description** is the recommended way to document properties
- @Description takes precedence when both exist

### 2. KSP Generates Extensions

```kotlin
val schemaString = Circle::class.jsonSchemaString  // String
val schemaObject = Circle::class.jsonSchema        // JsonObject
```

### 3. Use With LLMs

```kotlin
// OpenAI
FunctionTool(
    name = "create_circle",
    parameters = Parameters.fromJsonString(Circle::class.jsonSchemaString)
)

// Anthropic
tool {
    name = "create_circle"
    inputSchema = Circle::class.jsonSchema
}
```

## Generated Schema Example

Input:
```kotlin
/**
 * A circle defined by its radius.
 */
@Schema
data class Circle(
    val name: String,

    @Description("Radius in units (must be positive)")
    val radius: Double,

    val color: String = "#FF5733"
)
```

Output:
```json
{
  "$id": "com.example.shapes.Circle",
  "type": "object",
  "properties": {
    "name": { "type": "string" },
    "radius": {
      "type": "number",
      "description": "Radius in units (must be positive)"
    },
    "color": { "type": "string" }
  },
  "required": ["name", "radius"],
  "description": "A circle defined by its radius."
}
```

Note: `color` is optional (has default), class KDoc becomes schema description, @Description adds property description.

## Setting Up KSP

### 1. Add Plugins

```kotlin
// build.gradle.kts
plugins {
    kotlin("multiplatform") version "2.2.21"
    kotlin("plugin.serialization") version "2.2.21"
    id("com.google.devtools.ksp") version "2.3.4"
}
```

### 2. Configure Source Sets & Dependencies

```kotlin
kotlin {
    jvm()
    js(IR) { nodejs() }

    sourceSets {
        commonMain {
            kotlin.srcDir("build/generated/ksp/metadata/commonMain/kotlin")
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-schema-annotations:0.0.2")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
            }
        }
    }
}

dependencies {
    add("kspCommonMainMetadata", "org.jetbrains.kotlinx:kotlinx-schema-ksp:0.0.2")
}
```

### 3. Configure Tasks & KSP

```kotlin
tasks.named("compileKotlinJvm") { dependsOn("kspCommonMainKotlinMetadata") }
tasks.named("compileKotlinJs") { dependsOn("kspCommonMainKotlinMetadata") }

ksp {
    arg("kotlinx.schema.withSchemaObject", "true")  // Generate JsonObject accessor
    arg("kotlinx.schema.rootPackage", "com.example.shapes")  // Schema $id prefix
}
```

## Learn More

- [kotlinx-schema Repository](../../README.md)
- [KSP Documentation](https://kotlinlang.org/docs/ksp-overview.html)

## License

Apache 2.0
