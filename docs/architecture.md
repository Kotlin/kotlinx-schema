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
- **Third-party support**: Generate schemas for types you don’t own, without editing their source.

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

