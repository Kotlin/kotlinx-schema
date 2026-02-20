[![JetBrains incubator project](https://jb.gg/badges/incubator.svg)](https://confluence.jetbrains.com/display/ALL/JetBrains+on+GitHub)
[![JetBrains Experimental](https://kotl.in/badges/experimental.svg)](https://confluence.jetbrains.com/display/ALL/JetBrains+on+GitHub)

[![Maven Central](https://img.shields.io/maven-central/v/org.jetbrains.kotlinx/kotlinx-schema-ksp.svg?label=Maven%20Central)](https://central.sonatype.com/search?q=org.jetbrains.kotlinx%2Fkotlinx-schema-*)
[![Build with Gradle](https://github.com/Kotlin/kotlinx-schema/actions/workflows/build.yml/badge.svg)](https://github.com/Kotlin/kotlinx-schema/actions/workflows/build.yml)
[![CodeQL](https://github.com/Kotlin/kotlinx-schema/actions/workflows/github-code-scanning/codeql/badge.svg)](https://github.com/Kotlin/kotlinx-schema/actions/workflows/github-code-scanning/codeql)
[![Docs](https://img.shields.io/badge/Docs-Live-blue?logo=kotlin)](https://kotlin.github.io/kotlinx-schema/)
[![Examples](https://img.shields.io/badge/Examples-blue?logo=github)](https://github.com/Kotlin/kotlinx-schema/tree/main/examples)
[![Ask DeepWiki](https://deepwiki.com/badge.svg)](https://deepwiki.com/Kotlin/kotlinx-schema)

[![Kotlin](https://img.shields.io/badge/kotlin-2.2+-blueviolet.svg?logo=kotlin)](http://kotlinlang.org)
[![Kotlin Multiplatform](https://img.shields.io/badge/Platforms-%20JVM%20%7C%20Wasm%2FJS%20%7C%20Native%20-blueviolet?logo=kotlin)](https://kotlinlang.org/docs/multiplatform.html)
[![JVM](https://img.shields.io/badge/JVM-17+-red.svg?logo=jvm)](http://java.com)
[![License](https://img.shields.io/badge/License-Apache_2.0-yellow.svg)](LICENSE)

**Table of contents:**
<!--- TOC -->

* [Key Features](#key-features)
* [Why kotlinx-schema?](#why-kotlinx-schema?)
  * [When to Use](#when-to-use)
* [Choosing Your Approach](#choosing-your-approach)
* [Quick Start](#quick-start)
  * [Annotate Your Models](#annotate-your-models)
  * [Configuration](#configuration)
    * [Quick Setup](#quick-setup)
* [Runtime schema generation](#runtime-schema-generation)
  * [Why Runtime Generation?](#why-runtime-generation?)
  * [Usage](#usage)
* [What Gets Generated](#what-gets-generated)
* [Examples](#examples)
  * [Basic data classes](#basic-data-classes)
  * [Enums with descriptions](#enums-with-descriptions)
  * [Nested objects](#nested-objects)
  * [Generic types](#generic-types)
  * [Sealed class polymorphism](#sealed-class-polymorphism)
* [Using @Schema and @Description annotations](#using-@schema-and-@description-annotations)
  * [@Schema annotation](#@schema-annotation)
  * [@Description annotation](#@description-annotation)
* [Function calling schema generation for LLMs](#function-calling-schema-generation-for-llms)
  * [Why This Format?](#why-this-format?)
  * [Basic Usage](#basic-usage)
  * [Generated Schema](#generated-schema)
  * [Key Features](#key-features)
  * [Working with Multiple Functions](#working-with-multiple-functions)
  * [Nullable Parameters](#nullable-parameters)
  * [Compile-time Function Schema Generation (KSP)](#compile-time-function-schema-generation-ksp)
    * [Top-Level Functions](#top-level-functions)
    * [Instance/Member Functions](#instancemember-functions)
    * [Companion Object Functions](#companion-object-functions)
    * [Singleton Object Functions](#singleton-object-functions)
    * [What Gets Generated](#what-gets-generated)
    * [Schema Format](#schema-format)
    * [KSP vs Runtime Reflection](#ksp-vs-runtime-reflection)
    * [Suspend Functions](#suspend-functions)
* [Multi-Framework Annotation Support](#multi-framework-annotation-support)
  * [Supported Annotations](#supported-annotations)
  * [How It Works](#how-it-works)
  * [Customizing Annotation Detection](#customizing-annotation-detection)
    * [Default Configuration](#default-configuration)
    * [Adding Custom Annotations](#adding-custom-annotations)
    * [Example: Adding Support for a Custom Framework](#example-adding-support-for-a-custom-framework)
  * [Example: Reusing Jackson Annotations](#example-reusing-jackson-annotations)
  * [Example: LangChain4j Integration](#example-langchain4j-integration)
  * [Example: Koog AI Agents](#example-koog-ai-agents)
  * [Precedence Rules](#precedence-rules)
* [JSON Schema DSL](#json-schema-dsl)
* [Building and Contributing](#building-and-contributing)
* [Requirements](#requirements)
* [Code of Conduct](#code-of-conduct)
* [License](#license)

<!--- END -->

# kotlinx-schema

**Generate JSON schemas and LLM function calling schemas from Kotlin code — including classes you don't own.**

> [!IMPORTANT]
> Given the highly experimental nature of this work, nothing is settled in stone.
> [Kotlinx-schema-json](kotlinx-schema-json) might eventually be moved
> to [kotlinx-serialization](https://github.com/Kotlin/kotlinx.serialization).

Quick Links:

- [KSP Configuration Guide](docs/ksp.md)
- [Serialization-Based Schema Generation](docs/serializable.md)
- [Project Architecture](docs/architecture.md)

## Key Features

**Dual Generation Modes:**

- **Compile-time (KSP)**: Zero runtime overhead, multiplatform, for your annotated classes
- **Runtime (Reflection)**: JVM-only, for any class including third-party libraries
- **Runtime (SerialDescriptor)**: Kotlin serializable classes

**LLM Integration:**

- First-class support for OpenAI/Anthropic function calling format
- Automatic strict mode and parameter validation
- Function name and description extraction

**Flexible Annotation Support:**

- Recognizes `@Description`, `@LLMDescription`, `@JsonPropertyDescription`, `@P`, and more
- Recognizes KDoc
- Works with annotations from Jackson, LangChain4j, Koog without code changes

**Comprehensive Type Support:**

- Enums, collections, maps, nested objects, nullability, generics (with star-projection)
- Sealed class hierarchies with automatic `oneOf` generation
- Proper union types for nullable parameters (`["string", "null"]`)
- Type constraints (min/max, patterns, formats)
- **Default values** (compile-time: tracked but not extracted; runtime: fully extracted)

**Developer Experience:**

- Gradle plugin for one-line setup
- Type-safe Kotlin DSL for programmatic schema construction
- Works everywhere: JVM, JS, iOS, macOS, Wasm

> [!TIP]
> **Need to build JSON Schemas manually?** The [**kotlinx-schema-json**](kotlinx-schema-json) module provides type-safe
> Kotlin models and DSL compliant with [JSON Schema Draft 2020-12](https://json-schema.org/draft/2020-12/schema), with
> support for polymorphism, discriminators, and type-safe enums. [See JSON Schema DSL section ↓](#json-schema-dsl)

## Why kotlinx-schema?

This library solves three key challenges:

1. **🤖 LLM Function Calling Integration**: Generate OpenAI/Anthropic-compatible function schemas directly from Kotlin
   functions with proper type definitions and descriptions
2. **📦 Third-Party Class Support**: Create schemas for library classes without modifying their source code (Spring
   entities, Ktor models, etc.)
3. **🔄 Multi-Framework Compatibility**: Works with existing annotations from Jackson, LangChain4j, Koog, and more — no
   code changes needed

### When to Use

* 🤖 Building LLM-powered applications with structured function calling (OpenAI, Anthropic, Claude, MCP)
* 👽 Need schemas for third-party library classes you cannot modify
* ✅ Already using `@Description`-like annotations from other frameworks
* 👌 Want zero runtime overhead with compile-time generation (multiplatform support)
* ☕️ Need dynamic schema generation at runtime via reflection (JVM)

## Choosing Your Approach

|                                   | 🔧 [KSP Processor](docs/ksp.md) | 📦 [Serialization-based](docs/serializable.md) | 🔍 [Runtime Reflection](#runtime-schema-generation) |
|:----------------------------------|:-------------------------------:|:----------------------------------------------:|:---------------------------------------------------:|
| **Platforms**                     |       JVM + Multiplatform       |              JVM + Multiplatform               |                      JVM only                       |
| **When generated**                |          Compile-time           |                    Runtime                     |                       Runtime                       |
| **Requires annotation processor** |            Yes (KSP)            |                       No                       |                         No                          |
| **Class must be `@Serializable`** |               No                |                      Yes                       |                         No                          |
| **Annotate class with `@Schema`** |            Required             |                  Not required                  |                    Not required                     |
| **KDoc extracted to description** |                ✅                |                       ❌                        |                          ❌                          |
| **Extract data class defaults**   |                ❌                |                       ❌                        |                          ✅                          |
| **Third-party classes**           |                ❌                |            ✅ (only `@Serializable`)            |                   ✅ any JVM class                   |

- **[Pick KSP](docs/ksp.md)** when you own the classes, want zero runtime overhead, and target Multiplatform or need KDoc
in your schema.

- **[Pick Serialization-based](docs/serializable.md)** when your classes are already `@Serializable` and you need
Multiplatform support without a build-time processor.

- **[Pick Reflection](#runtime-schema-generation)** when you need JVM-only runtime generation for third-party classes, or
need to extract data class default values. Works equally well for `@Serializable` classes on JVM.

## Quick Start

Recommended: use the Gradle plugin.
It applies KSP for you, wires generated sources, and sets up task dependencies.

Refer to the example projects [here](./examples).

### Annotate Your Models

<!--- CLEAR -->
<!--- INCLUDE
import kotlinx.schema.Schema
import kotlinx.schema.Description
-->

```kotlin
/**
 * A postal address for deliveries and billing.
 */
@Schema
data class Address(
    @Description("Street address, including house number") val street: String,
    @Description("City or town name") val city: String,
    @Description("Postal or ZIP code") val zipCode: String,
    @Description("Two-letter ISO country code; defaults to US") val country: String = "US",
)
```

<!--- KNIT example-knit-readme-01.kt --> 

> **Note:** KDoc comments on classes can also be used as descriptions.

### Configuration

#### Quick Setup

Add the Google KSP plugin and the processor dependency:

```kotlin
// Multiplatform (KMP)
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
```

For JVM-only projects, the Kotlinx-Schema Gradle plugin, Maven, and full configuration options, see *
*[KSP Configuration Guide](docs/ksp.md)**.

## Runtime schema generation

> [!NOTE]
> If your classes are `@Serializable`, use the [Serialization-Based Generator](docs/serializable.md) instead — it works
> on all platforms without reflection.

For JVM-only scenarios with classes you don't own or can't annotate, use
[
`ReflectionClassJsonSchemaGenerator`](kotlinx-schema-generator-json/src/main/kotlin/kotlinx/schema/generator/json/ReflectionClassJsonSchemaGenerator.kt)
and [
`ReflectionFunctionCallingSchemaGenerator`](kotlinx-schema-generator-json/src/main/kotlin/kotlinx/schema/generator/json/ReflectionFunctionCallingSchemaGenerator.kt)
with Kotlin reflection.

### Why Runtime Generation?

**Primary use case: Third-party library classes**

The compile-time (KSP) approach requires you to annotate classes with `@Schema`, which isn't possible for:

- Library classes (Spring entities, Ktor models, database classes)
- Framework-provided models
- Classes from dependencies you don't control

Runtime generation solves this by using reflection to analyze any class at runtime.

> [!IMPORTANT]
> **Limitations:**
> - KDoc annotations are not available at runtime
> - Function parameter defaults (e.g., `fun foo(x: Int = 5)`) cannot be extracted via reflection
> - Data class property defaults (e.g., `data class Config(val port: Int = 8080)`) ARE supported

### Usage

<!--- CLEAR -->
<!--- INCLUDE
import kotlinx.schema.Schema
import kotlinx.schema.json.JsonSchema
-->

```kotlin
// Works with ANY class, even from third-party libraries
import com.thirdparty.library.User  // Not your code!

val generator = kotlinx.schema.generator.json.ReflectionClassJsonSchemaGenerator.Default
val schema: JsonSchema = generator.generateSchema(User::class)
val schemaString: String = generator.generateSchemaString(User::class)
```

<!--- KNIT example-knit-readme-02.kt -->

**Add dependency**: `org.jetbrains.kotlinx:kotlinx-schema-generator-json:<version>`

## What Gets Generated

Schemas follow JSON Schema Draft 2020-12 format. Example (pretty-printed):

```json
{
    "$schema": "https://json-schema.org/draft/2020-12/schema",
    "$id": "com.example.Address",
    "type": "object",
    "properties": {
        "street": {
            "type": "string",
            "description": "Street address, including house number"
        },
        "city": {
            "type": "string",
            "description": "City or town name"
        },
        "zipCode": {
            "type": "string",
            "description": "Postal or ZIP code"
        },
        "country": {
            "type": "string",
            "description": "Two-letter ISO country code; defaults to US",
            "default": "US"
        }
    },
    "required": [
        "street",
        "city",
        "zipCode"
    ],
    "additionalProperties": false,
    "description": "A postal address for deliveries and billing."
}
```

- Enums are `type: string` with `enum: []` and carry `@Description` as `description`.
- Object properties include their inferred type schema and, when present, property-level `@Description` as`description`.
- **Default values** are automatically extracted and included in the schema when using **runtime reflection** (e.g.,
  `val country: String = "US"` → `"default": "US"`). Note: KSP (compile-time) tracks which properties have defaults but
  cannot extract the actual values.
- Nullable properties are emitted as a union including `null`.
- Collections: `List<T>`/`Set<T>` → `{ "type":"array", "items": T }`; `Map<String, V>` →
  `{ "type":"object", "additionalProperties": V }`.
- Unknown/generic type parameters resolve to `kotlin.Any` with a minimal definition in `$defs`.

## Examples

### Basic data classes

Here's a practical example of a product model with various property types:

<!--- CLEAR -->
<!--- INCLUDE
import kotlinx.schema.Schema
import kotlinx.schema.Description
-->

```kotlin
@Description("A purchasable product with pricing and inventory info.")
@Schema
data class Product(
    @Description("Unique identifier for the product")
    val id: Long,
    @Description("Human-readable product name")
    val name: String,
    @Description("Optional detailed description of the product")
    val description: String?,
    @Description("Unit price expressed as a decimal number")
    val price: Double,
    @Description("Whether the product is currently in stock")
    val inStock: Boolean = true,
    @Description("List of tags for categorization and search")
    val tags: List<String> = emptyList(),
)
```

<!--- KNIT example-knit-readme-03.kt --> 

Use the generated extensions:

```kotlin
val schema = Product::class.jsonSchemaString
val schemaObject = Product::class.jsonSchema
```

<details>
<summary>Generated JSON schema</summary>

```json
{
    "$schema": "https://json-schema.org/draft/2020-12/schema",
    "$id": "com.example.Product",
    "type": "object",
    "properties": {
        "id": {
            "type": "integer",
            "description": "Unique identifier for the product"
        },
        "name": {
            "type": "string",
            "description": "Human-readable product name"
        },
        "description": {
            "type": [
                "string",
                "null"
            ],
            "description": "Optional detailed description of the product"
        },
        "price": {
            "type": "number",
            "description": "Unit price expressed as a decimal number"
        },
        "inStock": {
            "type": "boolean",
            "description": "Whether the product is currently in stock",
            "default": true
        },
        "tags": {
            "type": "array",
            "items": {
                "type": "string"
            },
            "description": "List of tags for categorization and search",
            "default": []
        }
    },
    "required": [
        "id",
        "name",
        "description",
        "price"
    ],
    "additionalProperties": false,
    "description": "A purchasable product with pricing and inventory info."
}
```

</details>

### Enums with descriptions

Enums are supported with descriptions on both the enum class and individual values:

<!--- CLEAR -->
<!--- INCLUDE
import kotlinx.schema.Schema
import kotlinx.schema.Description
-->

```kotlin
@Description("Current lifecycle status of an entity.")
@Schema
enum class Status {
    @Description("Entity is active and usable")
    ACTIVE,

    @Description("Entity is inactive or disabled")
    INACTIVE,

    @Description("Entity is pending activation or approval")
    PENDING,
}
```

<!--- KNIT example-knit-readme-04.kt -->

<details>
<summary>Generated JSON schema</summary>

```json
{
    "$schema": "https://json-schema.org/draft/2020-12/schema",
    "$id": "com.example.Status",
    "type": "string",
    "enum": [
        "ACTIVE",
        "INACTIVE",
        "PENDING"
    ],
    "description": "Current lifecycle status of an entity."
}
```

</details>

### Nested objects

You can compose schemas by nesting annotated classes:

<!--- CLEAR -->
<!--- INCLUDE
import kotlinx.schema.Schema
import kotlinx.schema.Description

@Schema
data class Address(val street: String, val city: String, val zipCode: String, val country: String = "US")

@Schema
data class Product(val id: Long, val name: String, val description: String?, val price: Double, val inStock: Boolean = true, val tags: List<String> = emptyList())

@Schema
enum class Status { ACTIVE, INACTIVE, PENDING }
-->

```kotlin
@Description("A person with a first and last name and age.")
@Schema
data class Person(
    @Description("Given name of the person")
    val firstName: String,
    @Description("Family name of the person")
    val lastName: String,
    @Description("Age of the person in years")
    val age: Int,
)

@Description("An order placed by a customer containing multiple items.")
@Schema
data class Order(
    @Description("Unique order identifier")
    val id: String,
    @Description("The customer who placed the order")
    val customer: Person,
    @Description("Destination address for shipment")
    val shippingAddress: Address,
    @Description("List of items included in the order")
    val items: List<Product>,
    @Description("Current status of the order")
    val status: Status,
)
```

<!--- KNIT example-knit-readme-05.kt -->

The generated schema for `Order` will automatically include definitions for all nested types (`Person`, `Address`,
`Product`, `Status`) in the `$defs` section, with appropriate `$ref` pointers to link them together. This makes it easy
to build complex, composable data models.

### Generic types

Generic classes are supported, with type parameters resolved at usage sites:

<!--- CLEAR -->
<!--- INCLUDE
import kotlinx.schema.Schema
import kotlinx.schema.Description
-->

```kotlin
@Description("A generic container that wraps content with optional metadata.")
@Schema
data class Container<T>(
    @Description("The wrapped content value")
    val content: T,
    @Description("Arbitrary metadata key-value pairs")
    val metadata: Map<String, Any> = emptyMap(),
)
```

<!--- KNIT example-knit-readme-06.kt -->

Generic type parameters are resolved at the usage site. When generating a schema for a generic class, unbound type
parameters (like `T`) are treated as `kotlin.Any` with a minimal definition in the `$defs` section. For more specific
typing, instantiate the generic class with concrete types when you need them.

### Sealed class polymorphism

The library automatically generates JSON schemas for Kotlin sealed class hierarchies using `oneOf`:

<!--- CLEAR -->
<!--- INCLUDE
import kotlinx.schema.Schema
import kotlinx.schema.Description
import kotlinx.schema.generator.json.ReflectionClassJsonSchemaGenerator
import kotlinx.schema.json.encodeToString
import kotlinx.serialization.json.Json
-->

```kotlin
@Description("Multicellular eukaryotic organism of the kingdom Metazoa")
@Schema
sealed class Animal {
    /**
     * Animal's name
     */
    @Description("Animal's name")
    abstract val name: String

    @Schema(withSchemaObject = true)
    data class Dog(
        @Description("Animal's name")
        override val name: String,
    ) : Animal()

    @Schema(withSchemaObject = true)
    data class Cat(
        @Description("Animal's name")
        override val name: String,
    ) : Animal()
}
```

<!--- INCLUDE 
fun main() {
-->

```kotlin
val generator = ReflectionClassJsonSchemaGenerator.Default
val schema = generator.generateSchema(Animal::class)

println(schema.encodeToString(Json { prettyPrint = true }))
```

<!--- SUFFIX
}
-->
<!--- KNIT example-knit-readme-07.kt -->

<details>
<summary>Generated JSON schema</summary>

```json
{
    "$schema": "https://json-schema.org/draft/2020-12/schema",
    "$id": "kotlinx.schema.integration.type.Animal",
    "description": "Multicellular eukaryotic organism of the kingdom Metazoa",
    "type": "object",
    "additionalProperties": false,
    "oneOf": [
        {
            "$ref": "#/$defs/Animal.Cat"
        },
        {
            "$ref": "#/$defs/Animal.Dog"
        }
    ],
    "$defs": {
        "Animal.Cat": {
            "type": "object",
            "properties": {
                "type": {
                    "type": "string",
                    "const": "Animal.Cat"
                },
                "name": {
                    "type": "string",
                    "description": "Animal's name"
                }
            },
            "required": [
                "type",
                "name"
            ],
            "additionalProperties": false
        },
        "Animal.Dog": {
            "type": "object",
            "properties": {
                "type": {
                    "type": "string",
                    "const": "Animal.Dog"
                },
                "name": {
                    "type": "string",
                    "description": "Animal's name"
                }
            },
            "required": [
                "type",
                "name"
            ],
            "additionalProperties": false
        }
    }
}
```

</details>

**Key features:**

- **`oneOf` with `$ref`**: Each sealed subclass is referenced from a `$defs` section
- **Property inheritance**: Base class properties included in each subtype
- **Type safety**: Each subtype gets its own schema definition
- **Default values**: Subtype properties with defaults (like `lives: Int = 9`) are included

## Using @Schema and @Description annotations

### @Schema annotation

Mark classes with `@Schema` to generate extension properties for them:

```kotlin
@Schema  // Uses default schema type "json"
data class Address(val street: String, val city: String)

@Schema("json")  // Explicitly specify schema type
data class Person(val name: String, val age: Int)
```

**@Schema parameters:**

- `value = "json"`: Schema type (only JSON currently supported)
- `withSchemaObject = false`: Generate `jsonSchema: JsonObject` property (
  see [Advanced Configuration](#advanced-configuration))

**Note**: `jsonSchemaString` is always generated. `jsonSchema` requires `withSchemaObject = true`.

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

**Tip**: With the recommended compiler flag `-Xannotation-default-target=param-property`, a bare `@Description` on a
primary constructor parameter also applies to the property. If you do not enable the flag, use `@param:Description` for
constructor-declared properties.

## Function calling schema generation for LLMs

Modern LLMs (OpenAI GPT-4, Anthropic Claude, etc.) use structured function calling to interact with your code.
They require a specific JSON schema format that describes available functions, their parameters, and types.

### Why This Format?

LLM APIs need to know:

- What functions are available and what they do
- Parameter names, types, and descriptions
- Which parameters are required
- Type constraints (enums, formats, ranges)

This library automatically generates schemas that comply with
the [OpenAI function calling specification](https://platform.openai.com/docs/guides/function-calling), making it easy to
expose Kotlin functions to LLMs.

### Basic Usage

<!--- CLEAR -->
<!--- INCLUDE
import kotlinx.schema.Description
import kotlinx.schema.generator.json.ReflectionFunctionCallingSchemaGenerator
import kotlinx.serialization.Serializable

@Serializable
data class WeatherInfo(val temp: Double, val unit: String)
-->

```kotlin
@Description("Get current weather for a location")
fun getWeather(
    @Description("City and country, e.g. 'London, UK'")
    location: String,

    @Description("Temperature unit")
    unit: String = "celsius"
): WeatherInfo {
    return WeatherInfo(20.0, unit)
}

val generator = ReflectionFunctionCallingSchemaGenerator.Default
val schema = generator.generateSchema(::getWeather)
```

<!--- KNIT example-knit-readme-08.kt -->

### Generated Schema

The generated schema follows the LLM function calling format:

```json
{
    "type": "function",
    "name": "getWeather",
    "description": "Get current weather for a location",
    "strict": true,
    "parameters": {
        "type": "object",
        "properties": {
            "location": {
                "type": "string",
                "description": "City and country, e.g. 'London, UK'"
            },
            "unit": {
                "type": "string",
                "description": "Temperature unit"
            }
        },
        "required": [
            "location",
            "unit"
        ],
        "additionalProperties": false
    }
}
```

### Key Features

- **Automatic extraction**: Function name and descriptions from `@Description` annotations
- **Default values**: Property defaults in nested data classes are automatically extracted (e.g.,
  `data class Config(val port: Int = 8080)`)
- **Strict mode**: `strict: true` enables
  OpenAI's [strict mode](https://platform.openai.com/docs/guides/function-calling#strict-mode) for reliable parsing
- **Union types**: Nullable parameters use `["string", "null"]` instead of `nullable: true`
- **Required by default**: All parameters marked as required (OpenAI structured outputs requirement)
- **Type safety**: Proper JSON Schema types from Kotlin types (Int → integer, String → string, etc.)

> **Note:** Function parameter defaults (e.g., `unit: String = "celsius"`) cannot be extracted via reflection, but
> nested data class property defaults are fully supported.

### Working with Multiple Functions

<!--- CLEAR -->
<!--- INCLUDE
import kotlinx.schema.Description
import kotlinx.schema.generator.json.ReflectionFunctionCallingSchemaGenerator
import kotlinx.serialization.json.Json
import kotlinx.schema.json.encodeToJsonObject
-->

```kotlin
// Define your functions
@Description("Search the knowledge base")
fun searchKnowledge(
    @Description("Search query") query: String,
    @Description("Max results") limit: Int = 10
): String = TODO()

@Description("Calculate order total with tax")
fun calculateTotal(
    @Description("Item prices") prices: List<Double>,
    @Description("Tax rate as decimal") taxRate: Double = 0.0
): Double = TODO()

// Generate schemas
val generator = ReflectionFunctionCallingSchemaGenerator.Default
val schemas = listOf(::searchKnowledge, ::calculateTotal)
    .map { generator.generateSchema(it) }

// Serialize to JSON
val jsonSchemas = schemas.map { Json.encodeToString(it) }

// Or get as JsonObject
val schemaObjects = schemas.map { it.encodeToJsonObject() }
```

<!--- KNIT example-knit-readme-09.kt -->

The generated schemas can be sent to any LLM API that supports function calling (OpenAI, Anthropic, etc.).
Integration with specific LLM providers requires their respective client libraries.

### Nullable Parameters

Nullable parameters are represented as union types:

<!--- CLEAR -->
<!--- INCLUDE
import kotlinx.schema.Description
import kotlinx.serialization.Serializable
-->

```kotlin
@Description("Update user profile")
fun updateProfile(
    @Description("User ID") userId: String,
    @Description("New name, if changing") name: String? = null,
    @Description("New email, if changing") email: String? = null
): Boolean = TODO("does not matter")// ...
```

<!--- KNIT example-knit-readme-10.kt -->

Generates:

```json
{
    "properties": {
        "userId": {
            "type": "string",
            "description": "User ID"
        },
        "name": {
            "type": [
                "string",
                "null"
            ],
            "description": "New name, if changing"
        },
        "email": {
            "type": [
                "string",
                "null"
            ],
            "description": "New email, if changing"
        }
    },
    "required": [
        "userId",
        "name",
        "email"
    ]
}
```

**Note**: Even nullable parameters are in `required` array. The `null` type in the union indicates optionality.

For more details on function calling schemas and OpenAI compatibility,
see [kotlinx-schema-json/README.md](kotlinx-schema-json/README.md#function-calling-schema-for-llm-apis).

### Compile-time Function Schema Generation (KSP)

Generate function schemas at compile time with zero runtime overhead. KSP generates type-safe extensions for all your
annotated functions, with APIs that reflect where functions actually live in your code.

#### Top-Level Functions

Annotate package-level functions to generate top-level schema accessors:

<!--- CLEAR -->
<!--- INCLUDE
import kotlinx.schema.Schema
import kotlinx.schema.Description

fun greetPersonJsonSchemaString(): String = ""
-->

```kotlin
@Schema
@Description("Sends a greeting message to a person")
fun greetPerson(
    @Description("Name of the person to greet")
    name: String,
    @Description("Optional greeting prefix (e.g., 'Hello', 'Hi')")
    greeting: String = "Hello",
): String = "$greeting, $name!"

// Generated: top-level functions
val schema = greetPersonJsonSchemaString()
```

<!--- KNIT example-knit-readme-11.kt -->

#### Instance/Member Functions

Annotate class methods to generate `KClass` extensions on the containing class:

<!--- CLEAR -->
<!--- INCLUDE
import kotlinx.schema.Schema
import kotlinx.schema.Description
import kotlin.reflect.KClass

fun KClass<UserService>.registerUserJsonSchemaString(): String = ""
-->

```kotlin
class UserService {
    @Schema
    @Description("Registers a new user in the system")
    fun registerUser(
        @Description("Username for the new account")
        username: String,
        @Description("Email address")
        email: String,
    ): String = "User registered"
}

// Generated: KClass extension on UserService
val schema = UserService::class.registerUserJsonSchemaString()
```

<!--- KNIT example-knit-readme-12.kt -->

#### Companion Object Functions

Annotate companion methods to generate `KClass` extensions on the companion object itself:

<!--- CLEAR -->
<!--- INCLUDE
import kotlinx.schema.Schema
import kotlinx.schema.Description
import kotlin.reflect.KClass

fun KClass<DatabaseConnection.Companion>.createJsonSchemaString(): String = ""
-->

```kotlin
class DatabaseConnection {
    companion object {
        @Schema
        @Description("Creates a new database connection")
        fun create(
            @Description("Database host")
            host: String,
            @Description("Database port")
            port: Int = 5432,
        ): DatabaseConnection = TODO()
    }
}

// Generated: KClass extension on companion object
val schema = DatabaseConnection.Companion::class.createJsonSchemaString()
```

<!--- KNIT example-knit-readme-13.kt -->

This API correctly reflects that companion functions belong to the companion object, not the parent class.

#### Singleton Object Functions

Annotate object methods to generate `KClass` extensions on the object type:

<!--- CLEAR -->
<!--- INCLUDE
import kotlinx.schema.Schema
import kotlinx.schema.Description
import kotlin.reflect.KClass

fun KClass<ConfigurationManager>.loadConfigJsonSchemaString(): String = ""
-->

```kotlin
object ConfigurationManager {
    @Schema
    @Description("Loads configuration from a file")
    fun loadConfig(
        @Description("Configuration file path")
        filePath: String,
        @Description("Whether to create file if it doesn't exist")
        createIfMissing: Boolean = false,
    ): Map<String, String> = TODO()
}

// Generated: KClass extension on object
val schema = ConfigurationManager::class.loadConfigJsonSchemaString()
```

<!--- KNIT example-knit-readme-14.kt -->

#### What Gets Generated

KSP generates schema accessor functions that match where your functions live:

| Function Type | Annotate         | Generated API                | Example                                                        |
|---------------|------------------|------------------------------|----------------------------------------------------------------|
| **Top-level** | Package function | Top-level accessor           | `greetPersonJsonSchemaString()`                                |
| **Instance**  | Class method     | `KClass` extension           | `UserService::class.registerUserJsonSchemaString()`            |
| **Companion** | Companion method | `Companion::class` extension | `DatabaseConnection.Companion::class.createJsonSchemaString()` |
| **Object**    | Object method    | `Object::class` extension    | `ConfigurationManager::class.loadConfigJsonSchemaString()`     |

For each annotated function, you get:

- **Always**: `{functionName}JsonSchemaString(): String` — returns the schema as a JSON string
- **Optional**: `{functionName}JsonSchema(): FunctionCallingSchema` — returns the schema object (requires
  `withSchemaObject = true`)

#### Schema Format

Generated schemas follow the [OpenAI function calling format](https://platform.openai.com/docs/guides/function-calling):

```json
{
    "type": "function",
    "name": "greetPerson",
    "description": "Sends a greeting message to a person",
    "strict": true,
    "parameters": {
        "type": "object",
        "properties": {
            "name": {
                "type": "string",
                "description": "Name of the person to greet"
            },
            "greeting": {
                "type": "string",
                "description": "Optional greeting prefix"
            }
        },
        "required": [
            "name",
            "greeting"
        ],
        "additionalProperties": false
    }
}
```

**OpenAI Strict Mode**: All parameters are marked as required by default, even those with default values. This ensures
compatibility with [OpenAI Structured Outputs](https://platform.openai.com/docs/guides/function-calling#strict-mode).

#### KSP vs Runtime Reflection

| Feature            | KSP (Compile-time)                                                                      | Reflection (Runtime)                     |
|--------------------|-----------------------------------------------------------------------------------------|------------------------------------------|
| **Performance**    | Zero runtime cost                                                                       | Small reflection overhead                |
| **Platforms**      | Multiplatform                                                                           | JVM only                                 |
| **Default values** | Tracked but not extracted ([KSP limitation](https://github.com/google/ksp/issues/1868)) | Fully extracted from data classes        |
| **When to use**    | Your annotated functions                                                                | Third-party functions, dynamic scenarios |

#### Suspend Functions

Suspend functions work identically to regular functions. The generated schemas don't expose the suspend modifier—they
describe parameter types only:

<!--- CLEAR -->
<!--- INCLUDE
import kotlinx.schema.Schema
import kotlinx.schema.Description
import kotlinx.serialization.Serializable

@Serializable
data class UserData(val id: Long)

fun fetchUserDataJsonSchemaString(): String = ""
-->

```kotlin
@Schema
@Description("Fetches user data asynchronously")
suspend fun fetchUserData(
    @Description("User ID to fetch") userId: Long,
): UserData = TODO()

// Generated API works the same way
val schema = fetchUserDataJsonSchemaString()
```

<!--- KNIT example-knit-readme-15.kt -->

## Multi-Framework Annotation Support

**You don't need to change your existing code!**

kotlinx-schema recognizes description annotations from multiple frameworks by their **simple name**, allowing you to
generate schemas from code that uses annotations from other libraries.

### Supported Annotations

The library automatically recognizes these description annotations by default:

| Annotation                                                 | Simple Name               | Library/Framework | Example                               |
|------------------------------------------------------------|---------------------------|-------------------|---------------------------------------|
| `kotlinx.schema.Description`                               | `Description`             | kotlinx-schema    | `@Description("User name")`           |
| `ai.koog.agents.core.tools.annotations.LLMDescription`     | `LLMDescription`          | Koog AI agents    | `@LLMDescription("Query text")`       |
| `com.fasterxml.jackson.annotation.JsonPropertyDescription` | `JsonPropertyDescription` | Jackson           | `@JsonPropertyDescription("Email")`   |
| `com.fasterxml.jackson.annotation.JsonClassDescription`    | `JsonClassDescription`    | Jackson           | `@JsonClassDescription("User model")` |
| `dev.langchain4j.model.output.structured.P`                | `P`                       | LangChain4j       | `@P("Search query")`                  |

### How It Works

The introspector matches annotations by their **simple name only**, not the fully qualified name. This means:

- ✅ No code changes needed to generate schemas from existing annotated classes
- ✅ Can migrate between annotation libraries without modifying code
- ✅ Generate schemas for third-party code that uses different annotations
- ✅ Use your preferred annotation library while still getting schema generation

### Customizing Annotation Detection

Annotation detection is configurable via `kotlinx-schema.properties` loaded from the classpath.
The configuration file is **optional** — if not provided or fails to load, the library uses sensible defaults.

#### Default Configuration

By default, the library recognizes:

**Annotation names**: Description, LLMDescription, JsonPropertyDescription, JsonClassDescription, P
**Attribute names**: value, description

#### Adding Custom Annotations

To customize, place `kotlinx-schema.properties` in your project's resources:

```properties
# Add your custom annotations to the defaults
introspector.annotations.description.names=Description,MyCustomAnnotation,DocString
introspector.annotations.description.attributes=value,description,text
```

**Note**: The library falls back to built-in defaults if the configuration file is missing or cannot be loaded.

#### Example: Adding Support for a Custom Framework

```kotlin
// Your custom annotation
package com.mycompany.annotations

annotation class ApiDoc(val text: String)

// Usage in your models
@ApiDoc(text = "Customer profile information")
data class Customer(
    @ApiDoc(text = "Unique customer identifier")
    val id: Long,
    val name: String
)
```

Update `kotlinx-schema.properties`:

```properties
introspector.annotations.description.names=Description,ApiDoc
introspector.annotations.description.attributes=value,description,text
```

Now the schema generator will recognize `@ApiDoc` and extract descriptions from its `text` parameter.

### Example: Reusing Jackson Annotations

If your project already uses Jackson for JSON serialization, you can generate schemas from existing Jackson-annotated
classes without any modifications. This is particularly useful for REST APIs and Spring Boot applications where Jackson
annotations are already present.

<!--- CLEAR -->
<!--- INCLUDE
import kotlinx.schema.generator.json.ReflectionClassJsonSchemaGenerator

annotation class JsonClassDescription(val value: String)
annotation class JsonPropertyDescription(val value: String)
-->

```kotlin
// Existing code with Jackson annotations - NO CHANGES NEEDED!
@JsonClassDescription("Customer profile data")
data class Customer(
    @JsonPropertyDescription("Unique customer ID")
    val id: Long,

    @JsonPropertyDescription("Full name")
    val name: String,

    @JsonPropertyDescription("Contact email")
    val email: String
)

// Generate JSON schema without modifying the code
val generator = kotlinx.schema.generator.json.ReflectionClassJsonSchemaGenerator.Default
val schema = generator.generateSchema(Customer::class)

// Schema includes all Jackson descriptions!
```

<!--- KNIT example-knit-readme-16.kt -->

### Example: LangChain4j Integration

LangChain4j uses the `@P` annotation for parameter descriptions in AI function calling. The library recognizes these
annotations automatically, enabling seamless integration with existing LangChain4j codebases.

<!--- CLEAR -->
<!--- INCLUDE
import kotlinx.schema.generator.json.ReflectionFunctionCallingSchemaGenerator

annotation class P(val value: String)
-->

```kotlin
// Code using LangChain4j annotations
data class SearchQuery(
    @P("Search terms")
    val query: String,

    @P("Maximum results to return")
    val limit: Int = 10
)

// Generate schema for LLM function calling
val generator = ReflectionFunctionCallingSchemaGenerator.Default
val schema = generator.generateSchema(SearchQuery::class.constructors.first())
```

<!--- KNIT example-knit-readme-17.kt -->

### Example: Koog AI Agents

Koog AI framework uses `@LLMDescription` for documenting agent tools and parameters. The library supports both the
verbose `description =` syntax and the shorthand form, making migration from Koog straightforward.

<!--- CLEAR -->
<!--- INCLUDE
import kotlinx.schema.Schema

annotation class LLMDescription(val value: String = "", val description: String = "")
-->

```kotlin
@LLMDescription(description = "Product with pricing information")
@Schema
data class Product(
    @LLMDescription(description = "Product identifier")
    val id: Long,

    @LLMDescription("Product name")
    val name: String,

    @LLMDescription("Unit price")
    val price: Double,
)
```

<!--- KNIT example-knit-readme-18.kt -->

### Precedence Rules

If multiple description annotations are present on the same element, the library uses this precedence order:

1. `@Description` (kotlinx-schema's own annotation)
2. Other annotations in alphabetical order by simple name

**Tip**: For best compatibility, prefer `@Description` from kotlinx-schema when writing new code, but existing
annotations from other libraries work seamlessly.

## JSON Schema DSL

For manual schema construction, use the [**kotlinx-schema-json**](kotlinx-schema-json) module.
It provides type-safe Kotlin models compliant with
the [JSON Schema Draft 2020-12](https://json-schema.org/draft/2020-12/schema) specification
and a DSL for building JSON Schema definitions programmatically, with full kotlinx-serialization support.

```kotlin
dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-schema-json:<version>")
}
```

**Quick Example:**

<!--- CLEAR -->
<!--- INCLUDE
import kotlinx.schema.json.jsonSchema
-->

```kotlin
val schema = jsonSchema {
    property("id") {
        required = true
        string { format = "uuid" }
    }
    property("email") {
        required = true
        string { format = "email" }
    }
    property("age") {
        integer {
            minimum = 0.0
            maximum = 150.0
        }
    }
    // Polymorphic types with discriminators
    property("role") {
        oneOf {
            discriminator(propertyName = "type") {
                "admin" mappedTo "#/definitions/AdminRole"
                "user" mappedTo {
                    property("type") { string { constValue = "user" } }
                    property("permissions") { array { ofString() } }
                }
            }
        }
    }
}
```

<!--- KNIT example-knit-readme-19.kt -->

**Features:**

- ✅ Type-safe property definitions (string, number, integer, boolean, array, object, reference)
- ✅ **Polymorphism**: oneOf, anyOf, allOf with elegant discriminator support
- ✅ **Type-safe enums**: Native Kotlin types (List<String>, List<Number>, etc.) instead of JsonElement
- ✅ Constraints: required, nullable, enum, const, min/max, format validation
- ✅ Nested schemas and arrays of complex types
- ✅ Full kotlinx-serialization integration
- ✅ Kotlin Multiplatform support

**📖 For comprehensive documentation, see [kotlinx-schema-json/README.md](kotlinx-schema-json/README.md)** covering:

- Complete DSL reference and type-safe enum API
- Polymorphism patterns (oneOf, anyOf, allOf) with discriminators
- Generic properties with heterogeneous enums
- Working with nested objects and arrays
- Serialization/deserialization examples
- Function calling schema for LLM APIs

## Building and Contributing

For build instructions, development setup, and contribution guidelines, see [CONTRIBUTING.md](CONTRIBUTING.md).

## Requirements

- Kotlin 2.2+
- KSP 2 (applied automatically when using the Gradle plugin)
- _kotlinx-serialization-json_ for JsonObject support

Tip: If you use `@Description` on primary constructor parameters, enable
`-Xannotation-default-target=param-property` in Kotlin compiler options so the description applies to the backing
property.

## Code of Conduct

This project and the corresponding community are governed by
the [JetBrains Open Source and Community Code of Conduct](https://github.com/jetbrains#code-of-conduct). Please make
sure you read and adhere to it.

## License

This project is licensed under the Apache License 2.0 - see the [LICENSE](LICENSE) file for details.
