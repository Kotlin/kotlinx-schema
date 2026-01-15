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

1. **Single KSP Execution**: KSP runs `kspCommonMain` task (generates to `build/generated/kotlinxSchema/commonMain/kotlin`)
2. **Common Source Only**: All generated schemas are placed in the commonMain source set, making them available to all targets
3. **Source Set Registration**: Generated sources are automatically registered with the `commonMain` source set using the typed Kotlin Multiplatform Extension API
4. **No Per-Target Generation**: KSP does not run separately for each platform target (jvm, js, etc.)
5. **Works with Any Target**: The plugin works with any Kotlin Multiplatform target combination (JVM, JS, Native, etc.)

### JVM Project Behavior

For JVM-only projects:

1. **JVM KSP Execution**: KSP runs `kspKotlin` task for the main source set (generates to `build/generated/kotlinxSchema/main/kotlin`)
2. **Source Set Registration**: Generated sources are automatically registered with the `main` source set using the typed Kotlin JVM Extension API
3. **Single Source Directory**: All generated schemas are placed in the JVM main source set


## Requirements

- Gradle 7.0+
- Kotlin 1.9+

## Limitations

### Common-Only Multiplatform Projects Not Supported

The plugin currently requires **at least one platform target** (JVM, JS, Native, etc.) to be configured in multiplatform projects. Common-only projects with no platform targets are not supported.

**Supported configurations:**

```kotlin
// ✅ Works: JVM target
kotlin {
    jvm()
    sourceSets {
        commonMain { /* ... */ }
    }
}

// ✅ Works: JS target
kotlin {
    js { nodejs() }
    sourceSets {
        commonMain { /* ... */ }
    }
}

// ✅ Works: Multiple targets
kotlin {
    jvm()
    js { nodejs() }
    iosArm64()
    sourceSets {
        commonMain { /* ... */ }
    }
}
```

**Unsupported configuration:**

```kotlin
// ❌ Does not work: Common-only (no targets)
kotlin {
    // No targets configured
    sourceSets {
        commonMain { /* ... */ }
    }
}
```

If you have a common-only project, you must add at least one platform target for the plugin to work properly.

## Usage

### Basic Setup (Kotlin JVM)

```kotlin
plugins {
    kotlin("jvm") version "2.2.21"
    id("org.jetbrains.kotlinx.schema.ksp") version "0.1.0"  // KSP is auto-applied
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
    id("org.jetbrains.kotlinx.schema.ksp") version "0.1.0"  // KSP is auto-applied
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

    // Optional: Generate JsonObject properties in addition to JSON strings
    // Requires kotlinx-serialization-json dependency
    // Fully supported in multiplatform commonMain
    withSchemaObject.set(true)
}
```

### Using `withSchemaObject` in Multiplatform Projects

The `withSchemaObject` option generates `jsonSchema` properties that return `kotlinx.serialization.json.JsonObject` instances. This feature **fully supports multiplatform projects** including `commonMain` source sets.

**Requirements:**
1. Add `kotlinx-serialization-json` dependency to `commonMain`
2. Apply `kotlin("plugin.serialization")` plugin
3. **Important**: Ensure `mavenCentral()` appears before `mavenLocal()` in repository order

**Example configuration:**

```kotlin
// settings.gradle.kts
dependencyResolutionManagement {
    repositories {
        mavenCentral()  // Must be first for proper multiplatform metadata resolution
        mavenLocal()
    }
}

// build.gradle.kts
plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")  // Required for JsonObject support
    id("org.jetbrains.kotlinx.schema.ksp")
}

kotlin {
    jvm()
    js { nodejs() }

    sourceSets {
        commonMain {
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")
            }
        }
    }
}

kotlinxSchema {
    withSchemaObject.set(true)
}
```

**Generated code** (available in commonMain for all targets):

```kotlin
// JSON schema as string
val schemaString: String = Person::class.jsonSchemaString

// JSON schema as JsonObject (when withSchemaObject = true)
val schemaObject: JsonObject = Person::class.jsonSchema
```

**Troubleshooting multiplatform metadata compilation:**

If you encounter "Unresolved reference 'serialization'" errors during `compileCommonMainKotlinMetadata`, verify:

1. Repository order in `settings.gradle.kts` - `mavenCentral()` must be listed before `mavenLocal()`
2. The `kotlin("plugin.serialization")` plugin is applied
3. `kotlinx-serialization-json` is declared in `commonMain` dependencies

The repository order is critical because Gradle must resolve the correct Kotlin multiplatform metadata variants for `kotlinx-serialization-json`. When `mavenLocal()` is listed first, Gradle may incorrectly resolve JVM-specific artifacts for metadata compilation.
```

## How It Works

1. **Automatic KSP Setup**: The plugin automatically applies and configures the KSP plugin

2. **Dependency Management**: Automatically adds the kotlinx-schema-ksp processor:
   - JVM projects: Adds KSP dependency for the main source set
   - Multiplatform projects: Adds KSP dependency only for commonMain metadata

3. **Source Set Configuration**: Generated Kotlin files are automatically added to your source sets:
   - JVM projects: `build/generated/kotlinxSchema/main/kotlin`
   - Multiplatform projects: `build/generated/kotlinxSchema/commonMain/kotlin` (shared across all targets)

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

The plugin automatically registers generated sources using Gradle's typed API. If IntelliJ IDEA doesn't recognize generated code:

1. **Sync Gradle**: File > Sync Project with Gradle Files
2. **Rebuild**: Build > Rebuild Project
3. **Check Generated Directory**: Verify files exist in:
   - JVM: `build/generated/kotlinxSchema/main/kotlin`
   - Multiplatform: `build/generated/kotlinxSchema/commonMain/kotlin`
4. **Invalidate Caches**: File > Invalidate Caches / Restart (last resort)

## Examples

See the integration test projects in the repository for complete working examples.

## License

This plugin is part of the kotlinx-schema project. See the root project for license information.
