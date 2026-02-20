# Serialization-Based Schema Generation

**Table of contents**
<!--- TOC -->

* [Overview](#overview)
* [Setup](#setup)
* [Basic Usage](#basic-usage)
* [Configuration](#configuration)
  * [Introspector configuration](#introspector-configuration)
  * [Custom description extraction](#custom-description-extraction)
  * [JSON Schema output configuration](#json-schema-output-configuration)
  * [JsonSchemaConfig presets](#jsonschemaconfig-presets)
  * [JsonSchemaConfig reference](#jsonschemaconfig-reference)
* [Polymorphic types](#polymorphic-types)
* [See Also](#see-also)

<!--- END -->
<!--- TEST_NAME ExampleKnitSerializableTest -->

Generate JSON Schema at runtime from any `@Serializable` class using its `SerialDescriptor`.

## Overview

`SerializationClassJsonSchemaGenerator` converts a kotlinx.serialization `SerialDescriptor` into a JSON Schema.
It works with any `@Serializable` class across all supported platforms: JVM, JS/Wasm, and Native.

Use this approach when you need schema generation at runtime without a compile-time processing step,
or when integrating with existing kotlinx.serialization descriptors directly.

> [!NOTE]
> If you own the classes and target multiplatform, consider the [KSP processor](ksp.md) for zero-runtime-overhead compile-time generation.

## Setup

Add the `kotlinx-schema-generator-json` dependency to your project:

```kotlin
// build.gradle.kts
dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-schema-generator-json:<version>")
}
```

No annotation processors, plugins, or additional configuration required.

## Basic Usage

Define your model using a `@Serializable` data class:

<!--- CLEAR -->
<!--- INCLUDE
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName
import kotlinx.schema.generator.json.serialization.SerializationClassJsonSchemaGenerator
import kotlinx.schema.json.encodeToString
import kotlinx.serialization.json.Json
-->
```kotlin
@Serializable
@SerialName("com.example.User")
data class User(val name: String, val age: Int)
```

Then generate the schema using the `Default` singleton:

<!--- INCLUDE
fun main() {
-->
```kotlin
val generator = SerializationClassJsonSchemaGenerator.Default
val schema = generator.generateSchema(User.serializer().descriptor)
println(schema.encodeToString(Json { prettyPrint = true }))
```
<!--- SUFFIX
}
-->
<!--- KNIT example-knit-serializable-01.kt -->

This code prints:
```json
{
    "$schema": "https://json-schema.org/draft/2020-12/schema",
    "$id": "com.example.User",
    "type": "object",
    "properties": {
        "name": {
            "type": "string"
        },
        "age": {
            "type": "integer"
        }
    },
    "additionalProperties": false,
    "required": [
        "name",
        "age"
    ]
}
```

For custom behavior, construct the generator directly with explicit `introspectorConfig` or `jsonSchemaConfig`.

## Configuration

`SerializationClassJsonSchemaGenerator` accepts three optional constructor parameters:

| Parameter            | Type                                          | Default                                | Description                                     |
|:---------------------|:----------------------------------------------|:---------------------------------------|:------------------------------------------------|
| `json`               | `Json`                                        | `Json { encodeDefaults = false; ... }` | JSON configuration, controls discriminator.     |
| `introspectorConfig` | `SerializationClassSchemaIntrospector.Config` | `Config()`                             | Controls how descriptors are introspected.      |
| `jsonSchemaConfig`   | `JsonSchemaConfig`                            | `JsonSchemaConfig.Default`             | Controls schema output (nullability, required). |

### Introspector configuration

`SerializationClassSchemaIntrospector.Config` controls how the generator reads descriptions from annotations on your serializable classes and properties.

```kotlin
public data class Config(
    val descriptionExtractor: DescriptionExtractor = DescriptionExtractor { null }
)
```

By default, no descriptions are extracted unless annotations are recognized by the built-in `Introspections` utility or a custom extractor is provided.

### Custom description extraction

If your project uses a custom annotation to document properties — for example,
a framework annotation or your own convention — provide a `DescriptionExtractor` to map it to the schema `description` field.

`DescriptionExtractor` is a functional interface:

```kotlin
public fun interface DescriptionExtractor {
    public fun extract(annotations: List<Annotation>): String?
}
```

**Example**: extract descriptions from a `@CustomDescription` annotation.

Define your annotation and model:

<!--- CLEAR -->
<!--- INCLUDE
import kotlinx.schema.generator.json.JsonSchemaConfig
import kotlinx.schema.generator.json.serialization.SerializationClassJsonSchemaGenerator
import kotlinx.schema.generator.json.serialization.SerializationClassSchemaIntrospector
import kotlinx.schema.json.JsonSchema
import kotlinx.schema.json.encodeToString
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialInfo
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
-->
```kotlin
@OptIn(ExperimentalSerializationApi::class)
@SerialInfo
annotation class CustomDescription(val value: String)

@Serializable
@SerialName("com.example.Person")
data class Person(
    @property:CustomDescription("First name of the person")
    val firstName: String,
)
```

Then create a generator with a `DescriptionExtractor` that reads from `@CustomDescription`:

<!--- INCLUDE
fun main() {
-->
```kotlin
val generator = SerializationClassJsonSchemaGenerator(
    introspectorConfig = SerializationClassSchemaIntrospector.Config(
        descriptionExtractor = { annotations ->
            annotations.filterIsInstance<CustomDescription>().firstOrNull()?.value
        },
    ),
)

val schema: JsonSchema = generator.generateSchema(Person.serializer().descriptor)
val schemaString: String = schema.encodeToString(Json { prettyPrint = true })

println(schemaString)
```
<!--- SUFFIX
}
-->
<!--- KNIT example-knit-serializable-02.kt -->

This code prints:
```json
{
    "$schema": "https://json-schema.org/draft/2020-12/schema",
    "$id": "com.example.Person",
    "type": "object",
    "properties": {
        "firstName": {
            "type": "string",
            "description": "First name of the person"
        }
    },
    "additionalProperties": false,
    "required": [
        "firstName"
    ]
}
```

> [!TIP]
> The extractor receives the full annotation list for each property or class. You can combine multiple annotation sources or apply fallback logic inside the lambda.

The built-in `@Description` annotation from `kotlinx-schema-annotations` is always recognized without a custom extractor. See [Annotation Reference](../README.md#using-schema-and-description-annotations).

### JSON Schema output configuration

Pass a `JsonSchemaConfig` to control how nullable types and required fields appear in the output:

<!--- CLEAR -->
<!--- INCLUDE
import kotlinx.schema.generator.json.JsonSchemaConfig
import kotlinx.schema.generator.json.serialization.SerializationClassJsonSchemaGenerator
import kotlinx.schema.json.JsonSchema
import kotlinx.schema.json.encodeToString
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName
import kotlinx.serialization.json.Json

fun main() {
-->
```kotlin
@Serializable
@SerialName("com.example.Person")
data class Person(
    val firstName: String,
)


val generator = SerializationClassJsonSchemaGenerator(
    jsonSchemaConfig = JsonSchemaConfig.Strict,
)

val schema: JsonSchema = generator.generateSchema(Person.serializer().descriptor)
val schemaString: String = schema.encodeToString(Json { prettyPrint = true })

println(schemaString)
```
<!--- SUFFIX
}
-->
<!--- KNIT example-knit-serializable-03.kt -->

This code generates:
```json
{
    "$schema": "https://json-schema.org/draft/2020-12/schema",
    "$id": "com.example.Person",
    "type": "object",
    "properties": {
        "firstName": {
            "type": "string"
        }
    },
    "additionalProperties": false,
    "required": [
        "firstName"
    ]
}
```

### JsonSchemaConfig presets

| Preset    | Description                                                                                    |
|:----------|:-----------------------------------------------------------------------------------------------|
| `Default` | Respects default values; nullable fields use union types `["string", "null"]`.                 |
| `Strict`  | All fields required (including nullable); union types. Use for OpenAI strict function calling. |
| `OpenAPI` | Nullable fields use `"nullable": true`; includes `discriminator` for polymorphic types.        |

### JsonSchemaConfig reference

| Property                 | Type      | Default | Description                                                                    |
|:-------------------------|:----------|:--------|:-------------------------------------------------------------------------------|
| `respectDefaultPresence` | `Boolean` | `false` | Mark fields with default values as optional (requires reflection).             |
| `requireNullableFields`  | `Boolean` | `true`  | Whether nullable fields appear in the `required` array.                        |
| `useUnionTypes`          | `Boolean` | `true`  | Represent nullable types as `["string", "null"]` (Draft 2020-12).              |
| `useNullableField`       | `Boolean` | `false` | Emit `"nullable": true` instead of union types (legacy OpenAPI compatibility). |
| `includeDiscriminator`   | `Boolean` | `false` | Include a `discriminator` object in polymorphic schemas (OpenAPI 3.x).         |

> [!NOTE]
> `useUnionTypes` and `useNullableField` are mutually exclusive — exactly one must be `true`.

## Polymorphic types

Sealed classes are supported. The generator reads the discriminator configuration from the `Json` instance you provide:

<!--- CLEAR -->
<!--- INCLUDE
import kotlinx.schema.generator.json.serialization.SerializationClassJsonSchemaGenerator
import kotlinx.schema.json.JsonSchema
import kotlinx.schema.json.encodeToString
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName
import kotlinx.serialization.json.Json

-->
```kotlin
@Serializable
@SerialName("com.example.Shape")
sealed class Shape {
    @Serializable
    @SerialName("com.example.Shape.Circle") 
    data class Circle(val radius: Double) : Shape()
    
    @Serializable
    @SerialName("com.example.Shape.Rectangle") 
    data class Rectangle(val width: Double, val height: Double) : Shape()
}
```
<!--- INCLUDE
fun main() {
-->
```kotlin
val generator = SerializationClassJsonSchemaGenerator(
    json = Json { classDiscriminator = "type" }
)

val schema: JsonSchema = generator.generateSchema(Shape.serializer().descriptor)
val schemaString: String = schema.encodeToString(Json { prettyPrint = true })

println(schemaString)
```
<!--- SUFFIX
}
-->
<!--- KNIT example-knit-serializable-04.kt -->

The generated schema uses `oneOf` with a `$defs` section for each subtype, with a required `type` discriminator field on each subtype object.

```json
{
    "$schema": "https://json-schema.org/draft/2020-12/schema",
    "$id": "com.example.Shape",
    "type": "object",
    "additionalProperties": false,
    "oneOf": [
        {
            "$ref": "#/$defs/com.example.Shape.Circle"
        },
        {
            "$ref": "#/$defs/com.example.Shape.Rectangle"
        }
    ],
    "$defs": {
        "com.example.Shape.Circle": {
            "type": "object",
            "properties": {
                "radius": {
                    "type": "number"
                }
            },
            "required": [
                "radius"
            ],
            "additionalProperties": false
        },
        "com.example.Shape.Rectangle": {
            "type": "object",
            "properties": {
                "width": {
                    "type": "number"
                },
                "height": {
                    "type": "number"
                }
            },
            "required": [
                "width",
                "height"
            ],
            "additionalProperties": false
        }
    }
}
```

## See Also

- [KSP Processor](ksp.md) — Compile-time schema generation with zero runtime overhead
- [Annotation Reference](../README.md#using-schema-and-description-annotations) — `@Schema` and `@Description` usage
- [Multi-Framework Annotation Support](../README.md#multi-framework-annotation-support) — Recognize Jackson, LangChain4j, and other annotations
- [Runtime Schema Generation](../README.md#runtime-schema-generation) — Reflection-based alternative for third-party classes
- [JSON Schema DSL](../kotlinx-schema-json/README.md) — Manual schema construction
