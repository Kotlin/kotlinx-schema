# Kotlinx Schema Gradle Plugin

A Gradle plugin for generating JSON schemas using KSP (Kotlin Symbol Processing).

## Overview

This plugin simplifies the integration of kotlinx-schema-ksp into your Kotlin projects. It automatically configures KSP dependencies, sets up source directories for generated code, and handles task dependencies for both JVM and multiplatform projects.

## Features

- ✅ Automatic configuration of KSP dependencies
- ✅ Support for both Kotlin JVM and Kotlin Multiplatform projects (common code only)
- ✅ Configurable schema generation options
- ✅ Automatic source set configuration for generated code
- ✅ Proper task dependency management

### Multiplatform Project Behavior

For multiplatform projects, the plugin generates schemas only once in commonMain:

1. **Single KSP Execution**: KSP runs only for `kspCommonMainMetadata` (generates to `build/generated/ksp/metadata/commonMain/kotlin`)
2. **Common Source Only**: All generated schemas are placed in the commonMain source set, making them available to all targets
3. **No Per-Target Generation**: KSP does not run separately for each platform target (jvm, js, etc.)
4. **Platform-Specific Classes**: Classes that depend on platform-specific APIs (e.g., JacksonModel for JVM) should be placed in platform-specific source sets (e.g., jvmMain) to generate their schemas correctly

### JVM Project Behavior

For JVM-only projects:

1. **JVM KSP Execution**: KSP runs for the main source set (generates to `build/generated/ksp/main/kotlin`)
2. **Single Source Directory**: All generated schemas are placed in the JVM main source set


## Requirements

- Gradle 7.0+
- Kotlin 1.9+

## Usage

### Basic Setup (Kotlin JVM)

```kotlin
plugins {
    kotlin("jvm") version "2.2.21"
    id("kotlinx.schema") version "0.1.0"  // KSP is auto-applied
}

kotlinxSchema {
    enabled.set(true)              // Optional, defaults to true
    rootPackage.set("com.example") // Optional, only process this package and subpackages
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.1")
}
```

### Multiplatform Setup

```kotlin
plugins {
    kotlin("multiplatform") version "2.2.21"
    id("kotlinx.schema") version "0.1.0"  // KSP is auto-applied
}

kotlin {
    jvm()
    js { nodejs() }

    sourceSets {
        commonMain {
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.1")
            }
        }
    }
}

kotlinxSchema {
    enabled.set(true)              // Optional, defaults to true
    rootPackage.set("com.example") // Optional, only process this package and subpackages
}
```

### Annotate Your Data Classes

```kotlin
import kotlinx.schema.Schema
import kotlinx.schema.Description

@Description("A person with basic information")
@Schema
data class Person(
    @Description("Given name of the person")
    val firstName: String,
    @Description("Family name of the person")
    val lastName: String,
    @Description("Age of the person in years")
    val age: Int
)
```

### Use Generated Extensions

The plugin generates extension properties for each annotated class:

```kotlin
// Access JSON schema as string
val schemaString: String = Person::class.jsonSchemaString

// Access JSON schema as JsonObject
val schemaObject: JsonObject = Person::class.jsonSchema
```

## Configuration

The plugin provides a `kotlinxSchema` extension for configuration:

```kotlin
kotlinxSchema {
    // Whether schema generation is enabled
    // Defaults to true
    enabled.set(true)

    // Optional: Only process classes in this package and its subpackages
    // If not set, all packages are processed
    rootPackage.set("com.example")
}
```

## How It Works

1. **Automatic KSP Setup**: The plugin automatically applies and configures the KSP plugin

2. **Dependency Management**: Automatically adds the kotlinx-schema-ksp processor:
   - JVM projects: Adds KSP dependency for the main source set
   - Multiplatform projects: Adds KSP dependency only for commonMain metadata

3. **Source Set Configuration**: Generated Kotlin files are automatically added to your source sets:
   - JVM projects: `build/generated/ksp/main/kotlin`
   - Multiplatform projects: `build/generated/ksp/metadata/commonMain/kotlin` (shared across all targets)

4. **Task Dependencies**: Ensures KSP tasks run before compilation tasks, maintaining proper build order

## Generated Code

For each class annotated with `@Schema`, the plugin generates:

1. **jsonSchemaString**: Extension property returning the JSON schema as a String
2. **jsonSchema**: Extension property returning the JSON schema as a JsonObject

Example generated code:

```kotlin
public val KClass<Person>.jsonSchemaString: String
    get() = """
    {
      "$id": "com.example.Person",
      "$defs": {
        "com.example.Person": {
          "type": "object",
          "properties": {
            "firstName": { "type": "string" },
            "lastName": { "type": "string" },
            "age": { "type": "integer" }
          },
          "required": ["firstName", "lastName", "age"]
        }
      },
      "$ref": "#/$defs/com.example.Person"
    }
    """.trimIndent()

public val KClass<Person>.jsonSchema: JsonObject
    get() = Json.decodeFromString<JsonObject>(jsonSchemaString)
```

## Troubleshooting

### Generated Code Not Found

Run `./gradlew clean build` to ensure KSP generates the code properly. Check that your build directory contains the generated files.

### IDE Not Recognizing Generated Code

In IntelliJ IDEA, you may need to:
1. Sync Gradle project (File > Sync Project with Gradle Files)
2. Rebuild project (Build > Rebuild Project)
3. Invalidate caches (File > Invalidate Caches / Restart)

## Examples

See the integration test projects in the repository for complete working examples.

## License

This plugin is part of the kotlinx-schema project. See the root project for license information.
