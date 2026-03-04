# KSP Processor Configuration

**Table of contents**
<!--- TOC -->

* [Setup](#setup)
  * [Google KSP gradle plugin](#google-ksp-gradle-plugin)
    * [Multiplatform projects](#multiplatform-projects)
    * [JVM-only projects](#jvm-only-projects)
  * [Kotlinx-Schema gradle plugin](#kotlinx-schema-gradle-plugin)
    * [Multiplatform projects](#multiplatform-projects)
    * [JVM-only projects](#jvm-only-projects)
  * [Maven Plugin](#maven-plugin)
* [Configuration options](#configuration-options)
  * [Options reference](#options-reference)
  * [Filtering by class/function name](#filtering-by-classfunction-name)
    * [Kotlinx-Schema Gradle plugin](#kotlinx-schema-gradle-plugin)
    * [Google KSP plugin](#google-ksp-plugin)
    * [Maven plugin](#maven-plugin)
  * [Option priority](#option-priority)
* [Generated Code](#generated-code)
* [See Also](#see-also)

<!--- END -->

Generate JSON schemas at compile time with zero runtime overhead using the `kotlinx-schema` KSP processor.

## Setup

Configure the KSP processor directly in your Gradle build script, Maven pom.xml, or use the dedicated Gradle plugin.

### Google KSP gradle plugin

Add the [Google KSP plugin](https://kotlinlang.org/docs/ksp-quickstart.html) and processor dependency to your project.

#### Multiplatform projects

```kotlin
plugins {
    kotlin("multiplatform")
    id("com.google.devtools.ksp") version "<ksp-version>"
}

dependencies {
    add("kspCommonMainMetadata", "org.jetbrains.kotlinx:kotlinx-schema-ksp:<version>")
    implementation("org.jetbrains.kotlinx:kotlinx-schema-annotations:<version>")
}

kotlin {
    sourceSets.commonMain.kotlin.srcDir("build/generated/ksp/metadata/commonMain/kotlin")
}

// Ensure KSP runs before compilation
tasks.withType<org.jetbrains.kotlin.gradle.dsl.KotlinCompile<*>>().all {
    if (name != "kspCommonMainKotlinMetadata") dependsOn("kspCommonMainKotlinMetadata")
}

ksp {
    arg("kotlinx.schema.rootPackage", "com.example")
}
```

Check out an [example project](https://github.com/Kotlin/kotlinx-schema/tree/main/examples/gradle-google-ksp).

#### JVM-only projects

```kotlin
plugins {
    kotlin("jvm")
    id("com.google.devtools.ksp") version "<ksp-version>"
}

dependencies {
    ksp("org.jetbrains.kotlinx:kotlinx-schema-ksp:<version>")
    implementation("org.jetbrains.kotlinx:kotlinx-schema-annotations:<version>")
}

sourceSets.main.kotlin.srcDir("build/generated/ksp/main/kotlin")
```

### Kotlinx-Schema gradle plugin

The plugin automatically handles KSP configuration, source set registration, and task dependencies,
and provides additional configuration options (DSL) for schema generation.

1. Register the plugin in `settings.gradle.kts`:

    ```kotlin
    pluginManagement {
        repositories {
            google()
            mavenCentral()
        }
        
        resolutionStrategy {
            eachPlugin {
                if (requested.id.id == "org.jetbrains.kotlinx.schema.ksp") {
                    useModule("org.jetbrains.kotlinx:kotlinx-schema-gradle-plugin:<version>")
                }
            }
        }
    }
    ```

2. Apply the plugin and dependencies in your `build.gradle.kts`:

#### Multiplatform projects

```kotlin
plugins {
    kotlin("multiplatform")
    id("org.jetbrains.kotlinx.schema.ksp")
}

kotlin {
    sourceSets.commonMain.dependencies {
        implementation("org.jetbrains.kotlinx:kotlinx-schema-annotations:<version>")
        implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:<version>") // Required for withSchemaObject
    }
}

kotlinxSchema {
    rootPackage.set("com.example")
}
```

#### JVM-only projects

```kotlin
plugins {
    kotlin("jvm")
    id("org.jetbrains.kotlinx.schema.ksp")
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-schema-annotations:<version>")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:<version>")
}

kotlinxSchema {
    rootPackage.set("com.example")
}
```

**Notes:**
- You do NOT need to apply the KSP plugin yourself — the Gradle plugin does it.
- You do NOT need to add generated source directories — the plugin does it.
- For an example project, see [gradle-plugin-integration-tests](https://github.com/Kotlin/kotlinx-schema/tree/main/gradle-plugin-integration-tests).

### Maven Plugin

You may also run schema generation with KSP in your Maven projects.

Add the [`ksp-maven-plugin`](https://github.com/kpavlov/ksp-maven-plugin) with the processor dependency
and include the annotations library in your project.

```xml

<plugin>
    <groupId>me.kpavlov.ksp.maven</groupId>
    <artifactId>ksp-maven-plugin</artifactId>
    <version>0.3.0</version>
    <extensions>true</extensions>
    <dependencies>
        <dependency>
            <groupId>org.jetbrains.kotlinx</groupId>
            <artifactId>kotlinx-schema-ksp</artifactId>
            <version>${kotlinx-schema.version}</version>
        </dependency>
    </dependencies>
    <configuration>
        <options>
            <kotlinx.schema.rootPackage>com.example</kotlinx.schema.rootPackage>
        </options>
    </configuration>
</plugin>

<!-- In <dependencies> -->
<dependencies>
    <dependency>
        <groupId>org.jetbrains.kotlinx</groupId>
        <artifactId>kotlinx-schema-annotations-jvm</artifactId>
        <version>${kotlinx-schema.version}</version>
    </dependency>
</dependencies>

<properties>
<!-- check latest version: https://central.sonatype.com/artifact/org.jetbrains.kotlinx/kotlinx-schema-ksp -->
<kotlinx-schema.version>0.0.5</kotlinx-schema.version>
</properties>
```

Check out an [example project](https://github.com/Kotlin/kotlinx-schema/tree/main/examples/maven-ksp).

## Configuration options

Options can be set globally in your build configuration or overridden per-class via `@Schema`.

### Options reference

| Option             | Type      | Default | Description                                                                        |
|:-------------------|:----------|:--------|:-----------------------------------------------------------------------------------|
| `enabled`          | `Boolean` | `true`  | Enable or disable schema generation.                                               |
| `rootPackage`      | `String`  | `null`  | Limit processing to this package and its subpackages. Improves build performance.  |
| `include`          | `String`  | `null`  | Comma- or semicolon-separated glob patterns. Only matching symbols are processed.  |
| `exclude`          | `String`  | `null`  | Comma- or semicolon-separated glob patterns. Matching symbols are always skipped.  |
| `withSchemaObject` | `Boolean` | `false` | Generate `jsonSchema: JsonObject` property. Requires `kotlinx-serialization-json`. |
| `visibility`       | `String`  | `""`    | Visibility modifier for generated extensions (`public`, `internal`, etc.).         |

### Filtering by class/function name

Use `kotlinx.schema.include` and `kotlinx.schema.exclude` to control exactly which annotated classes 
and functions get schemas generated — without touching the annotations themselves. 
Both options accept a comma- or semicolon-separated list of glob patterns matched against the fully qualified name.

**Glob syntax:**

| Pattern | Matches                                                             |
|:--------|:--------------------------------------------------------------------|
| `*`     | Any sequence of characters within a single package segment (no `.`) |
| `**`    | Any sequence of characters across package segments (including `.`)  |
| `?`     | Any single character (no `.`)                                       |

**Rules:**
- If `include` is set, only symbols matching at least one pattern are processed.
- `exclude` is applied after `include` — a symbol matching any exclude pattern is always skipped.
- Both options apply to classes and functions, and work alongside `rootPackage`; the root package filter runs first.

#### Kotlinx-Schema Gradle plugin

```kotlin
kotlinxSchema {
    // Generate schemas only for symbols in the api and dto subpackages
    include.set("com.example.api.**, com.example.dto.**")

    // Skip internal implementation symbols even if they match the include pattern
    exclude.set("**.internal.**, **.*Internal")
}
```

#### Google KSP plugin

```kotlin
ksp {
    arg("kotlinx.schema.include", "com.example.api.**, com.example.dto.**")
    arg("kotlinx.schema.exclude", "**.internal.**, **.*Internal")
}
```

#### Maven plugin

```xml
<configuration>
    <options>
        <kotlinx.schema.include>com.example.api.**, com.example.dto.**</kotlinx.schema.include>
        <kotlinx.schema.exclude>**.internal.**, **.*Internal</kotlinx.schema.exclude>
    </options>
</configuration>
```

> [!TIP]
> For large projects, combine `rootPackage` with `include` for maximum build performance:
> `rootPackage` narrows the KSP symbol scan, then `include` filters the remaining candidates.

### Option priority

1. **Annotation Parameter** (highest) — `@Schema(withSchemaObject = true)`
2. **KSP Argument** — Global processor options (e.g., `arg()` in Gradle or `<options>` in Maven)
3. **Gradle Option** (Plugin only) — `kotlinxSchema { withSchemaObject.set(true) }`
4. **Default Value** (lowest)

> [!TIP]
> Use an empty string `visibility.set("")` (default) for Multiplatform projects targeting Native
> to avoid "redundant visibility modifier" warnings.

## Generated Code

For each `@Schema`-annotated class, the processor generates extension properties:

<!--- CLEAR -->
<!--- MODULE docs -->
<!--- INCLUDE
import kotlinx.schema.Schema
-->
```kotlin
@Schema(withSchemaObject = true)
data class User(val name: String)
```
<!--- KNIT example-knit-ksp-01.kt --> 

Access generated extensions
```kotlin
val jsonString: String = User::class.jsonSchemaString
val jsonObject: JsonObject = User::class.jsonSchema
```

For each `@Schema`-annotated function, the processor generates additional top-level or extension function:


<!--- CLEAR -->
<!--- MODULE docs -->
<!--- INCLUDE
import kotlinx.schema.Schema
data class Shape(val name: String)
-->
```kotlin
@Schema(withSchemaObject = true)
internal fun calculateArea(shape: Shape): Double = TODO("only signature matters")
```
<!--- KNIT example-knit-ksp-02.kt --> 

Access generated functions:
```kotlin
val functionCallSchemaString: String = calculateAreaJsonSchemaString() // <function name> + "jsonSchemaString()" 
val functionCallSchema: JsonObject = calculateAreaJsonSchema() // <function name> + "jsonSchema()" 
```

## See Also

- [Serialization-Based Generator](serializable.md) — Runtime generation for any `@Serializable` class (no KSP required)
- [Annotation Reference](../README.md#using-schema-and-description-annotations) — `@Schema` and `@Description` usage
- [Runtime Schema Generation](../README.md#runtime-schema-generation) — Alternative using Reflection (JVM only)
- [Function Calling Schemas](../README.md#function-calling-schema-generation-for-llms) — Generate LLM function schemas
- [JSON Schema DSL](../kotlinx-schema-json/README.md) — Manual schema construction
