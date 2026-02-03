package kotlinx.schema.generator.core

/**
 * Marks internal APIs related to the schema generators as requiring opt-in.
 *
 * APIs annotated with this marker are considered internal to the schema generation logic
 * and are subject to change without prior notice. These APIs are not intended for public use
 * and may introduce breaking changes in future updates.
 *
 * It is recommended to only use this API internally or for experimental purposes
 * and not to rely on its stability for production code.
 */
@RequiresOptIn("This API is internal and may change without notice")
public annotation class InternalSchemaGeneratorApi
