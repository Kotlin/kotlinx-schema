# Kotlinx Schema Gradle Plugin

<!--- TOC -->

* [Overview](#overview)
* [Features](#features)
  * [Multiplatform Project Behavior](#multiplatform-project-behavior)
  * [JVM Project Behavior](#jvm-project-behavior)
* [Usage](#usage)
  * [Kotlin JVM project setup](#kotlin-jvm-project-setup)
  * [Kotlin Multiplatform project setup](#kotlin-multiplatform-project-setup)
  * [Annotate Your Data Classes](#annotate-your-data-classes)
  * [Use Generated Extensions](#use-generated-extensions)
* [Configuration](#configuration)
* [Troubleshooting multiplatform metadata compilations](#troubleshooting-multiplatform-metadata-compilations)

<!--- END -->

A Gradle plugin for generating JSON schemas using KSP (Kotlin Symbol Processing).

## Overview

This plugin simplifies the integration of kotlinx-schema-ksp into your Kotlin projects. 
It automatically configures KSP dependencies, sets up source directories for generated code, 
and handles task dependencies for both JVM and multiplatform projects.

## Features

- ✅ Automatic configuration of KSP dependencies
- ✅ Support for both Kotlin JVM and Kotlin Multiplatform projects (common code only)
- ✅ Configurable schema generation options
- ✅ Automatic source set configuration for generated code
- ✅ Proper task dependency management

### Multiplatform Project Behavior

Current limitation: for multiplatform projects, the plugin generates schemas only once in commonMain:

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

## Usage

### Kotlin JVM project setup

gradle.properties:
```properties
kotlinxSchemaVersion=0.0.4
```

build.gradle.kts:
```kotlin
plugins {
    kotlin("jvm") version "2.2.21"
    kotlin("plugin.serialization") version "2.2.21"
    id("org.jetbrains.kotlinx.schema.ksp")
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-schema-annotations:${project.properties["kotlinxSchemaVersion"]}")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.1")
}

kotlinxSchema {
    enabled.set(true)              // Optional, defaults to true
    rootPackage.set("com.example") // Optional, only process this package and subpackages
    withSchemaObject.set(true)     // Optional. Requires kotlinx-serialization-json
}
```

settings.gradle.kts:
```kotlin
pluginManagement {
    resolutionStrategy {
        eachPlugin {
            if (requested.id.id == "org.jetbrains.kotlinx.schema.ksp") {
                useModule("org.jetbrains.kotlinx:kotlinx-schema-gradle-plugin:${providers.gradleProperty("kotlinxSchemaVersion").get()}")
            }
        }
    }
}
```

### Kotlin Multiplatform project setup

gradle.properties:
```properties
kotlinxSchemaVersion=0.0.4
```

```kotlin
plugins {
    kotlin("multiplatform") version "2.2.21"
    kotlin("plugin.serialization") version "2.2.21" 
    id("org.jetbrains.kotlinx.schema.ksp")
}

kotlin {
    jvm()
    js { nodejs() }

    sourceSets {
        commonMain {
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-schema-annotations:${project.properties["kotlinxSchemaVersion"]}")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.1")
            }
        }
    }
}

kotlinxSchema {
    enabled.set(true)              // Optional, defaults to true
    rootPackage.set("com.example") // Optional, only process this package and subpackages
    withSchemaObject.set(true)     // Optional. Requires kotlinx-serialization-json
}
```

### Annotate Your Data Classes
<!--- CLEAR -->
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
<!--- KNIT example-knit-ksp-readme-01.kt -->

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
    withSchemaObject.set(true)
}
```

## Troubleshooting multiplatform metadata compilations

If you encounter "Unresolved reference 'serialization'" errors during `compileCommonMainKotlinMetadata`, verify:

1. Repository order in `settings.gradle.kts` - `mavenCentral()` must be listed before `mavenLocal()`
2. The `kotlin("plugin.serialization")` plugin is applied
3. `kotlinx-serialization-json` is declared in `commonMain` dependencies

The repository order is critical because Gradle must resolve the correct Kotlin multiplatform metadata variants for `kotlinx-serialization-json`. When `mavenLocal()` is listed first, Gradle may incorrectly resolve JVM-specific artifacts for metadata compilation.
