package kotlinx.schema.ksp.gradle.plugin

import com.google.devtools.ksp.impl.KotlinSymbolProcessing
import com.google.devtools.ksp.processing.KSPJvmConfig
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.SymbolProcessorProvider
import com.google.devtools.ksp.symbol.KSNode
import org.gradle.api.Project
import org.gradle.api.file.FileCollection
import org.gradle.api.logging.LogLevel
import org.gradle.api.provider.Property
import org.jetbrains.kotlin.gradle.plugin.getKotlinPluginVersion
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask
import java.io.File
import java.net.URLClassLoader
import java.util.ServiceLoader

/**
 * Executes KSP2 programmatically for schema generation.
 */
internal class KspExecutor(
    private val project: Project,
) {
    /**
     * Execute KSP2 processing for a given source set.
     *
     * @param taskName Name for logging
     * @param sourceRoots Kotlin source directories
     * @param commonSourceRoots Common/shared source directories (for multiplatform)
     * @param classpath Compilation classpath
     * @param processorOptions Options to pass to processors
     * @param processorClasspath Classpath containing processor JARs
     * @param compileTask The Kotlin compile task to extract configuration from
     */
    @Suppress("LongMethod", "LongParameterList")
    fun execute(
        taskName: String,
        sourceRoots: List<File>,
        commonSourceRoots: List<File> = emptyList(),
        classpath: FileCollection,
        processorOptions: Map<String, String>,
        processorClasspath: FileCollection,
        compileTask: KotlinCompilationTask<*>,
    ): Boolean {
        project.logger.log(LogLevel.INFO, "kotlinx-schema: Executing KSP2 for $taskName")
        project.logger.log(LogLevel.INFO, "kotlinx-schema: Source roots: $sourceRoots")
        project.logger.log(LogLevel.INFO, "kotlinx-schema: Common source roots: $commonSourceRoots")
        project.logger.log(LogLevel.INFO, "kotlinx-schema: Classpath: ${classpath.files.size} files")
        classpath.files.forEach { file ->
            project.logger.log(LogLevel.INFO, "kotlinx-schema:   - ${file.name}")
        }

        // Extract build directory for reuse
        val buildDir =
            project.layout.buildDirectory
                .get()
                .asFile

        // Create output directories under build/generated/kotlinxSchema
        val baseOutputDir = buildDir.resolve("generated/kotlinxSchema/$taskName")
        val kotlinOutputDir = baseOutputDir.resolve("kotlin").apply { mkdirs() }
        val javaOutputDir = baseOutputDir.resolve("java").apply { mkdirs() }
        val resourceOutputDir = baseOutputDir.resolve("resources").apply { mkdirs() }
        val classOutputDir = buildDir.resolve("ksp-classes/$taskName").apply { mkdirs() }
        val cachesDir = buildDir.resolve("ksp-cache/$taskName").apply { mkdirs() }

        // Load processors
        val processors = loadProcessors(processorClasspath)
        if (processors.isEmpty()) {
            project.logger.error("kotlinx-schema: No KSP processors found")
            return false
        }

        project.logger.log(
            LogLevel.INFO,
            "kotlinx-schema: Loaded ${processors.size} processor(s)",
        )

        // Create logger
        val logger = GradleKspLogger(project)

        // Extract Kotlin compilation parameters from task
        val compilerOptions = compileTask.compilerOptions

        // Extract jvmTarget (only available on JVM tasks)
        val jvmTargetRaw =
            (compilerOptions as? org.jetbrains.kotlin.gradle.dsl.KotlinJvmCompilerOptions)
                ?.jvmTarget
                ?.orNull
                ?.toString()
                ?: PluginConstants.DEFAULT_JVM_TARGET
        val jvmTarget = jvmTargetRaw.removePrefix(PluginConstants.JVM_TARGET_PREFIX)

        // Extract language and API versions - use major.minor format only
        val languageVersion = extractKotlinVersion(property = compilerOptions.languageVersion)
        val apiVersion = extractKotlinVersion(property = compilerOptions.apiVersion)

        // Extract warning settings
        val allWarningsAsErrors = compilerOptions.allWarningsAsErrors.orNull ?: false

        project.logger.log(
            LogLevel.INFO,
            "kotlinx-schema: Kotlin config from task - jvmTarget=$jvmTarget, languageVersion=$languageVersion, " +
                "apiVersion=$apiVersion, allWarningsAsErrors=$allWarningsAsErrors",
        )

        // Configure KSP2
        val modifiedSources = mutableListOf<File>()
        val removedSources = mutableListOf<File>()
        val changedClasses = mutableListOf<String>()
        val kspConfig =
            KSPJvmConfig(
                moduleName = project.name,
                sourceRoots = sourceRoots,
                commonSourceRoots = commonSourceRoots,
                javaSourceRoots = emptyList(),
                libraries = classpath.files.toList(),
                outputBaseDir = buildDir,
                kotlinOutputDir = kotlinOutputDir,
                javaOutputDir = javaOutputDir,
                classOutputDir = classOutputDir,
                resourceOutputDir = resourceOutputDir,
                projectBaseDir = project.projectDir,
                cachesDir = cachesDir,
                jdkHome = File(System.getProperty("java.home")),
                jvmTarget = jvmTarget,
                jvmDefaultMode = PluginConstants.JVM_DEFAULT_MODE,
                languageVersion = languageVersion,
                apiVersion = apiVersion,
                processorOptions = processorOptions,
                incremental = false,
                incrementalLog = false,
                modifiedSources = modifiedSources,
                removedSources = removedSources,
                changedClasses = changedClasses,
                friends = emptyList(),
                allWarningsAsErrors = allWarningsAsErrors,
                mapAnnotationArgumentsInJava = true,
            )

        // Execute KSP2
        val processing =
            KotlinSymbolProcessing(
                kspConfig = kspConfig,
                symbolProcessorProviders = processors,
                logger = logger,
            )

        val exitCode = processing.execute()

        return if (exitCode == KotlinSymbolProcessing.ExitCode.OK) {
            project.logger.log(LogLevel.INFO, "kotlinx-schema: KSP2 processing completed successfully")
            true
        } else {
            project.logger.error("kotlinx-schema: KSP2 processing failed with exit code: $exitCode")
            false
        }
    }

    private fun loadProcessors(processorClasspath: FileCollection): List<SymbolProcessorProvider> {
        val urls = processorClasspath.files.map { it.toURI().toURL() }.toTypedArray()
        val classLoader = URLClassLoader(urls, javaClass.classLoader)

        return ServiceLoader
            .load(SymbolProcessorProvider::class.java, classLoader)
            .toList()
    }

    private fun extractKotlinVersion(property: Property<org.jetbrains.kotlin.gradle.dsl.KotlinVersion>): String =
        property.orNull?.version ?: getKotlinPluginVersion(project.logger).substringBeforeLast('.')

    /**
     * KSP Logger that delegates to Gradle logger.
     */
    private class GradleKspLogger(
        private val project: Project,
    ) : KSPLogger {
        override fun logging(
            message: String,
            symbol: KSNode?,
        ) {
            project.logger.info("KSP: $message")
        }

        override fun info(
            message: String,
            symbol: KSNode?,
        ) {
            project.logger.info("KSP: $message")
        }

        override fun warn(
            message: String,
            symbol: KSNode?,
        ) {
            project.logger.warn("KSP: $message")
        }

        override fun error(
            message: String,
            symbol: KSNode?,
        ) {
            project.logger.error("KSP: $message")
        }

        override fun exception(e: Throwable) {
            project.logger.error("KSP Exception", e)
        }
    }
}
