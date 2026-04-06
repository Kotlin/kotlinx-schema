package kotlinx.schema

import kotlin.annotation.AnnotationRetention.RUNTIME
import kotlin.annotation.AnnotationTarget.CLASS

/**
 * Excludes the annotated class from schema generation.
 *
 * When applied to a sealed subtype, the subtype is omitted from the parent's
 * polymorphic `oneOf` schema. The class remains fully functional at runtime —
 * only schema generation is affected.
 *
 * All three generators (KSP, reflection, serialization) recognize this annotation.
 * For serialization-based generation where annotations must carry
 * [@SerialInfo][kotlinx.serialization.SerialInfo], use
 * `@SerialSchemaIgnore` from the `kotlinx-schema-generator-json` module instead.
 *
 * Example:
 * ```kotlin
 * @Schema
 * sealed class Event {
 *     data class Click(val x: Int, val y: Int) : Event()
 *
 *     @SchemaIgnore
 *     data class Internal(val trace: String) : Event()
 * }
 * ```
 *
 * @see kotlinx.schema.Schema
 */
@Target(CLASS)
@Retention(RUNTIME)
@MustBeDocumented
public annotation class SchemaIgnore
