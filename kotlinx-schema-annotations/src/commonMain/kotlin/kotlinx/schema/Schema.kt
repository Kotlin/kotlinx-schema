package kotlinx.schema

import kotlin.annotation.AnnotationRetention.SOURCE
import kotlin.annotation.AnnotationTarget.CLASS
import kotlin.annotation.AnnotationTarget.FUNCTION

/**
 * Annotation to define a schema for a class, type, or function.
 *
 * This annotation is primarily used to associate a specific schema type with the annotated element.
 * By default, it targets JSON Schema but can accommodate custom schema representations as well.
 *
 * When applied to functions, generates FunctionCallingSchema for LLM function calling APIs.
 *
 * @property value The schema type. Defaults to "json" for JSON Schema.
 * @property withSchemaObject Indicates whether to generate a specific representation for the schema,
 * such as a `JsonSchema`/`JsonObject` for JSON Schema. This may require additional dependencies.
 */
@Target(CLASS, FUNCTION)
@Retention(SOURCE)
@MustBeDocumented
public annotation class Schema(
    /**
     * Schema Type
     *
     * Default value is "json" for JSON Schema
     */
    val value: String = "json",
    /**
     * Generate specific representation, e.g. JsonSchema for JSON Schema.
     *
     * It might require an additional compile-time dependency, e.g. Kotlin Serialization
     */
    val withSchemaObject: Boolean = false,
)
