package kotlinx.schema.integration.type

/**
 * Simple class without annotation - should not generate extensions
 */
data class NonAnnotatedClass(
    val value: String,
)
