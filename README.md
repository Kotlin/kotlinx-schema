[![JetBrains incubator project](https://jb.gg/badges/incubator.svg)](https://confluence.jetbrains.com/display/ALL/JetBrains+on+GitHub)
[![Kotlin CI with Gradle](https://github.com/Kotlin/kotlinx-schema/actions/workflows/gradle.yml/badge.svg?branch=main)](https://github.com/Kotlin/kotlinx-schema/actions/workflows/gradle.yml)
# kotlinx-schema

Kotlin Multiplatform toolkit for generating JSON Schemas from your Kotlin models at compile time using KSP (Kotlin Symbol Processing).

- Zero reflection at runtime
- Multiplatform support: annotations work on JVM, JS, iOS, macOS, Wasm; generated extensions work everywhere
- Supports enums, collections, maps, nested objects, nullability, and generics (with star-projection)
- Preserves @Description on classes and properties in the emitted schema
- Ships with a Gradle plugin for a one‑line setup in JVM or Multiplatform projects
- Optional runtime schema generation API available for dynamic use cases

## Quick start

Recommended: use the Gradle plugin. It applies KSP for you, wires generated sources, and sets up task dependencies.

### 1) Apply the Gradle plugin

Kotlin Multiplatform (Kotlin DSL):

```kotlin
plugins {
    kotlin("multiplatform")
    // Published plugin id:
    id("kotlinx.schema") // version "<x.y.z>" if used outside this repository
}

// Optional configuration
kotlinxSchema {
    // Limit processing to this package and its subpackages (speeds up builds)
    // Omit to process all packages
    rootPackage.set("com.example.models")
}

kotlin {
    compilerOptions {
        // Apply bare annotations to both constructor params and properties (recommended)
        freeCompilerArgs.add("-Xannotation-default-target=param-property")
    }

    // Targets…
    jvm()
    js { nodejs() }
    wasmJs { browser() }
    iosSimulatorArm64()

    sourceSets {
        commonMain {
            dependencies {
                // Annotations used in your code
                implementation("org.jetbrains.kotlinx:kotlinx-schema-annotations:<version>")
                // For JsonObject in runtime APIs
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:<version>")
            }
        }
    }
}
```

Single-target JVM setup:

```kotlin
plugins {
    kotlin("jvm")
    id("kotlinx.schema") // version "<x.y.z>" if used outside this repository
}

kotlinxSchema {
    rootPackage.set("com.example.models") // optional
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-schema-annotations:<version>")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:<version>")
}
```

Notes:
- You do NOT need to apply the KSP plugin yourself — the Gradle plugin does it.
- You do NOT need to add generated source directories — the plugin does it.
- For an example project using the plugin, see plugins/gradle/gradle-plugin-integration-tests.

Alternative: manual wiring without the Gradle plugin

If you prefer explicit KSP setup, you can configure it manually.

Multiplatform (metadata processing):

```kotlin
plugins {
    kotlin("multiplatform")
    id("com.google.devtools.ksp")
}

dependencies {
    add("kspCommonMainMetadata", "org.jetbrains.kotlinx:kotlinx-schema-ksp:<version>")
    implementation("org.jetbrains.kotlinx:kotlinx-schema-annotations:<version>")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:<version>")
}

kotlin {
    sourceSets.commonMain {
        // Make generated sources visible to metadata compilation
        kotlin.srcDir("build/generated/ksp/metadata/commonMain/kotlin")
    }
}
```

JVM only:

```kotlin
plugins {
    kotlin("jvm")
    id("com.google.devtools.ksp")
}

dependencies {
    ksp("org.jetbrains.kotlinx:kotlinx-schema-ksp:<version>")
    implementation("org.jetbrains.kotlinx:kotlinx-schema-annotations:<version>")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:<version>")
}

sourceSets.main {
    kotlin.srcDir("build/generated/ksp/main/kotlin")
}
```

### 2) Annotate your models

```kotlin
@Description("A postal address for deliveries and billing.")
@Schema
data class Address(
    @Description("Street address, including house number") val street: String,
    @Description("City or town name") val city: String,
    @Description("Postal or ZIP code") val zipCode: String,
    @Description("Two-letter ISO country code; defaults to US") val country: String = "US",
)
```

### 3) Use the generated extensions

```kotlin
val schemaString: String = Address::class.jsonSchemaString
val schemaObject: kotlinx.serialization.json.JsonObject = Address::class.jsonSchema
```

## What gets generated

Schemas follow a $id/$defs/$ref layout. Example (pretty-printed):

```json
{
  "$id": "com.example.Address",
  "$defs": {
    "com.example.Address": {
      "type": "object",
      "properties": {
        "street": { "type": "string", "description": "Street address, including house number" },
        "city":   { "type": "string", "description": "City or town name" },
        "zipCode":{ "type": "string", "description": "Postal or ZIP code" },
        "country":{ "type": "string", "description": "Two-letter ISO country code; defaults to US" }
      },
      "required": ["street", "city", "zipCode"],
      "additionalProperties": false,
      "description": "A postal address for deliveries and billing."
    }
  },
  "$ref": "#/$defs/com.example.Address"
}
```

- Enums are `type: string` with `enum: [...]` and carry `@Description` as `description`.
- Object properties include their inferred type schema and, when present, property-level `@Description` as `description`.
- Nullable properties are emitted as a union including `null`.
- Collections: `List<T>`/`Set<T>` → `{ "type":"array", "items": T }`; `Map<String, V>` → `{ "type":"object", "additionalProperties": V }`.
- Unknown/generic type parameters resolve to `kotlin.Any` with a minimal definition in `$defs`.

## Using @Schema and @Description annotations

### @Schema annotation

Mark classes with `@Schema` to generate extension properties for them:

```kotlin
@Schema  // Uses default schema type "json"
data class Address(val street: String, val city: String)

@Schema("json")  // Explicitly specify schema type
data class Person(val name: String, val age: Int)
```

The `@Schema` annotation has an optional `value` parameter (defaults to `"json"`) that specifies the schema type. Currently, only JSON Schema generation is supported, but this parameter allows for future extensibility to other schema formats (e.g., OpenAPI, Avro).

**Note**: Both `jsonSchemaString: String` and `jsonSchema: JsonObject` extension properties are always generated for all `@Schema`-annotated classes, regardless of annotation parameters.

### @Description annotation

Use `@Description` on classes and properties to add human-readable documentation to your schemas:

```kotlin
@Description("A purchasable product with pricing info")
@Schema
data class Product(
    @Description("Unique identifier for the product") val id: Long,
    @Description("Human-readable product name") val name: String,
    @Description("Optional detailed description of the product") val description: String?,
    @Description("Unit price expressed as a decimal number") val price: Double,
)
```

**Tip**: With the recommended compiler flag `-Xannotation-default-target=param-property`, a bare `@Description` on a primary constructor parameter also applies to the property. If you do not enable the flag, use `@param:Description` for constructor-declared properties.

## Runtime schema generation

For scenarios where you need to generate schemas dynamically at runtime (without KSP compile-time generation), you can use `SimpleJsonSchemaGenerator`:

```kotlin
import kotlinx.schema.generator.json.SimpleJsonSchemaGenerator
import kotlinx.serialization.Serializable

@Serializable
data class User(val name: String, val email: String)

// Generate schema at runtime
val schema: JsonObject = SimpleJsonSchemaGenerator.generateSchema(User::class)
val schemaString: String = SimpleJsonSchemaGenerator.generateSchemaString(User::class)
```

**Requirements for runtime generation**:
- Add dependency: `org.jetbrains.kotlinx:kotlinx-schema-generator-json:<version>`
- Classes must be annotated with `@Serializable` from kotlinx-serialization
- Uses Kotlin reflection at runtime (JVM only)

**When to use**:
- **Compile-time (KSP)**: Recommended for most cases - zero runtime overhead, multiplatform support
- **Runtime API**: Use when schemas need to be generated dynamically, or for prototyping

## Project architecture

Top-level modules you might interact with:

- **kotlinx-schema-annotations** — runtime annotations: @Schema and @Description
- **kotlinx-schema-generator-core** — internal representation (IR) for schema descriptions
- **kotlinx-schema-generator-json** — JSON Schema emitter from the IR
- **kotlinx-schema-ksp** — KSP processor that scans your code and generates the extension properties:
  - `KClass<T>.jsonSchema: JsonObject`
  - `KClass<T>.jsonSchemaString: String`
- **plugins/gradle/kotlinx-schema-gradle-plugin** — Gradle plugin (id: "kotlinx.schema") that:
  - Applies KSP automatically
  - Adds the KSP processor dependency
  - Wires generated sources into your source sets
  - Sets up multiplatform task dependencies
- **plugins/gradle/gradle-plugin-integration-tests** — a real MPP sample used as integration tests, showing how users consume the Gradle plugin
- **ksp-integration-tests** — KSP end‑to‑end tests for generation without the Gradle plugin

## Building and testing

- Build all: `./gradlew build`
- Run all tests: `./gradlew test`
- KSP integration tests (no Gradle plugin): `./gradlew :ksp-integration-tests:test`
- Gradle plugin integration sample: `./gradlew :plugins:gradle:gradle-plugin-integration-tests:check`

Generated code for tests can be found under:
- `ksp-integration-tests/build/generated/ksp/metadata/commonMain/kotlin`

## Requirements

- Kotlin 2.2+
- KSP 2 (applied automatically when using the Gradle plugin)
- _kotlinx-serialization-json_ for JsonObject support

Tip: If you use @Description on primary constructor parameters, enable
`-Xannotation-default-target=param-property` in Kotlin compiler options so the description applies to the backing property.

## Contributing

Contributions are welcome! Please ensure:
- All tests pass: `./gradlew test`
- Code generation works: `./gradlew :ksp-integration-tests:build`
- Generated code is clean and well-documented
- Integration tests cover new functionality

## License

This project is licensed under the Apache License 2.0 - see the [LICENSE](LICENSE) file for details.