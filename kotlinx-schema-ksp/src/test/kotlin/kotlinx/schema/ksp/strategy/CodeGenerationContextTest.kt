package kotlinx.schema.ksp.strategy

import com.google.devtools.ksp.processing.KSPLogger
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

/**
 * Unit tests for CodeGenerationContext extension functions.
 */
class CodeGenerationContextTest {
    private val mockLogger: KSPLogger = mockk(relaxed = true)

    @ParameterizedTest(name = "option={0} => {2}")
    @CsvSource(
        value = [
            "null, ''", // default
            "'', ''", // default
            "public, public",
            "internal, internal",
            "private, private",
            "'  \t', ''", // trim whitespace
            "'  private  ', private", // trim whitespace
        ],
        nullValues = ["null"],
    )
    fun `should return visibility`(
        optionVisibility: String?,
        expectedVisibility: String,
    ) {
        // Given
        val options =
            if (optionVisibility != null) {
                mapOf("kotlinx.schema.visibility" to optionVisibility)
            } else {
                emptyMap()
            }

        val context =
            CodeGenerationContext(
                options = options,
                parameters = emptyMap(),
                logger = mockLogger,
            )

        // When
        val result = context.visibility()

        // Then
        result shouldBe expectedVisibility
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
    fun `visibility() throws exception for invalid annotation parameter value`() {
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
}
