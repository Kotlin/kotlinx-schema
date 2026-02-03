# Module kotlinx-schema-ksp-gradle-plugin

Gradle plugin for seamless kotlinx-schema-ksp integration.

**Plugin ID:** `org.jetbrains.kotlinx.schema.ksp`

Automates KSP configuration and source set wiring for JSON Schema generation in Kotlin projects.

**Platform Support:** JVM and Multiplatform projects (Common, JVM, JS, Native, Wasm) • Gradle 7.6+ • Kotlin 2.2+

## Features

- Automatic KSP plugin application
- Source set configuration for generated code
- Task dependency management
- Configuration DSL for processor options

## Usage

```kotlin
plugins {
    id("org.jetbrains.kotlinx.schema.ksp") version "<version>"
}

kotlinxSchema {
    rootPackage = "com.example.models" // optional: filter generation
}
```

## Configuration Options

- `rootPackage` - limits schema generation to specified package and subpackages
- Inherits all KSP processor options from kotlinx-schema-ksp

See [Plugin Documentation](https://github.com/Kotlin/kotlinx-schema/blob/main/kotlinx-schema-ksp-gradle-plugin/README.md).

# Package kotlinx.schema.gradle.plugin

Gradle plugin implementation and extension DSL.
