[![JetBrains incubator project](https://jb.gg/badges/incubator.svg)](https://confluence.jetbrains.com/display/ALL/JetBrains+on+GitHub)
[![Kotlin CI with Gradle](https://github.com/Kotlin/kotlinx-schema/actions/workflows/gradle.yml/badge.svg?branch=main)](https://github.com/Kotlin/kotlinx-schema/actions/workflows/gradle.yml)
[![CodeQL](https://github.com/Kotlin/kotlinx-schema/actions/workflows/github-code-scanning/codeql/badge.svg)](https://github.com/Kotlin/kotlinx-schema/actions/workflows/github-code-scanning/codeql)

# kotlinx-schema

**Generate JSON Schemas and LLM function calling schemas from Kotlin code — including classes you don't own.**

> [!IMPORTANT]  
> The Library is **super experimental**. Some parts might eventually be part of [kotlinx-serialization](https://github.com/Kotlin/kotlinx.serialization).


## Why kotlinx-schema?

This library solves three key challenges:

1. **🤖 LLM Function Calling Integration**: Generate OpenAI/Anthropic-compatible function schemas directly from Kotlin functions with proper type definitions and descriptions
2. **📦 Third-Party Class Support**: Create schemas for library classes without modifying their source code (Spring entities, Ktor models, etc.)
3. **🔄 Multi-Framework Compatibility**: Works with existing annotations from Jackson, LangChain4j, Koog, and more — no code changes needed

### When to Use

* 🤖 Building LLM-powered applications with structured function calling (OpenAI, Anthropic, Claude, MCP)
* 👽 Need schemas for third-party library classes you cannot modify
* ✅ Already using `@Description`-like annotations from other frameworks
* 👌 Want zero runtime overhead with compile-time generation **(Multiplatform!!!)**
* ☕️ Need dynamic schema generation at runtime via reflection (JVM)

## Key Features

**Dual Generation Modes:**
- **Compile-time (KSP)**: Zero runtime overhead, multiplatform, for your annotated classes
- **Runtime (Reflection)**: JVM-only, for any class including third-party libraries

**LLM Integration:**
- First-class support for OpenAI/Anthropic function calling format
- Automatic strict mode and parameter validation
- Function name and description extraction

**Flexible Annotation Support:**
- Recognizes `@Description`, `@LLMDescription`, `@JsonPropertyDescription`, `@P`, and more
- Works with annotations from Jackson, LangChain4j, Koog without code changes

**Comprehensive Type Support:**
- Enums, collections, maps, nested objects, nullability, generics (with star-projection)
- Proper union types for nullable parameters (`["string", "null"]`)
- Type constraints (min/max, patterns, formats)

**Developer Experience:**
- Gradle plugin for one-line setup
- Type-safe Kotlin DSL for programmatic schema construction
- Works everywhere: JVM, JS, iOS, macOS, Wasm

## Quick start

Recommended: use the Gradle plugin.
It applies KSP for you, wires generated sources, and sets up task dependencies.

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
    iosArm64()
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
- For an example project using the plugin,
  see [gradle-plugin-integration-tests](plugins/gradle/gradle-plugin-integration-tests).

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
**Note!** for classes KDoc can also be used as a description.

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
                    "description": "Two-letter ISO country code; defaults to US"
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
    },
    "$ref": "#/$defs/com.example.Address"
}
```

- Enums are `type: string` with `enum: [...]` and carry `@Description` as `description`.
- Object properties include their inferred type schema and, when present, property-level `@Description` as`description`.
- Nullable properties are emitted as a union including `null`.
- Collections: `List<T>`/`Set<T>` → `{ "type":"array", "items": T }`; `Map<String, V>` →
  `{ "type":"object", "additionalProperties": V }`.
- Unknown/generic type parameters resolve to `kotlin.Any` with a minimal definition in `$defs`.

## Examples

### Basic data classes

Here's a practical example of a product model with various property types:

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

Use the generated extensions:

```kotlin
val schema = Product::class.jsonSchemaString
val schemaObject = Product::class.jsonSchema
```

<details>
<summary>Generated JSON schema</summary>

```json
{
    "$id": "com.example.Product",
    "$defs": {
        "com.example.Product": {
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
                    "description": "Whether the product is currently in stock"
                },
                "tags": {
                    "type": "array",
                    "items": {
                        "type": "string"
                    },
                    "description": "List of tags for categorization and search"
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
    },
    "$ref": "#/$defs/com.example.Product"
}
```

</details>

### Enums with descriptions

Enums are supported with descriptions on both the enum class and individual values:

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

<details>
<summary>Generated JSON schema</summary>

```json
{
    "$id": "com.example.Status",
    "$defs": {
        "com.example.Status": {
            "type": "string",
            "enum": [
                "ACTIVE",
                "INACTIVE",
                "PENDING"
            ],
            "description": "Current lifecycle status of an entity."
        }
    },
    "$ref": "#/$defs/com.example.Status"
}
```

</details>

### Nested objects

You can compose schemas by nesting annotated classes:

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

The generated schema for `Order` will automatically include definitions for all nested types (`Person`, `Address`,
`Product`, `Status`) in the `$defs` section, with appropriate `$ref` pointers to link them together. This makes it easy
to build complex, composable data models.

### Generic types

Generic classes are supported, with type parameters resolved at usage sites:

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

Generic type parameters are resolved at the usage site. When generating a schema for a generic class, unbound type
parameters (like `T`) are treated as `kotlin.Any` with a minimal definition in the `$defs` section. For more specific
typing, instantiate the generic class with concrete types when you need them.

## Using @Schema and @Description annotations

### @Schema annotation

Mark classes with `@Schema` to generate extension properties for them:

```kotlin
@Schema  // Uses default schema type "json"
data class Address(val street: String, val city: String)

@Schema("json")  // Explicitly specify schema type
data class Person(val name: String, val age: Int)
```

The `@Schema` annotation has an optional `value` parameter (defaults to `"json"`) that specifies the schema type.
Currently, only JSON Schema generation is supported, but this parameter allows for future extensibility to other schema
formats (e.g., OpenAPI, Avro).

**Note**: Both `jsonSchemaString: String` and `jsonSchema: JsonObject` extension properties are always generated for all
`@Schema`-annotated classes, regardless of annotation parameters.

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

## Runtime schema generation

For scenarios where compile-time generation isn't possible, use 
[`ReflectionClassJsonSchemaGenerator`](kotlinx-schema-generator-json/src/main/kotlin/kotlinx/schema/generator/json/ReflectionClassJsonSchemaGenerator.kt) 
and [`ReflectionFunctionCallingSchemaGenerator`](kotlinx-schema-generator-json/src/main/kotlin/kotlinx/schema/generator/json/ReflectionFunctionCallingSchemaGenerator.kt)
with Kotlin reflection (JVM only).

### Why Runtime Generation?

**Primary use case: Third-party library classes**

The compile-time (KSP) approach requires you to annotate classes with `@Schema`, which isn't possible for:
- Library classes (Spring entities, Ktor models, database classes)
- Framework-provided models
- Classes from dependencies you don't control

Runtime generation solves this by using reflection to analyze any class at runtime.

> [!IMPORTANT]
> **Limitation:** KDoc annotations are not available at runtime 

### Usage

```kotlin
// Works with ANY class, even from third-party libraries
import com.thirdparty.library.User  // Not your code!

val generator = kotlinx.schema.generator.json.ReflectionClassJsonSchemaGenerator.Default
val schema: JsonObject = generator.generateSchema(User::class)
val schemaString: String = generator.generateSchemaString(User::class)
```

**Add dependency**: `org.jetbrains.kotlinx:kotlinx-schema-generator-json:<version>`

### Choosing Your Approach

| Approach                 | Best For                               | Pros                                               | Cons                                |
|--------------------------|----------------------------------------|----------------------------------------------------|-------------------------------------|
| **Compile-time (KSP)**   | Your own annotated classes             | Zero runtime cost, multiplatform                   | Only works for classes you own      |
| **Runtime (Reflection)** | Third-party classes, dynamic scenarios | Works with any class, supports foreign annotations | JVM only, small reflection overhead |

**Decision guide**:
- ✅ Use **KSP** for your domain models in multiplatform projects
- ✅ Use **Reflection** for third-party library classes or when you need dynamic generation

## Function calling schema generation for LLMs

Modern LLMs (OpenAI GPT-4, Anthropic Claude, etc.) use structured function calling to interact with your code. 
They require a specific JSON schema format that describes available functions, their parameters, and types.

### Why This Format?

LLM APIs need to know:
- What functions are available and what they do
- Parameter names, types, and descriptions
- Which parameters are required
- Type constraints (enums, formats, ranges)

This library automatically generates schemas that comply with the [OpenAI function calling specification](https://platform.openai.com/docs/guides/function-calling), making it easy to expose Kotlin functions to LLMs.

### Basic Usage

```kotlin
@Description("Get current weather for a location")
fun getWeather(
    @Description("City and country, e.g. 'London, UK'")
    location: String,

    @Description("Temperature unit")
    unit: String = "celsius"
): WeatherInfo {
    // Implementation...
}

val generator = ReflectionFunctionCallingSchemaGenerator.Default
val schema = generator.generateSchema(::getWeather)
```

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
    "required": ["location", "unit"],
    "additionalProperties": false
  }
}
```

### Key Features

- **Automatic extraction**: Function name and descriptions from `@Description` annotations
- **Strict mode**: `strict: true` enables OpenAI's [strict mode](https://platform.openai.com/docs/guides/function-calling#strict-mode) for reliable parsing
- **Union types**: Nullable parameters use `["string", "null"]` instead of `nullable: true`
- **Required by default**: All parameters marked as required (OpenAI structured outputs requirement)
- **Type safety**: Proper JSON Schema types from Kotlin types (Int → integer, String → string, etc.)

### Working with Multiple Functions

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

The generated schemas can be sent to any LLM API that supports function calling (OpenAI, Anthropic, etc.). 
Integration with specific LLM providers requires their respective client libraries.

### Nullable Parameters

Nullable parameters are represented as union types:

```kotlin
@Description("Update user profile")
fun updateProfile(
    @Description("User ID") userId: String,
    @Description("New name, if changing") name: String? = null,
    @Description("New email, if changing") email: String? = null
): User = TODO("does not matter")// ...
```

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

For more details on function calling schemas and OpenAI compatibility, see [kotlinx-schema-json/README.md](kotlinx-schema-json/README.md#function-calling-schema-for-llm-apis).

## Multi-Framework Annotation Support

**You don't need to change your existing code!**

kotlinx-schema recognizes description annotations from multiple frameworks by their **simple name**, allowing you to generate schemas from code that uses annotations from other libraries.

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
val generator = ReflectionJsonSchemaGenerator.Default
val schema = generator.generateSchema(Customer::class)

// Schema includes all Jackson descriptions!
```

### Example: LangChain4j Integration

```kotlin
// Code using LangChain4j annotations
data class SearchQuery(
    @P("Search terms")
    val query: String,

    @P("Maximum results to return")
    val limit: Int = 10
)

// Generate schema for LLM function calling
val schema = ReflectionFunctionCallingSchemaGenerator.Default
    .generateSchema(SearchQuery::class)
```

### Example: Koog AI Agents

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

### Precedence Rules

If multiple description annotations are present on the same element, the library uses this precedence order:
1. `@Description` (kotlinx-schema's own annotation)
2. Other annotations in alphabetical order by simple name

**Tip**: For best compatibility, prefer `@Description` from kotlinx-schema when writing new code, but existing annotations from other libraries work seamlessly.

## JSON Schema DSL

For manual schema construction, use the [**kotlinx-schema-json**](kotlinx-schema-json) module. 
It provides type-safe Kotlin models and a DSL for building JSON Schema definitions programmatically, 
with full kotlinx-serialization support.

```kotlin
dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-schema-json:<version>")
}
```

Example:

```kotlin
val schema = jsonSchema {
    name = "User"
    schema {
        property("id") {
            required = true
            string { format = "uuid" }
        }
        property("email") {
            required = true
            string { format = "email" }
        }
        property("age") {
            integer { minimum = 0.0 }
        }
    }
}

// Serialize to JSON
val jsonString = Json.encodeToString(schema)
println(jsonString)
```
The result will be the following JSON Schema string:
```json
{
    "name": "User",
    "strict": false,
    "schema": {
        "type": "object",
        "properties": {
            "id": {
                "type": "string",
                "format": "uuid"
            },
            "email": {
                "type": "string",
                "format": "email"
            },
            "age": {
                "type": "integer",
                "minimum": 0.0
            }
        },
        "required": [
            "id",
            "email"
        ]
    }
}
```

See [kotlinx-schema-json/README.md](kotlinx-schema-json/README.md) for full documentation and examples.

## Project architecture

```mermaid
C4Context
    title kotlinx-schema

    Boundary(lib, "kotlinx-schema") {

        System(kxsGenCore, "kotlinx-schema-generator-core")
        System(kxsAnnotations, "kotlinx-schema-annotations")
        System(kxsGenJson, "kotlinx-schema-generator-json")
        System(kxsJsn, "kotlinx-schema-json")
        System(kxsKsp, "kotlinx-schema-ksp")

        System(kxsGradle, "kotlinx-schema-gradle-plugin")
    }


    Rel(kxsGenJson, kxsGenCore, "uses")
    Rel(kxsGenJson, kxsJsn, "uses")
    Rel(kxsGenCore, kxsAnnotations, "knows")
    Rel(kxsKsp, kxsGenJson, "uses")
    
    Rel(kxsGradle, kxsKsp, "uses")

    Boundary(userCode, "User's Application Code") {
        System_Ext(userModels, "User Domain Models")
        System_Ext(userModelsExt, "User Models Extensions")
        Rel(userModelsExt, userModels, "uses")
    }

    Rel(userModels, kxsAnnotations, "uses")
    Rel(kxsKsp, userModelsExt, "generates")

```

Top-level modules you might interact with:

- **kotlinx-schema-annotations** — runtime annotations: @Schema and @Description
- **kotlinx-schema-json** — type-safe models and DSL for building JSON Schema definitions programmatically
- **kotlinx-schema-generator-core** — internal representation (IR) for schema descriptions, introspection utils, generator interfaces
- **kotlinx-schema-generator-json** — JSON Schema transformer from the IR
- **kotlinx-schema-ksp** — KSP processor that scans your code and generates the extension properties:
    - `KClass<T>.jsonSchema: JsonObject`
    - `KClass<T>.jsonSchemaString: String`
- **plugins/gradle/kotlinx-schema-gradle-plugin** — Gradle plugin (id: "kotlinx.schema") that:
    - Applies KSP automatically
    - Adds the KSP processor dependency
    - Wires generated sources into your source sets
    - Sets up multiplatform task dependencies
- **plugins/gradle/gradle-plugin-integration-tests** — a real MPP sample used as integration tests, showing how users
  consume the Gradle plugin
- **ksp-integration-tests** — KSP end‑to‑end tests for generation without the Gradle plugin

### Workflow

```mermaid
sequenceDiagram
    actor C as Client
    participant S as SchemaGeneratorService
    participant G as SchemaGenerator
    participant I as SchemaIntrospector
    participant T as TypeGraphTransformer
    
    C->>S: getGenerator(T::class, R::class)
    S-->>G: find
    activate G
    S-->>C: SchemaGenerator
    C->>G: generate(T) : R?

    G->>I: introspect(T)
    I-->>G: TypeGraph

    G->>T: transform(graph = TypeGraph, rootName)
    T-->>G: schema (R)
    G-->>C: schema (R)
    deactivate G
```
1. _Client_ (KSP Processor or Java class) calls _SchemaGeneratorService_ to lookup _SchemaGenerator_ 
   by target type T and expected schema class. _SchemaGeneratorService_ returns _SchemaGenerator_, if any.
2. _Client_ (KSP Processor or Java class) calls _SchemaGenerator_ to generate a Schema string representation, 
and, optionally, object a Schema string representation.
3. SchemaGenerator invokes SchemaIntrospector to convert an object into _TypeGraph_
4. _TypeGraphTransformer_ converts a _TypeGraph_ to a target representation (e.g., JSON Schema)
   and returns it to SchemaGenerator

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
`-Xannotation-default-target=param-property` in Kotlin compiler options so the description applies to the backing
property.

## Contributing

Read the [Contributing Guidelines](CONTRIBUTING.md).

## Code of Conduct

This project and the corresponding community are governed by
the [JetBrains Open Source and Community Code of Conduct](https://github.com/jetbrains#code-of-conduct). Please make
sure you read and adhere to it.

## License

This project is licensed under the Apache License 2.0 - see the [LICENSE](LICENSE) file for details.
