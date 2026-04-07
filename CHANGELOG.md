## 0.5.0
> Published 2026-04-07

### Added
- **Open polymorphism support**: abstract classes and interfaces generate JSON Schema when subtypes are registered via          
  `SerializersModule`; deterministic `oneOf`/`anyOf` output with descriptive errors for missing registrations
- **`@SerialDescription` annotation**: `@SerialInfo`-compatible description annotation read automatically by the
  serialization-based generator — no custom `DescriptionExtractor` needed (#303, #304)
- **`@SchemaIgnore` / `@SerialSchemaIgnore`**: exclude sealed subtypes from schema generation; Jackson `@JsonIgnoreType`      
  recognized by default; KSP fails fast on contradictory `@Schema` + `@SchemaIgnore` (#277)
- **`@SerialName` support in reflection and KSP**: class names, property names, enum entries, sealed subtypes, and            
  `$id`/`$defs`/`$ref` now honor `@SerialName` via FQN-aware annotation matching (#204, #308)

### Fixed
- **Recursive types**: schema generation correctly handles recursive sealed hierarchies and self-referencing types; subtypes  
  routed through cycle-safe path with idempotent discriminator injection (#307)
- **Inline value class schemas**: generation now matches kotlinx-serialization format; descriptions propagated to flattened
  primitive schema with correct property-level override precedence; TypeRef caching added

### Dependencies
- Bump `kotest` from 6.1.7 to 6.1.11
- Bump `dokka-gradle-plugin` from 2.1.0 to 2.2.0
- Bump `kotlinx.kover` from 0.9.7 to 0.9.8
- Bump `gradle-wrapper` from 9.4.0 to 9.4.1
- Bump `mcp` from 0.9.0 to 0.11.0 (examples)
- Bump `ai.koog:agents-tools` from 0.6.4 to 0.7.3 (examples)

## 0.4.4
> Published 2026-03-18

### Fixed

* fix: returning empty description for multi-element annotations (#269)
* fix: description is not extracted from non-public annotation interfaces (#269)

---

## 0.4.3
> Published 2026-03-17

### Fixed
- **Java record class support**: reflection schema generator now correctly introspects Java `record` types (#263)
- **Nullable required constructor parameters**: default value extraction no longer silently skips nullable required
  parameters when building mock constructor arguments, restoring correct defaults detection for classes with
  patterns such as `data class Foo(val tag: String? = "abc", val count: Int? = 5)` (#263)
- **Private constructor filtering**: fallback constructor lookup now skips `private` constructors, preventing
  accidental invocation of synthetic or internal-only constructors (#263)

### Dependencies
- Bump `kotest` from 6.1.6 to 6.1.7

---

## 0.4.2
> Published 2026-03-13

### Fixed
- **Missing descriptions for inline primitive, list, and map nodes**: `node.description` is now correctly propagated
  for inline `PrimitiveNode`, `ListNode`, and `MapNode` schemas (#251)

### Dependencies
- Bump `kotest` from 6.1.5 to 6.1.6

---

## 0.4.1
> Published 2026-03-10

### Fixed
- **Missing property descriptions for class types**: class-level `@Description` annotations are now correctly propagated
  to nested object schemas (#238)

### Documentation
- Improved `Module.md` annotation descriptions and updated Dokka links (#241)

---

## 0.4.0
> Published 2026-03-09

### Breaking Changes
- **`$defs` for all named types**: all named types now always register in `$defs` and use `$ref` at every call site;
  previously only nullable named types were emitted as `$ref` (#194)
- **`kotlin.Any` maps to `{}`**: the previous output `{"type":"object","additionalProperties":false}` was incorrect;
  the unconstrained schema `{}` is now used per JSON Schema Draft 2020-12 (#194)
- **Polymorphic discriminator on by default**: `includePolymorphicDiscriminator` defaults to `true` — sealed subtype
  schemas now include a `const` discriminator property unless explicitly disabled (#212)
- **Fully-qualified subclass names**: sealed subclass names in `$defs`, `$ref`, and discriminator `const` values now
  use fully-qualified names (e.g. `com.example.Animal.Cat`) instead of simple names (#212)
- **`JsonSchemaConfig.Strict` and `FunctionCallingSchemaConfig.Strict`**: `respectDefaultPresence` set to `false` —
  all fields are required regardless of Kotlin default values (#212)

### Added
- **Polymorphic types**: sealed class hierarchies generate `oneOf` (JSON Schema) or `anyOf` (function calling) with a
  `const` discriminator per subtype, supported across KSP, reflection, and serialization backends (#212)
- **KSP class/function filtering**: `include` and `exclude` processor options accept glob patterns to filter which
  classes and functions receive generated extensions (#181)
- **Constructor parameter annotations for descriptions**: reflection generator now also searches constructor parameter
  annotations when resolving property descriptions (#203)
- **`suspend fun` support**: `ReflectionFunctionIntrospector` now correctly introspects suspendable functions (#212)

### Changed
- `ReflectionClassIntrospectionContext` and `ReflectionFunctionIntrospectionContext` merged into a single
  `ReflectionIntrospectionContext`, eliminating duplication (#212)
- `TDecl` type parameter removed from `BaseIntrospectionContext` (#212)

### Dependencies
- Bump `kotest` from 6.1.3 to 6.1.5
- Bump `ai.koog:agents-tools` from 0.6.2 to 0.6.4
- Bump `dev.langchain4j:langchain4j-core` from 1.11.0 to 1.12.2
- Bump `gradle` wrapper from 9.3.1 to 9.4.0

---

## 0.3.2
> Published 2026-02-20

### Added
- **Custom description extraction**: `SerializationClassJsonSchemaGenerator` now accepts `SerializationClassSchemaIntrospector.Config`
  with a pluggable `DescriptionExtractor` — map any annotation to the schema `description` field without modifying your models (#196)

### Documentation
- New guide: [Serialization-Based Schema Generation](docs/serializable.md) covering setup, configuration, and polymorphic types (#197)
- Knit integration: README and guide code examples are now compiled and verified (#187)

### Dependencies
- Bump `ksp` from 2.3.5 to 2.3.6
- Bump `io.github.oshai:kotlin-logging` from 7.0.14 to 8.0.01

## 0.3.1
> Published 2026-02-12

### Added
- Support WasmJS/Browser and watchOS X64 targets

### Fixed
- Move slf4j.simple dependency to test scope

## 0.3.0
> Published 2026-02-03

### Breaking Changes
- **Multiplatform migration**: `kotlinx-schema-generator-core` and `kotlinx-schema-generator-json` are now Kotlin Multiplatform
  - Affects internal introspection APIs and test structure
  - Reflection-based generators remain JVM-only; serialization-based generators now multiplatform

### Added
- **KDoc support**: Parameter, field, and property descriptions extracted from KDoc comments (#109, #148)
  - Works with KSP processor for compile-time generation
  - Complements `@Description` annotations

### Changed
- **Documentation**: Updated documentation and `Module.md` files
- **Test migration**: Moved tests to `commonTest` for multiplatform compatibility

### Fixed
- **Package structure**: Moved `TypeGraphToJsonObjectSchemaTransformer` to `kotlinx.schema.json`

## 0.2.0
> Published 2026-02-02

### Breaking Changes
- **JsonSchema: `additionalProperties` API**: Replaced `JsonPrimitive` with type-safe `AdditionalPropertiesConstraint` sealed interface
  - Use `AllowAdditionalProperties`, `DenyAdditionalProperties`, or `AdditionalPropertiesSchema(schema)` instead of boolean primitives
  - Enables compile-time type safety and better IDE support

### Added
- **kotlinx.serialization support**: New `SerializationClassJsonSchemaGenerator` for runtime introspection
  - Generate schemas from `SerialDescriptor` without KSP or reflection
  - Support for primitives, enums, objects, lists, maps, and polymorphic types
  - _**NB! Type/field descriptions are not supported due to limitations of kotlinx.serialization model!**_
- **Extended type-safe Schema DSL**
- **Internal API markers**: `@InternalSchemaGeneratorApi` annotation for APIs subject to change
- **Documentation**: Architecture pipeline overview with + diagrams

### Changed
- **Serializers refactoring**: Consolidated six enum serializers into generic `TypedEnumSerializer`
  - Moved serializers to a dedicated package for better organization
  - Simplified `StringOrListSerializer` and `AdditionalPropertiesSerializer`
- **Introspection architecture**: Extracted shared state management into `BaseIntrospectionContext<TDecl, TType>`
  - Eliminates code duplication across Reflection, KSP, and Serialization backends
  - Unified cycle detection and type caching

### Fixed
- Complex object parameters in function calling schemas are now handled correctly
  - Extracted shared type handlers (`handleAnyFallback`, `handleSealedClass`, `handleEnum`, `handleObjectOrClass`)
  - Improved error messages for unhandled KSType cases

### Dependencies
- Bump `kotlinx.kover` from 0.9.4 to 0.9.5
- Bump `gradle` from 9.3.0 to 9.3.1


## 0.1.0
> Published 2026-02-02

**Note**: Duplicate entry - see version below for actual release notes.


## 0.1.0
> Published 2026-01-30

### Breaking Changes
- Flattened `JsonSchema` structure - removed nested `JsonSchemaDefinition` wrapper
- Changed nullable representation from `"nullable": true` to `["type", "null"]` (JSON Schema 2020-12)
- Removed `strictSchemaFlag` configuration option
- Changed discriminator fields from `default` to `const` in sealed classes
- Reordered `JsonSchema` constructor parameters (`schema` before `id`)

### Added
- `useUnionTypes`, `useNullableField`, `includeDiscriminator` configuration flags
- `JsonSchemaConfig.Default`, `JsonSchemaConfig.Strict`, `JsonSchemaConfig.OpenAPI` presets
- Support for enum and primitive root schemas
- Centralized `formatSchemaId()` method for ID generation
- `JsonSchemaConstants` for reduced object allocation

### Changed
- Schemas now generate as flat JSON Schema Draft 2020-12 compliant output
- Updated to `ksp-maven-plugin` v0.3.0
- Enhanced KSP documentation with Gradle and Maven examples

### Fixed
- Enum root schemas now generate correctly (previously generated empty objects)
- Local classes now use `simpleName` fallback instead of failing

### Dependencies
- Bump `ai.koog:agents-tools` from 0.6.0 to 0.6.1
- Bump `com.google.devtools.ksp` from 2.3.4 to 2.3.5 (examples)

