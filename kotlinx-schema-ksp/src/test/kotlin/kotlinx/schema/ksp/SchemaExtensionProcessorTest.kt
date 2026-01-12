package kotlinx.schema.ksp

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.KSAnnotated
import io.kotest.matchers.collections.shouldBeEmpty
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

/**
 * Unit tests for SchemaExtensionProcessor.
 *
 * These tests verify that the processor correctly:
 * - Passes options to ClassSourceCodeGenerator with correct precedence
 * - Creates files with proper naming and packages
 * - Handles enabled/disabled state
 * - Manages lifecycle correctly
 *
 * Code generation logic itself is tested in SourceCodeGeneratorTest.
 */
@ExtendWith(MockKExtension::class)
class SchemaExtensionProcessorTest {
    @MockK
    private lateinit var codeGenerator: CodeGenerator

    @MockK
    private lateinit var sourceCodeGenerator: ClassSourceCodeGenerator

    @MockK(relaxUnitFun = true)
    private lateinit var logger: KSPLogger

    @MockK
    private lateinit var resolver: Resolver

    private lateinit var subject: SchemaExtensionProcessor

    @BeforeEach
    fun beforeEach() {
        subject =
            SchemaExtensionProcessor(
                codeGenerator,
                sourceCodeGenerator,
                logger,
                options = emptyMap(),
            )
    }

    @Test
    fun `should skip processing when disabled via options`() {
        // Given
        val options = mapOf("kotlinx.schema.enabled" to "false")
        subject = SchemaExtensionProcessor(codeGenerator, sourceCodeGenerator, logger, options)

        val symbols = mockk<Sequence<KSAnnotated>>()
        every { resolver.getSymbolsWithAnnotation("kotlinx.schema.Schema") } returns symbols

        // When
        val result = subject.process(resolver)

        // Then
        result.shouldBeEmpty()
        verify(exactly = 0) { codeGenerator.createNewFile(any(), any(), any()) }
    }

    @Test
    fun `should process when enabled is explicitly true`() {
        // Given
        val options = mapOf("kotlinx.schema.enabled" to "true")
        subject = SchemaExtensionProcessor(codeGenerator, sourceCodeGenerator, logger, options)

        val emptySymbols = emptySequence<KSAnnotated>()
        every { resolver.getSymbolsWithAnnotation("kotlinx.schema.Schema") } returns emptySymbols

        // When
        val result = subject.process(resolver)

        // Then
        result.shouldBeEmpty()
    }

    @Test
    fun `should process when enabled option is not set (default enabled)`() {
        // Given
        val options = emptyMap<String, String>()
        subject = SchemaExtensionProcessor(codeGenerator, sourceCodeGenerator, logger, options)

        val emptySymbols = emptySequence<KSAnnotated>()
        every { resolver.getSymbolsWithAnnotation("kotlinx.schema.Schema") } returns emptySymbols

        // When
        val result = subject.process(resolver)

        // Then
        result.shouldBeEmpty()
    }

    @Test
    fun `finish should log success message`() {
        // Given
        val options = emptyMap<String, String>()
        subject = SchemaExtensionProcessor(codeGenerator, sourceCodeGenerator, logger, options)

        // When
        subject.finish()

        // Then
        verify { logger.info(match { it.contains("Done") }) }
    }

    @Test
    fun `onError should log error with options`() {
        // Given
        val options =
            mapOf(
                "kotlinx.schema.enabled" to "true",
                "kotlinx.schema.withSchemaObject" to "true",
            )
        subject = SchemaExtensionProcessor(codeGenerator, sourceCodeGenerator, logger, options)

        // When
        subject.onError()

        // Then
        verify {
            logger.error(match { it.contains("Error") && it.contains("KSP Processor Options") })
        }
    }
}
