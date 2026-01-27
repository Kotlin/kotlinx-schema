package kotlinx.schema.ksp.strategy

import com.google.devtools.ksp.processing.KSPLogger
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import org.junit.jupiter.api.Test

/**
 * Unit tests for CodeGenerationContext extension functions.
 */
class CodeGenerationContextTest {
    private val mockLogger: KSPLogger = mockk(relaxed = true)

    @Test
    fun `visibility() returns public when option is public`() {
        // Given
        val context =
            CodeGenerationContext(
                options = mapOf("kotlinx.schema.visibility" to "public"),
                parameters = emptyMap(),
                logger = mockLogger,
            )

        // When
        val result = context.visibility()

        // Then
        result shouldBe "public"
    }

    @Test
    fun `visibility() returns internal when option is internal`() {
        // Given
        val context =
            CodeGenerationContext(
                options = mapOf("kotlinx.schema.visibility" to "internal"),
                parameters = emptyMap(),
                logger = mockLogger,
            )

        // When
        val result = context.visibility()

        // Then
        result shouldBe "internal"
    }

    @Test
    fun `visibility() returns private when option is private`() {
        // Given
        val context =
            CodeGenerationContext(
                options = mapOf("kotlinx.schema.visibility" to "private"),
                parameters = emptyMap(),
                logger = mockLogger,
            )

        // When
        val result = context.visibility()

        // Then
        result shouldBe "private"
    }

    @Test
    fun `visibility() returns empty string when option is empty`() {
        // Given
        val context =
            CodeGenerationContext(
                options = mapOf("kotlinx.schema.visibility" to ""),
                parameters = emptyMap(),
                logger = mockLogger,
            )

        // When
        val result = context.visibility()

        // Then
        result shouldBe ""
    }

    @Test
    fun `visibility() returns empty string when option is missing`() {
        // Given
        val context =
            CodeGenerationContext(
                options = emptyMap(),
                parameters = emptyMap(),
                logger = mockLogger,
            )

        // When
        val result = context.visibility()

        // Then
        result shouldBe ""
    }

    @Test
    fun `visibility() throws exception for invalid value`() {
        // Given
        val context =
            CodeGenerationContext(
                options = mapOf("kotlinx.schema.visibility" to "protected"),
                parameters = emptyMap(),
                logger = mockLogger,
            )

        // When/Then
        val exception =
            shouldThrow<IllegalArgumentException> {
                context.visibility()
            }
        exception.message shouldBe "Invalid visibility option: protected"
    }

    @Test
    fun `visibility() trims whitespace from option value`() {
        // Given
        val context =
            CodeGenerationContext(
                options = mapOf("kotlinx.schema.visibility" to "  public  "),
                parameters = emptyMap(),
                logger = mockLogger,
            )

        // When
        val result = context.visibility()

        // Then
        result shouldBe "public"
    }

    @Test
    fun `visibility() reads from annotation parameter when option is not set`() {
        // Given
        val context =
            CodeGenerationContext(
                options = emptyMap(),
                parameters = mapOf("visibility" to "internal"),
                logger = mockLogger,
            )

        // When
        val result = context.visibility()

        // Then
        result shouldBe "internal"
    }

    @Test
    fun `visibility() annotation parameter takes precedence over option`() {
        // Given
        val context =
            CodeGenerationContext(
                options = mapOf("kotlinx.schema.visibility" to "public"),
                parameters = mapOf("visibility" to "internal"),
                logger = mockLogger,
            )

        // When
        val result = context.visibility()

        // Then
        result shouldBe "internal"
    }

    @Test
    fun `visibility() throws exception for invalid annotation parameter value`() {
        // Given
        val context =
            CodeGenerationContext(
                options = emptyMap(),
                parameters = mapOf("visibility" to "protected"),
                logger = mockLogger,
            )

        // When/Then
        val exception =
            shouldThrow<IllegalArgumentException> {
                context.visibility()
            }
        exception.message shouldBe "Invalid visibility option: protected"
    }

    @Test
    fun `visibility() trims whitespace from annotation parameter value`() {
        // Given
        val context =
            CodeGenerationContext(
                options = emptyMap(),
                parameters = mapOf("visibility" to "  private  "),
                logger = mockLogger,
            )

        // When
        val result = context.visibility()

        // Then
        result shouldBe "private"
    }

    @Test
    fun `visibility() returns empty string when annotation parameter is empty`() {
        // Given
        val context =
            CodeGenerationContext(
                options = emptyMap(),
                parameters = mapOf("visibility" to ""),
                logger = mockLogger,
            )

        // When
        val result = context.visibility()

        // Then
        result shouldBe ""
    }

    @Test
    fun `visibility() returns empty string when annotation parameter is whitespace only`() {
        // Given
        val context =
            CodeGenerationContext(
                options = emptyMap(),
                parameters = mapOf("visibility" to "   "),
                logger = mockLogger,
            )

        // When
        val result = context.visibility()

        // Then
        result shouldBe ""
    }
}
