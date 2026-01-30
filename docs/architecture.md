# Architecture

`kotlinx-schema` is a layered library that generates schemas—primarily JSON Schema—from Kotlin declarations
and Java classes. It unifies compile-time analysis (KSP) and runtime inspection (reflection) by translating
both into a shared Internal Representation (IR).

The architecture is modular by design, so you can plug in new schema formats and generation strategies while
keeping behavior consistent across JVM, JS, Native, and Wasm.

**Architecture goals:**

- **Unified IR**: Separate schema sources (KSP or reflection) from output targets (JSON Schema, LLM function calling).
- **Multiplatform (KSP)**: Support schema generation across all Kotlin targets.
- **Extensibility**: Enable third-party annotations and custom transformations.
- **Zero runtime overhead (KSP)**: Prefer compile-time generation for performance-sensitive paths.
- **Third-party support**: Generate schemas for types you don't own, without editing their source.

## Overview

The library follows a three-stage pipeline: **Sources** → **Introspectors** → **TypeGraph** → **Transformers** → **Schemas**

```mermaid
graph LR
    subgraph Sources["📦 SOURCES"]
        Kotlin["Kotlin Classes<br/>@Schema annotated"]
        Java["Java Classes<br/>Third-party libs"]
        Functions["Kotlin Functions<br/>LLM integration"]
    end

    subgraph Introspectors["🔍 INTROSPECTORS"]
        KSP["KspSchemaIntrospector<br/><i>compile-time</i>"]
        Reflect["ReflectionSchemaIntrospector<br/><i>runtime</i>"]
    end

    subgraph IR["🧬 INTERNAL REPRESENTATION"]
        TypeGraph["TypeGraph<br/>Unified intermediate format<br/>Properties • Types • Descriptions<br/>Nullable • Defaults • Generics"]
    end

    subgraph Transformers["⚙️ TRANSFORMERS"]
        JsonTransform["JsonSchemaTransformer<br/>Draft 2020-12"]
        FuncTransform["FunctionCallingTransformer<br/>OpenAI/Anthropic format"]
    end

    subgraph Output["📄 SCHEMA OUTPUTS"]
        KtGen["Generated .kt<br/>Extension properties<br/>MyClass::class.jsonSchema"]
        JsonObj["JsonObject<br/>kotlinx.serialization"]
        JsonStr["JSON String<br/>Serialized schema"]
    end

    Kotlin --> KSP
    Kotlin --> Reflect
    Java --> Reflect
    Functions --> KSP
    Functions --> Reflect

    KSP --> TypeGraph
    Reflect --> TypeGraph

    TypeGraph --> JsonTransform
    TypeGraph --> FuncTransform

    JsonTransform --> KtGen
    JsonTransform --> JsonObj
    JsonTransform --> JsonStr
    FuncTransform --> JsonObj
    FuncTransform --> JsonStr

    classDef sourceStyle fill:#e3f2fd,stroke:#1565c0,stroke-width:3px,color:#000
    classDef introspectorStyle fill:#fff3e0,stroke:#ef6c00,stroke-width:3px,color:#000
    classDef irStyle fill:#fce4ec,stroke:#c2185b,stroke-width:4px,color:#000
    classDef transformStyle fill:#f3e5f5,stroke:#7b1fa2,stroke-width:3px,color:#000
    classDef outputStyle fill:#e8f5e9,stroke:#2e7d32,stroke-width:3px,color:#000

    class Kotlin,Java,Functions sourceStyle
    class KSP,Reflect introspectorStyle
    class TypeGraph irStyle
    class JsonTransform,FuncTransform transformStyle
    class KtGen,JsonObj,JsonStr outputStyle
```

**The Transformation Story:**

1. **Sources** (blue) — Kotlin classes, Java classes, or Kotlin functions serve as input
2. **Introspectors** (orange) — Extract type information using KSP (compile-time) or Reflection (runtime)
3. **TypeGraph** (pink) — Unified internal representation containing all type metadata
4. **Transformers** (purple) — Convert TypeGraph to JSON Schema or Function Calling format
5. **Outputs** (green) — Generated Kotlin code, JsonObject, or JSON strings

## Module Dependencies

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
- **kotlinx-schema-generator-core** — internal representation (IR) for schema descriptions, introspection utils,
  generator interfaces
- **kotlinx-schema-generator-json** — JSON Schema transformer from the IR
- **kotlinx-schema-ksp** — KSP processor that scans your code and generates the extension properties:
    - `KClass<T>.jsonSchema: JsonObject`
    - `KClass<T>.jsonSchemaString: String`
- **kotlinx-schema-gradle-plugin** — Gradle plugin (id: "org.jetbrains.kotlinx.schema.ksp") that:
    - Applies KSP automatically
    - Adds the KSP processor dependency
    - Wires generated sources into your source sets
    - Sets up multiplatform task dependencies
- **gradle-plugin-integration-tests** — Independent build that includes the main project; demonstrates real MPP usage
  and integration testing
- **ksp-integration-tests** — KSP end‑to‑end tests for generation without the Gradle plugin

### Workflow

```mermaid
sequenceDiagram
    actor C as Client
    participant S as SchemaGeneratorService
    participant G as SchemaGenerator
    participant I as SchemaIntrospector
    participant T as TypeGraphTransformer
    note over T: has Config
    C ->> S: getGenerator(T::class, R::class)
    S -->> G: find
    activate G
    S -->> C: SchemaGenerator
    C ->> G: generate(T) : R?
    G ->> I: introspect(T)
    I -->> G: TypeGraph
    G ->> T: transform(TypeGraph, rootName)
    T -->> G: schema (R)
    G -->> C: schema (R)
    deactivate G
```

1. _Client_ (KSP Processor or Java class) calls _SchemaGeneratorService_ to lookup _SchemaGenerator_
   by target type T and expected schema class. _SchemaGeneratorService_ returns _SchemaGenerator_, if any.
2. _Client_ (KSP Processor or Java class) calls _SchemaGenerator_ to generate a Schema string representation,
   and, optionally, object a Schema string representation.
3. SchemaGenerator invokes SchemaIntrospector to convert an object into _TypeGraph_
4. _TypeGraphTransformer_ converts a _TypeGraph_ to a target representation (e.g., JSON Schema)
   with respect to respecting _Config_ object and returns it to SchemaGenerator

