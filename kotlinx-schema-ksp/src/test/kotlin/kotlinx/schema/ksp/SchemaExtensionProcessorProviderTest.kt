package kotlinx.schema.ksp

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import org.junit.jupiter.api.Test

class SchemaExtensionProcessorProviderTest {
    @MockK
    private lateinit var options: Map<String, String>

    @MockK
    private lateinit var kspLogger: KSPLogger

    @MockK
    private lateinit var codeGenerator: CodeGenerator

    @MockK
    private lateinit var environment: SymbolProcessorEnvironment

    private val provider = SchemaExtensionProcessorProvider()

    @Test
    fun `Should create generator`() {
        MockKAnnotations.init(this)
        every { environment.codeGenerator } returns codeGenerator
        every { environment.logger } returns kspLogger
        every { environment.options } returns options

        val processor = provider.create(environment = environment)

        processor.shouldBeInstanceOf<SchemaExtensionProcessor>()
    }
}
