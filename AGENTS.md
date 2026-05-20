# AGENTS.md

This file provides guidance to coding agents when working with code in this repository.

## Project

Kotlin library that generates JSON Schema (Draft 2020-12) and LLM function-calling schemas from Kotlin code. Three generation modes — KSP (compile-time, multiplatform), reflection (JVM runtime), and kotlinx.serialization `SerialDescriptor` (multiplatform runtime) — all funnel through a shared IR (`TypeGraph`).

## Build Commands

```bash
./gradlew build                          # Full build + tests (matches CI)
./gradlew checkKotlinAbi                 # ABI check — fails if api/*.api is stale
./gradlew updateKotlinAbi                # Regenerate ABI dump after public API changes; commit the diff
./gradlew :docs:knitCheck --no-configuration-cache   # Validate README/docs snippets
```

> README snippets in `<!--- INCLUDE -->` / `<!--- KNIT example-knit-readme-NN.kt -->` blocks are extracted to `.kt` files under `docs/` and compiled by knit — if you touch them, knitCheck will fail until you regenerate.

Single test (prefer `jvmTest` on KMP modules):
```bash
./gradlew :<module>:jvmTest --tests "FQN.TestClass.test name with backticks"
./gradlew :ksp-integration-tests:test    # KSP end-to-end (no Gradle plugin)
./gradlew :ksp-integration-tests:build   # trigger KSP codegen → build/generated/ksp/
```

Gradle plugin integration tests (separate composite build via `includeBuild()`, only sees the plugin after publication):
```bash
./gradlew publishPluginAndSync           # publish plugin to build/project-repo; reload IDE after
./gradlew testGradlePlugin               # publish + run :gradle-plugin-integration-tests:allTests
```

## Module Structure

```
kotlinx-schema-annotations         Runtime annotations (@Schema, @Description, @SchemaIgnore)
kotlinx-schema-json                JSON Schema Draft 2020-12 type-safe models + DSL (standalone)
kotlinx-schema-generator-core      IR (TypeGraph) + introspector/transformer interfaces
kotlinx-schema-generator-json      TypeGraph → JsonSchema / FunctionCallingSchema; reflection + serialization generators
kotlinx-schema-ksp                 KSP processor; uses generator-json
kotlinx-schema-ksp-gradle-plugin   Applies KSP, wires source sets
ksp-integration-tests              KSP end-to-end tests (no Gradle plugin)
gradle-plugin-integration-tests    Composite build that exercises the plugin as external users would
```

## Architecture

```
Sources ─┬─► KspSchemaIntrospector (compile-time)         ─┐
         ├─► ReflectionSchemaIntrospector (JVM runtime)    ├─► TypeGraph ─┬─► JsonSchemaTransformer       ─► JsonObject / String
         └─► SerializationClassSchemaIntrospector (KMP)    ─┘             └─► FunctionCallingTransformer  ─► FunctionCallingSchema
```

`TypeGraph` (`kotlinx-schema-generator-core/.../ir/TypeGraph.kt`) is the single source of truth. `TypeNode` is sealed (`PrimitiveNode`, `EnumNode`, `ObjectNode`, `ListNode`, `MapNode`, `AnyNode`, `PolymorphicNode`); named types live in `$defs` via `TypeId` and are referenced through `TypeRef.Ref`.

**Module boundaries are load-bearing:**
- Introspectors gather model metadata only — no JSON specifics.
- IR is framework-agnostic — no KSP types, no `JsonObject`.
- Emitters/transformers convert IR to output — no KSP / `SerialDescriptor` leakage.

KSP-side: `SchemaExtensionProcessor` is the entry point; per-function-location strategies under `functions/` (`TopLevelFunctionStrategy`, `InstanceFunctionStrategy`, `CompanionFunctionStrategy`, `ObjectFunctionStrategy`) decide whether the generated accessor is top-level, a `KClass<T>` extension, or a `KClass<T.Companion>`/`KClass<Object>` extension. Generated sources land in `<module>/build/generated/ksp/...` (MPP commonMain: `build/generated/ksp/metadata/commonMain/kotlin`).

## Schema Emission Rules

- Nullable primitive → `type: ["<type>", "null"]`. Nullable ref → `oneOf: [{ "$ref": ... }, { "type": "null" }]`.
- Named types keyed by FQ class name in `$defs`; referenced via `$ref` everywhere (avoids cross-package collisions).
- Sealed/open polymorphism → `oneOf` of `$ref`s + discriminator `type: { const: FQN }`.
- `kotlin.Any` / unbound type params → `AnyNode` → `{}`.
- `@SchemaIgnore` (KSP/reflection), `@SerialSchemaIgnore` (serialization), Jackson `@JsonIgnoreType` — all skip sealed subtypes.
- Description annotations matched **by simple name, case-insensitively** (or by FQN, case-sensitively, if the configured name contains a dot). Defaults: `Description`, `LLMDescription`, `JsonPropertyDescription`, `JsonClassDescription`, `P`. Override via `kotlinx-schema.properties` on the classpath.
- Function-calling schemas: all parameters always `required` (OpenAI strict mode); nullable params use union `["string", "null"]`, never `nullable: true`.
- KSP cannot extract function/property default *values* — `Property.hasDefaultValue` is tracked, `defaultValue` is null. Reflection populates the actual value.

## Code Conventions

- `allWarningsAsErrors = true` + `extraWarnings = true` on KMP modules (`buildSrc/.../kotlin-multiplatform-convention.gradle.kts`) — compiler warnings break the build.
- **Multi-dollar raw strings**: use `$$"""…"""` (or `${'$'}id` inline) for JSON Schema literals containing `$id`, `$ref`, `$defs`, `$schema`. Kotlin interpolation otherwise consumes them.
- `@Description` on primary constructor params requires `-Xannotation-default-target=param-property`.
- `api/<module>.api` is the public ABI snapshot — run `./gradlew updateKotlinAbi` after API changes and commit the diff.
- Each module needs `Module.md` for Dokka (starts with `# Module <module-name>`; package docs `# Package <package.name>`; subsections level 2+).

## Testing

- Parameterized tests preferred for 3+ variations: `@CsvSource` for scalars, `@MethodSource` for complex types (annotate the class `@TestInstance(TestInstance.Lifecycle.PER_CLASS)` so providers are instance methods — no `companion object`/`@JvmStatic`).
- Frameworks: kotlin-test, Kotest assertions (infix `shouldBe`, `shouldNotBeNull { … }`, `assertSoftly(subject) { … }`), MockK.
- JSON/schema comparisons: `schema shouldEqualJson """…""".trimIndent()` — structural, not whitespace-based. Annotate JSON literals with `// language=json`.
- For generators, verify both `KClass<T>.jsonSchemaString` and `KClass<T>.jsonSchema` where applicable; confirm non-annotated classes do **not** gain generated extensions.
