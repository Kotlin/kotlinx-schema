package kotlinx.schema.integration

/**
 * Simple class without annotation - should not generate extensions
 */
data class NonAnnotatedClass(
    val value: String,
)
