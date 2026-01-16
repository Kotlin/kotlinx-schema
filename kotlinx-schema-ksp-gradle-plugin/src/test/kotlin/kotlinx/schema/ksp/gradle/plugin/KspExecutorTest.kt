package kotlinx.schema.ksp.gradle.plugin

import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileCollection
import org.gradle.api.file.ProjectLayout
import org.gradle.api.logging.LogLevel
import org.gradle.api.logging.Logger
import org.gradle.api.provider.Property
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmCompilerOptions
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import java.io.File
import java.util.ServiceLoader
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

/**
 * Unit tests for KspExecutor using MockK.
 */
@Execution(ExecutionMode.SAME_THREAD)
class KspExecutorTest {
    private lateinit var project: Project
    private lateinit var logger: Logger
    private lateinit var executor: KspExecutor
    private lateinit var tempDir: File

    @BeforeTest
    fun setup() {
        // Create mocks
        project = mockk(relaxed = true)
        logger = mockk(relaxed = true)

        // Setup temp directory
        tempDir = createTempDirectory("ksp-executor-test").toFile()

        // Mock project layout
        val layout = mockk<ProjectLayout>()
        val buildDirProperty = mockk<DirectoryProperty>()
        every { project.layout } returns layout
        every { layout.buildDirectory } returns buildDirProperty
        every { buildDirProperty.get().asFile } returns tempDir
        every { project.projectDir } returns tempDir
        every { project.name } returns "test-project"
        every { project.logger } returns logger

        executor = KspExecutor(project)
    }

    @AfterTest
    fun cleanup() {
        clearAllMocks()
        tempDir.deleteRecursively()
    }

    @Test
    fun `execute creates output directories`() {
        // Given
        val processorClasspath = mockProcessorClasspath()
        val sourceRoots = listOf(tempDir.resolve("src"))
        val classpath = mockk<FileCollection>()
        every { classpath.files } returns emptySet()
        val compileTask = mockCompilationTask()

        // When
        executor.execute(
            taskName = "commonMain",
            sourceRoots = sourceRoots,
            classpath = classpath,
            processorOptions = emptyMap(),
            processorClasspath = processorClasspath,
            compileTask = compileTask,
        )

        // Then: Verify directories were created
        tempDir.resolve("generated/kotlinxSchema/commonMain/kotlin").exists() shouldBe true
        tempDir.resolve("generated/kotlinxSchema/commonMain/java").exists() shouldBe true
        tempDir.resolve("generated/kotlinxSchema/commonMain/resources").exists() shouldBe true
        tempDir.resolve("ksp-classes/commonMain").exists() shouldBe true
        tempDir.resolve("ksp-cache/commonMain").exists() shouldBe true
    }

    @Test
    fun `execute returns false when ServiceLoader returns empty list`() {
        // Given: Non-empty processor classpath but ServiceLoader finds no processors
        val processorClasspath = mockProcessorClasspath()
        val sourceRoots = listOf(tempDir.resolve("src"))
        val classpath = mockk<FileCollection>()
        every { classpath.files } returns emptySet()
        val compileTask = mockCompilationTask()

        // Mock ServiceLoader to return empty iterator
        mockkStatic(ServiceLoader::class)
        val emptyIterator = mockk<MutableIterator<com.google.devtools.ksp.processing.SymbolProcessorProvider>>()
        every { emptyIterator.hasNext() } returns false

        val emptyLoader = mockk<ServiceLoader<com.google.devtools.ksp.processing.SymbolProcessorProvider>>()
        every { emptyLoader.iterator() } returns emptyIterator

        every {
            ServiceLoader.load(
                com.google.devtools.ksp.processing.SymbolProcessorProvider::class.java,
                any<ClassLoader>(),
            )
        } returns emptyLoader

        try {
            // When
            val result =
                executor.execute(
                    taskName = "test",
                    sourceRoots = sourceRoots,
                    classpath = classpath,
                    processorOptions = emptyMap(),
                    processorClasspath = processorClasspath,
                    compileTask = compileTask,
                )

            // Then
            result shouldBe false
            verify { logger.error("kotlinx-schema: No KSP processors found") }
        } finally {
            unmockkStatic(ServiceLoader::class)
        }
    }

    @Test
    fun `execute logs source roots and classpath`() {
        // Given
        val processorClasspath = mockProcessorClasspath()
        val sourceRoot1 = tempDir.resolve("src/main/kotlin")
        val sourceRoot2 = tempDir.resolve("src/commonMain/kotlin")
        val sourceRoots = listOf(sourceRoot1, sourceRoot2)

        val classpathFile = tempDir.resolve("lib.jar")
        val classpath = mockk<FileCollection>()
        every { classpath.files } returns setOf(classpathFile)

        val compileTask = mockCompilationTask()

        // When
        executor.execute(
            taskName = "test",
            sourceRoots = sourceRoots,
            commonSourceRoots = listOf(sourceRoot2),
            classpath = classpath,
            processorOptions = emptyMap(),
            processorClasspath = processorClasspath,
            compileTask = compileTask,
        )

        // Then
        verify { logger.log(LogLevel.INFO, "kotlinx-schema: Source roots: $sourceRoots") }
        verify { logger.log(LogLevel.INFO, "kotlinx-schema: Common source roots: ${listOf(sourceRoot2)}") }
        verify { logger.log(LogLevel.INFO, "kotlinx-schema: Classpath: 1 files") }
    }

    @Test
    fun `execute extracts JVM target from compiler options`() {
        // Given
        val processorClasspath = mockProcessorClasspath()
        val sourceRoots = listOf(tempDir.resolve("src"))
        val classpath = mockk<FileCollection>()
        every { classpath.files } returns emptySet()

        val jvmOptions = mockk<KotlinJvmCompilerOptions>(relaxed = true)
        val jvmTargetProperty = mockk<Property<org.jetbrains.kotlin.gradle.dsl.JvmTarget>>()
        val jvmTargetValue = mockk<org.jetbrains.kotlin.gradle.dsl.JvmTarget>()

        every { jvmOptions.jvmTarget } returns jvmTargetProperty
        every { jvmTargetProperty.orNull } returns jvmTargetValue
        every { jvmTargetValue.toString() } returns "JVM_17"

        val compileTask = mockCompilationTask(jvmOptions)

        // When
        executor.execute(
            taskName = "test",
            sourceRoots = sourceRoots,
            classpath = classpath,
            processorOptions = emptyMap(),
            processorClasspath = processorClasspath,
            compileTask = compileTask,
        )

        // Then: Verify JVM target was extracted and logged
        verify {
            logger.log(
                LogLevel.INFO,
                match { it.contains("jvmTarget=17") },
            )
        }
    }

    @Test
    fun `execute uses default JVM target when not available`() {
        // Given
        val processorClasspath = mockProcessorClasspath()
        val sourceRoots = listOf(tempDir.resolve("src"))
        val classpath = mockk<FileCollection>()
        every { classpath.files } returns emptySet()

        // Compile task without JVM options (e.g., JS/Native target)
        val compileTask = mockCompilationTask(jvmOptions = null)

        // When
        executor.execute(
            taskName = "test",
            sourceRoots = sourceRoots,
            classpath = classpath,
            processorOptions = emptyMap(),
            processorClasspath = processorClasspath,
            compileTask = compileTask,
        )

        // Then: Should use default JVM target (11)
        verify {
            logger.log(
                LogLevel.INFO,
                match { it.contains("jvmTarget=${PluginConstants.DEFAULT_JVM_TARGET}") },
            )
        }
    }

    private fun mockCompilationTask(jvmOptions: KotlinJvmCompilerOptions? = null): KotlinCompilationTask<*> {
        val task = mockk<KotlinCompilationTask<*>>(relaxed = true)

        // If jvmOptions is provided, use it directly
        // Otherwise, create a non-JVM compiler options mock (like JS/Native)
        val compilerOptions =
            jvmOptions ?: mockk<org.jetbrains.kotlin.gradle.dsl.KotlinCommonCompilerOptions>(relaxed = true)

        every { task.compilerOptions } returns compilerOptions

        // Mock language and API version properties
        val languageVersionProp = mockk<Property<KotlinVersion>>()
        val apiVersionProp = mockk<Property<KotlinVersion>>()
        every { compilerOptions.languageVersion } returns languageVersionProp
        every { compilerOptions.apiVersion } returns apiVersionProp
        every { languageVersionProp.orNull } returns null
        every { apiVersionProp.orNull } returns null

        // Mock allWarningsAsErrors
        val warningsProp = mockk<Property<Boolean>>()
        every { compilerOptions.allWarningsAsErrors } returns warningsProp
        every { warningsProp.orNull } returns false

        return task
    }

    private fun mockProcessorClasspath(): FileCollection {
        // Create a dummy processor JAR file so the classpath is not empty
        // This allows the executor to proceed past the empty classpath check
        val dummyJar = tempDir.resolve("processor.jar")
        dummyJar.writeText("dummy") // Create a file (not a real JAR, but good enough for testing)

        val classpath = mockk<FileCollection>()
        every { classpath.files } returns setOf(dummyJar)
        return classpath
    }
}
