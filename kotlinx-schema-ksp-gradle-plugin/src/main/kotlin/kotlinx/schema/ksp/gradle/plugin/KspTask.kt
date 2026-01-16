package kotlinx.schema.ksp.gradle.plugin

import com.google.devtools.ksp.impl.KotlinSymbolProcessing
import com.google.devtools.ksp.processing.KSPJvmConfig
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import java.io.File

/**
 * Task that executes KSP to generate schema extension properties.
 *
 * This task is configuration-cache compatible:
 * - All inputs resolved during configuration phase
 * - No Project or Configuration references in task action
 * - Uses only serializable properties
 *
 * This task is cacheable because KSP is deterministic:
 * - Same inputs (sources, classpath, options) produce same outputs
 * - All inputs and outputs are properly declared
 */
@CacheableTask
internal abstract class KspTask : DefaultTask() {
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    internal abstract val sourceRoots: ConfigurableFileCollection

    @get:Classpath
    internal abstract val classpath: ConfigurableFileCollection

    @get:Classpath
    internal abstract val processorClasspath: ConfigurableFileCollection

    @get:Input
    internal abstract val processorOptions: MapProperty<String, String>

    @get:OutputDirectory
    internal abstract val outputDirectory: DirectoryProperty

    @get:Input
    internal abstract val moduleName: Property<String>

    @get:Internal
    internal abstract val projectBaseDir: DirectoryProperty

    @get:Nested
    internal abstract val compilerConfig: Property<CompilerConfig>

    @Suppress("LongMethod", "ThrowsCount")
    @TaskAction
    internal fun execute() {
        logger.info("kotlinx-schema: Executing KSP for ${moduleName.get()}")

        // Check processor classpath
        if (processorClasspath.isEmpty) {
            logger.error("kotlinx-schema: No KSP processors found")
            throw GradleException("No KSP processors found")
        }

        // Load processors
        val processors = loadProcessors(processorClasspath)
        if (processors.isEmpty()) {
            logger.error("kotlinx-schema: No KSP processors found")
            throw GradleException("No KSP processors found")
        }

        logger.info("kotlinx-schema: Loaded ${processors.size} processor(s)")

        // Setup output directories
        val outputDir = outputDirectory.get().asFile
        val kotlinOutputDir = outputDir.resolve("kotlin").apply { mkdirs() }
        val javaOutputDir = outputDir.resolve("java").apply { mkdirs() }
        val resourceOutputDir = outputDir.resolve("resources").apply { mkdirs() }
        val classOutputDir = outputDir.resolveSibling("ksp-classes/${moduleName.get()}").apply { mkdirs() }
        val cachesDir = outputDir.resolveSibling("ksp-cache/${moduleName.get()}").apply { mkdirs() }

        val config = compilerConfig.get()
        logger.info(
            "kotlinx-schema: Kotlin config - jvmTarget=${config.jvmTarget}, " +
                "languageVersion=${config.languageVersion}, apiVersion=${config.apiVersion}",
        )

        // Configure KSP2
        val kspConfig =
            KSPJvmConfig(
                moduleName = moduleName.get(),
                sourceRoots = sourceRoots.files.toList(),
                commonSourceRoots = emptyList(),
                javaSourceRoots = emptyList(),
                libraries = classpath.files.toList(),
                outputBaseDir = outputDir.parentFile,
                kotlinOutputDir = kotlinOutputDir,
                javaOutputDir = javaOutputDir,
                classOutputDir = classOutputDir,
                resourceOutputDir = resourceOutputDir,
                projectBaseDir = projectBaseDir.get().asFile,
                cachesDir = cachesDir,
                jdkHome = File(System.getProperty("java.home")),
                jvmTarget = config.jvmTarget,
                jvmDefaultMode = "all-compatibility",
                languageVersion = config.languageVersion,
                apiVersion = config.apiVersion,
                processorOptions = processorOptions.get(),
                incremental = false,
                incrementalLog = false,
                modifiedSources = mutableListOf(),
                removedSources = mutableListOf(),
                changedClasses = mutableListOf(),
                friends = emptyList(),
                allWarningsAsErrors = config.allWarningsAsErrors,
                mapAnnotationArgumentsInJava = true,
            )

        // Execute KSP
        val kspLogger = GradleKspLogger(logger)
        val ksp =
            KotlinSymbolProcessing(
                kspConfig = kspConfig,
                symbolProcessorProviders = processors,
                logger = kspLogger,
            )

        val exitCode = ksp.execute()

        if (exitCode != KotlinSymbolProcessing.ExitCode.OK) {
            throw GradleException("KSP processing failed with exit code: $exitCode")
        }

        logger.info("kotlinx-schema: KSP execution completed successfully")
    }
}

/**
 * Compiler configuration data extracted during configuration phase.
 * Contains only primitive/serializable data for configuration cache compatibility.
 */
internal data class CompilerConfig(
    @get:Input
    internal val jvmTarget: String,
    @get:Input
    internal val languageVersion: String,
    @get:Input
    internal val apiVersion: String,
    @get:Input
    internal val allWarningsAsErrors: Boolean,
) : java.io.Serializable
