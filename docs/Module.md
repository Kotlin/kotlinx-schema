# Module Kotlinx-Schema

The `Kotlinx-Schema` library provides a powerful, multi-platform framework for generating JSON schemas and LLM function calling schemas directly from Kotlin classes and functions.

**Platform Support:** Multiplatform (Common, JVM, JS, Native, Wasm) • Kotlin 2.2+

## Key Features

- **Compile-time Generation (KSP)**: Zero runtime overhead for your annotated classes.
- **Runtime Generation (Reflection)**: JVM-only support for any class, including those from third-party libraries.
- **Runtime Generation (Serialization)**: Schema generation based on `SerialDescriptor` for `@Serializable` classes.
- **LLM Function Calling**: First-class support for OpenAI and Anthropic function calling schema formats.
- **Multi-Framework Support**: Recognizes annotations from Jackson, LangChain4j, and more.
