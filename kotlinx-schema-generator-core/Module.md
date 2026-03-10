# Module kotlinx-schema-generator-core

Core abstractions and intermediate representation (IR) for schema generation.

Provides the foundational architecture unifying KSP, Reflection, and Serialization introspection strategies 
through a common [TypeGraph][kotlinx.schema.generator.core.ir.TypeGraph] IR.

**Platform Support:** Multiplatform IR models (Common) • JVM reflection introspection • Kotlin 2.2+

## Key Components

- [TypeGraph][kotlinx.schema.generator.core.ir.TypeGraph] - intermediate representation capturing type metadata, hierarchies, and annotations
  [SchemaGenerator][kotlinx.schema.generator.core.SchemaGenerator] - abstract interface for implementing custom generators
- [SchemaIntrospector][kotlinx.schema.generator.core.ir.SchemaIntrospector] - pluggable introspection layer for analyzing types
- [TypeGraphTransformer][kotlinx.schema.generator.core.ir.TypeGraphTransformer] - converts IR to concrete schema formats
- [Config][kotlinx.schema.generator.core.Config] - configuration for annotation recognition

## Architecture

Three introspection strategies converge on [TypeGraph][kotlinx.schema.generator.core.ir.TypeGraph]:

1. **Compile-time (KSP)**: Symbol processor → [TypeGraph][kotlinx.schema.generator.core.ir.TypeGraph] → generated code
2. **Runtime (Reflection)**: [KClass][kotlin.reflect.KClass] analysis → [TypeGraph][kotlinx.schema.generator.core.ir.TypeGraph] → runtime schema
3. **Runtime (Serialization)**: [SerialDescriptor][kotlinx.serialization.descriptors.SerialDescriptor] → [TypeGraph][kotlinx.schema.generator.core.ir.TypeGraph] → runtime schema

The unified [TypeGraph][kotlinx.schema.generator.core.ir.TypeGraph] feeds transformers that produce 
[JsonSchema][kotlinx.schema.json.JsonSchema], [FunctionCallingSchema][kotlinx.schema.json.FunctionCallingSchema], etc.

## Extending

Implement custom schema formats by:
1. Creating a [TypeGraphTransformer][kotlinx.schema.generator.core.ir.TypeGraphTransformer] implementation
2. Extending [AbstractSchemaGenerator][kotlinx.schema.generator.core.AbstractSchemaGenerator] with your transformer
3. Using existing introspectors or implementing [SchemaIntrospector][kotlinx.schema.generator.core.ir.SchemaIntrospector]

# Package kotlinx.schema.generator.core

Core schema generator abstractions and configuration.

# Package kotlinx.schema.generator.core.ir

Intermediate representation (IR) models and transformers.

# Package kotlinx.schema.generator.reflect

Reflection-based introspection for JVM runtime analysis.
