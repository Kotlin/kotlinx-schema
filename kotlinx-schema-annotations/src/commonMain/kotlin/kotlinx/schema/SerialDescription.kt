package kotlinx.schema

import kotlinx.serialization.SerialInfo

/**
 * Marks a description for a [@Serializable][kotlinx.serialization.Serializable] class or property
 * to be included in the generated JSON Schema.
 *
 * Unlike [@Description], this annotation carries [@SerialInfo] so it is preserved in
 * [SerialDescriptor][kotlinx.serialization.descriptors.SerialDescriptor] and is automatically
 * recognized by the serialization-based schema generator without any extra configuration.
 *
 * Example:
 * ```kotlin
 * @Serializable
 * @SerialDescription("A user account")
 * data class User(
 *     @SerialDescription("The user's full name")
 *     val name: String,
 * )
 * ```
 *
 * @see Description
 */
@OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)
@SerialInfo
@Target(
    AnnotationTarget.CLASS,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY,
    AnnotationTarget.VALUE_PARAMETER,
)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
public annotation class SerialDescription(
    val value: String,
)
