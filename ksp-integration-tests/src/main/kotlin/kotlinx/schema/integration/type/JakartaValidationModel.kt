package kotlinx.schema.integration.type

/**
 * Local definition of Min annotation to simulate Jakarta Validation behavior
 * in integration tests where jakarta.validation dependency is not available.
 * 
 * The introspection logic identifies annotations by name ("Min"), so this
 * is functionally equivalent for testing purposes.
 */
annotation class Min(val value: Long)

/**
 * Test model for verifying validation constraint extraction via KSP.
 */
data class JakartaValidationModel(
    @field:Min(5)
    val age: Int
)
