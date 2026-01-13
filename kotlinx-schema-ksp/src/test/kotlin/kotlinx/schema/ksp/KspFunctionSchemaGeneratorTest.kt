package kotlinx.schema.ksp

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

/**
 * Tests for KSP-based function schema generation.
 *
 * Note: Full KSP integration tests require compilation and are handled separately
 * in the ksp-integration-tests module. These tests verify the basic registration
 * and structure of the generator.
 */
class KspFunctionSchemaGeneratorTest {
    @Test
    fun `KspFunctionSchemaGenerator should have correct types`() {
        val generator = KspFunctionSchemaGenerator()
        generator.schemaType().simpleName shouldBe "FunctionCallingSchema"
        generator.targetType().simpleName shouldBe "KSFunctionDeclaration"
    }
}
