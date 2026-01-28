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

    @ParameterizedTest(name = "option={0} param={1} => {2}")
    @CsvSource(
        value = [
            "null, null, false", // default
            "null, true, true",
            "true, null, true",
            "false, true, true", // parameter overrides option
            "true, false, false", // parameter overrides option
        ],
        nullValues = ["null"],
    )
    fun `should return shouldGenerateSchemaObject`(
        option: Boolean?,
        parameter: Boolean?,
        expected: Boolean,
    ) {
        // Given
        val options =
            if (option != null) {
                mapOf("kotlinx.schema.withSchemaObject" to option.toString())
            } else {
                emptyMap()
            }

        val parameters =
            if (parameter != null) {
                mapOf("withSchemaObject" to parameter.toString())
            } else {
                emptyMap()
            }

        val context =
            CodeGenerationContext(
                options = options,
                parameters = parameters,
                logger = mockLogger,
            )

        // When
        val result = context.shouldGenerateSchemaObject()

        // Then
        result shouldBe expected
    }

    @ParameterizedTest(name = "option={0} => {1}")
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
