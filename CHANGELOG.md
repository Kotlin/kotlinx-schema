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

