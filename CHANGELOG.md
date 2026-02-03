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
- Corrected schema examples in documentation to match actual test output


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

