# Module kotlinx-schema-generator-core

Core abstractions and intermediate representation (IR) for schema generation.

Provides the foundational architecture unifying KSP, Reflection, and Serialization introspection strategies through a common [TypeGraph] IR.

**Platform Support:** Multiplatform IR models (Common) • JVM reflection introspection • Kotlin 2.2+

## Key Components

- [TypeGraph] - intermediate representation capturing type metadata, hierarchies, and annotations
- [SchemaGenerator] - abstract interface for implementing custom generators
- [SchemaIntrospector] - pluggable introspection layer for analyzing types
- [TypeGraphTransformer] - converts IR to concrete schema formats
- [Config] - configuration for annotation recognition

## Architecture

Three introspection strategies converge on [TypeGraph]:

1. **Compile-time (KSP)**: Symbol processor → TypeGraph → generated code
2. **Runtime (Reflection)**: KClass analysis → TypeGraph → runtime schema
3. **Runtime (Serialization)**: SerialDescriptor → TypeGraph → runtime schema

The unified [TypeGraph] feeds transformers that produce [JsonSchema], [FunctionCallingSchema], etc.

## Extending

Implement custom schema formats by:
1. Creating a [TypeGraphTransformer] implementation
2. Extending [AbstractSchemaGenerator] with your transformer
3. Using existing introspectors or implementing [SchemaIntrospector]

# Package kotlinx.schema.generator.core

Core schema generator abstractions and configuration.

# Package kotlinx.schema.generator.core.ir

Intermediate representation (IR) models and transformers.

# Package kotlinx.schema.generator.reflect

Reflection-based introspection for JVM runtime analysis.
