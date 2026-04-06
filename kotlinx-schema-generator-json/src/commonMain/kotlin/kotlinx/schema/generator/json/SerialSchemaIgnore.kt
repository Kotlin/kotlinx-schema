package kotlinx.schema.generator.json

import kotlinx.serialization.SerialInfo

/**
 * Excludes the annotated [@Serializable][kotlinx.serialization.Serializable] class from schema generation.
 *
 * Unlike [@SchemaIgnore][kotlinx.schema.SchemaIgnore], this annotation carries [@SerialInfo] so it is
 * preserved in [SerialDescriptor][kotlinx.serialization.descriptors.SerialDescriptor] and is automatically
 * recognized by the serialization-based schema generator.
 *
 * When applied to a sealed subtype, the subtype is omitted from the parent's
 * polymorphic `oneOf` schema. The class remains fully functional at runtime —
 * only schema generation is affected.
 *
 * Example:
 * ```kotlin
 * @Serializable
 * sealed class Event {
 *     @Serializable
 *     data class Click(val x: Int, val y: Int) : Event()
 *
 *     @Serializable
 *     @SerialSchemaIgnore
 *     data class Internal(val trace: String) : Event()
 * }
 * ```
 *
 * @see kotlinx.schema.SchemaIgnore
 * @see SerialDescription
 */
@OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)
@SerialInfo
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
public annotation class SerialSchemaIgnore
