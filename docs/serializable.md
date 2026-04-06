# Serialization-Based Schema Generation

**Table of contents**
<!--- TOC -->

* [Overview](#overview)
* [Setup](#setup)
* [Basic Usage](#basic-usage)
* [Configuration](#configuration)
  * [Introspector configuration](#introspector-configuration)
  * [@SerialDescription support](#@serialdescription-support)
  * [Custom description extraction](#custom-description-extraction)
  * [JSON Schema output configuration](#json-schema-output-configuration)
  * [JsonSchemaConfig presets](#jsonschemaconfig-presets)
  * [JsonSchemaConfig reference](#jsonschemaconfig-reference)
* [Polymorphic types](#polymorphic-types)
  * [Sealed polymorphism](#sealed-polymorphism)
  * [Excluding subtypes with @SerialSchemaIgnore](#excluding-subtypes-with-@serialschemaignore)
  * [Open polymorphism](#open-polymorphism)
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

| Parameter            | Type                                          | Default                    | Description                                     |
|:---------------------|:----------------------------------------------|:---------------------------|:------------------------------------------------|
| `json`               | `Json`                                        | `Json.Default`             | Controls JSON configuration.                    |
| `introspectorConfig` | `SerializationClassSchemaIntrospector.Config` | `Config()`                 | Controls how descriptors are introspected.      |
| `jsonSchemaConfig`   | `JsonSchemaConfig`                            | `JsonSchemaConfig.Default` | Controls schema output (nullability, required). |

### Introspector configuration

`SerializationClassSchemaIntrospector.Config` controls how the generator reads descriptions from
annotations on your serializable classes and properties. Its single field is:

| Field | Type | Default |
|:------|:-----|:--------|
| `descriptionExtractor` | `DescriptionExtractor` | Reads `@SerialDescription` |

By default, the generator recognizes `@SerialDescription` with no extra setup. To read a
different annotation, supply a custom `DescriptionExtractor` — see [Custom description extraction](#custom-description-extraction) below.

### @SerialDescription support

`@SerialDescription` is the built-in way to add descriptions to `@Serializable` classes.
Unlike `@Description`, it carries `@SerialInfo` so the serialization runtime preserves it in
`SerialDescriptor` — no custom configuration required.

It ships in the `kotlinx-schema-generator-json` module (`kotlinx.schema.generator.json` package),
so you already have it when using the serialization-based generator.

Add it to your class and constructor parameters:

<!--- CLEAR -->
<!--- INCLUDE
import kotlinx.schema.generator.json.SerialDescription
import kotlinx.schema.generator.json.serialization.SerializationClassJsonSchemaGenerator
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
-->
```kotlin
@Serializable
@SerialName("SuperUser")
@SerialDescription("A registered user account")
data class User(
    @property:SerialDescription("The user's display name")
    val name: String,
    val age: Int,
)
```

Generate the schema using `Default` — descriptions appear automatically:

<!--- INCLUDE
fun main() {
-->
```kotlin
val schema = SerializationClassJsonSchemaGenerator.Default.generateSchemaString(User.serializer().descriptor)
println(schema)
```
<!--- SUFFIX
}
-->
<!--- KNIT example-knit-serializable-02.kt -->

This code prints:
```json
{
  "$schema": "https://json-schema.org/draft/2020-12/schema",
  "$id": "SuperUser",
  "description": "A registered user account",
  "type": "object",
  "properties": {
    "name": { "type": "string", "description": "The user's display name" },
    "age": { "type": "integer" }
  },
  "required": ["name", "age"],
  "additionalProperties": false
}
```

> [!TIP]
> Use `@property:SerialDescription(...)` on constructor parameters to ensure the annotation
> reaches the backing property — required for the serialization runtime to include it in
> `SerialDescriptor.getElementAnnotations()`.

### Custom description extraction

If your project already uses a different annotation to document properties — for example,
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
@SerialInfo // Required: only @SerialInfo annotations are preserved in SerialDescriptor
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
<!--- KNIT example-knit-serializable-03.kt -->

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

> [!TIP]
> Descriptions are propagated for all property types — including nested objects, sealed classes,
> and collections. A description annotation on a property whose type is another class will appear
> in the schema alongside the `$ref` for that class.

> [!TIP]
> For inline value classes, the **class-level** description annotation propagates to the flattened
> primitive property in the schema. If the property itself also has a description annotation,
> the property-level one takes precedence. Annotations on the inner `value` property are not used.

> [!IMPORTANT]
> `SerialDescriptor` only carries annotations marked with
> [`@SerialInfo`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-core/kotlinx.serialization/-serial-info/).
> The built-in `@Description` from `kotlinx-schema-annotations` lacks `@SerialInfo` and is therefore **not** visible here.
> Use [`@SerialDescription`](#serialdescription-support) from this module instead — it carries `@SerialInfo` and works with `Default` out of the box.
> If you're describing classes you don't own, use the [KSP processor](ksp.md) or
> the [reflection-based generator](../README.md#runtime-schema-generation).

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
<!--- KNIT example-knit-serializable-04.kt -->

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

| Property                                 | Type      | Default | Description                                                                                                            |
|:-----------------------------------------|:----------|:--------|:-----------------------------------------------------------------------------------------------------------------------|
| `respectDefaultPresence`                 | `Boolean` | `true`  | Mark fields with default values as optional (requires reflection; KSP tracks presence but not values).                 |
| `requireNullableFields`                  | `Boolean` | `false` | Include nullable fields in the `required` array.                                                                       |
| `useUnionTypes`                          | `Boolean` | `true`  | Represent nullable types as `["string", "null"]` (Draft 2020-12).                                                      |
| `useNullableField`                       | `Boolean` | `false` | Emit `"nullable": true` instead of union types (legacy OpenAPI compatibility).                                         |
| `includePolymorphicDiscriminator`        | `Boolean` | `true`  | Add a `"type"` property with a constant discriminator value to each polymorphic subtype schema.                        |
| `includeOpenAPIPolymorphicDiscriminator` | `Boolean` | `false` | Include a `discriminator` mapping object in `oneOf` schemas (OpenAPI 3.x). Requires `includePolymorphicDiscriminator`. |

> [!NOTE]
> `useUnionTypes` and `useNullableField` are mutually exclusive — exactly one must be `true`.

## Polymorphic types

Both sealed and open polymorphic hierarchies are supported. The generator reads the discriminator
name from `Json.classDiscriminator` and produces `oneOf` schemas with `$defs` for each subtype.

### Sealed polymorphism

For sealed classes, the compiler embeds all subtypes in the `SerialDescriptor` — no `SerializersModule` needed:

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
<!--- KNIT example-knit-serializable-05.kt -->

Each subtype gets a required discriminator property containing the subtype's serial name as a constant:

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
                "type": {
                    "type": "string",
                    "const": "com.example.Shape.Circle"
                },
                "radius": {
                    "type": "number"
                }
            },
            "required": [
                "type",
                "radius"
            ],
            "additionalProperties": false
        },
        "com.example.Shape.Rectangle": {
            "type": "object",
            "properties": {
                "type": {
                    "type": "string",
                    "const": "com.example.Shape.Rectangle"
                },
                "width": {
                    "type": "number"
                },
                "height": {
                    "type": "number"
                }
            },
            "required": [
                "type",
                "width",
                "height"
            ],
            "additionalProperties": false
        }
    }
}
```

### Excluding subtypes with @SerialSchemaIgnore

Sealed hierarchies sometimes contain subtypes that shouldn't appear in the schema — internal sentinels,
testing stubs, or deprecated variants. Annotate them with `@SerialSchemaIgnore` to exclude them from
the generated `oneOf` composition and `$defs`:

```kotlin
@Serializable
@SerialName("com.example.Event")
sealed class Event {
    @Serializable
    @SerialName("com.example.Event.Click")
    data class Click(val x: Int, val y: Int) : Event()

    @Serializable
    @SerialName("com.example.Event.Internal")
    @SerialSchemaIgnore
    data class Internal(val trace: String) : Event()
}
```

The `Internal` subtype compiles and serializes normally — only schema generation skips it.

> [!NOTE]
> `@SerialSchemaIgnore` carries `@SerialInfo` so it's preserved in `SerialDescriptor`.
> The plain `@SchemaIgnore` annotation (from `kotlinx-schema-annotations`) works with KSP and
> reflection generators but is not visible through `SerialDescriptor`.

### Open polymorphism

For abstract classes and interfaces where subtypes aren't known at compile time, register them
in a `SerializersModule` and pass it via the `Json` configuration. Only the registered subtypes
appear in the generated schema:

<!--- CLEAR -->
<!--- INCLUDE
import kotlinx.schema.generator.json.serialization.SerializationClassJsonSchemaGenerator
import kotlinx.serialization.PolymorphicSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
-->
```kotlin
@Serializable
@SerialName("com.example.Flying")
abstract class Flying {
    abstract val name: String
}

@Serializable
@SerialName("com.example.Bird")
data class Bird(override val name: String, val wingspan: Double) : Flying()

@Serializable
@SerialName("com.example.Kite")
data class Kite(override val name: String, val material: String) : Flying()
```

<!--- INCLUDE
fun main() {
-->
```kotlin
val module = SerializersModule {
    polymorphic(Flying::class) {
        subclass(Bird::class)
        subclass(Kite::class)
    }
}

val generator = SerializationClassJsonSchemaGenerator(
    json = Json { serializersModule = module }
)

val schema = generator.generateSchemaString(
    PolymorphicSerializer(Flying::class).descriptor
)

println(schema)
```
<!--- SUFFIX
}
-->
<!--- KNIT example-knit-serializable-06.kt -->

The output follows the same `oneOf` / `$defs` structure as sealed classes. Register only a subset
of subtypes to produce a schema limited to those types.

> [!NOTE]
> Use `PolymorphicSerializer(Base::class).descriptor` as the entry point for open polymorphic
> types — `Base.serializer()` is not available for non-sealed abstract classes.

## See Also

- [KSP Processor](ksp.md) — Compile-time schema generation with zero runtime overhead
- [Annotation Reference](../README.md#using-schema-and-description-annotations) — `@Schema`, `@Description`, and `@SerialDescription` usage
- [Multi-Framework Annotation Support](../README.md#multi-framework-annotation-support) — Recognize Jackson, LangChain4j, and other annotations
- [Runtime Schema Generation](../README.md#runtime-schema-generation) — Reflection-based alternative for third-party classes
- [JSON Schema DSL](../kotlinx-schema-json/README.md) — Manual schema construction
