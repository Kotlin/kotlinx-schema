# Module kotlinx-schema-gradle-plugin

Gradle plugin that simplifies the integration of kotlinx-schema-ksp into Kotlin projects.

## Features

- Automatic KSP plugin application and configuration
- Support for Kotlin JVM and Multiplatform projects
- Configurable schema generation with `rootPackage` filtering
- Automatic source set and task dependency configuration

## Plugin ID

`org.jetbrains.kotlinx.schema.ksp`

## Usage

```kotlin
plugins {
    kotlin("jvm") version "2.2.21"
    id("org.jetbrains.kotlinx.schema.ksp")
}

kotlinxSchema {
    enabled.set(true)
    rootPackage.set("com.example") // Optional
    withSchemaObject.set(true)     // Optional. Requires kotlinx-serialization-json
}
```

or with Kotlin Multiplatform:
```kotlin
plugins {
    kotlin("multiplatform") version "2.2.21"
    id("org.jetbrains.kotlinx.schema.ksp") version "0.1.0"
}

kotlinxSchema {
    enabled.set(true)
    rootPackage.set("com.example") // Optional
    withSchemaObject.set(true)     // Optional. Requires kotlinx-serialization-json
}
```
