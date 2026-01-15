package kotlinx.schema.gradle

import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.logging.LogLevel
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask

/**
 * Builds KSP tasks for schema generation.
 */
internal class KspTaskBuilder(
    private val project: Project,
) {
    private val sourceCollector = SourceCollector(project)
    private val sourceSetRegistrar = SourceSetRegistrar(project)

    fun buildTask(
        config: KspTaskConfig,
        extension: KotlinxSchemaExtension,
    ) {
        val sourceRoots = sourceCollector.collectSourceRoots(config.sourceSets)
        if (sourceRoots.isEmpty()) {
            project.logger.warn("kotlinx-schema: No source directories found")
            return
        }

        val kspConfig = createKspConfiguration()
        addProcessorDependency()

        val kspTask = createKspTask(config, extension, sourceRoots, kspConfig)
        setupTaskDependencies(config.compileTaskName, kspTask)
        registerGeneratedSources(config, kspTask)
    }

    private fun createKspConfiguration() =
        project.configurations.maybeCreate(PluginConstants.KSP_CONFIGURATION).apply {
            isCanBeConsumed = false
            isCanBeResolved = true
        }

    private fun addProcessorDependency() {
        val version = VersionProvider.getPluginVersion()
        project.dependencies.add(
            PluginConstants.KSP_CONFIGURATION,
            "org.jetbrains.kotlinx:kotlinx-schema-ksp:$version",
        )
    }

    private fun createKspTask(
        config: KspTaskConfig,
        extension: KotlinxSchemaExtension,
        sourceRoots: List<java.io.File>,
        kspConfig: org.gradle.api.artifacts.Configuration,
    ) = project.tasks.register(config.taskName) { task ->
        task.group = "kotlinx-schema"
        task.description = "Generate JSON schema extension properties"

        // Declare outputs so Gradle can track generated files
        val outputDir = project.layout.buildDirectory.get().asFile
            .resolve("${PluginConstants.GENERATED_BASE_PATH}/${config.outputName}")
        task.outputs.dir(outputDir)
        task.outputs.cacheIf { true }

        // Declare inputs for up-to-date checking
        sourceRoots.forEach { task.inputs.dir(it) }
        if (extension.rootPackage.isPresent) {
            task.inputs.property("rootPackage", extension.rootPackage)
        }
        task.inputs.property("withSchemaObject", extension.withSchemaObject)

        task.doLast {
            executeKsp(config, extension, sourceRoots, kspConfig)
        }
    }

    private fun executeKsp(
        config: KspTaskConfig,
        extension: KotlinxSchemaExtension,
        sourceRoots: List<java.io.File>,
        kspConfig: org.gradle.api.artifacts.Configuration,
    ) {
        val classpath =
            project.configurations.findByName(config.classpathConfigName)
                ?: error("Classpath configuration ${config.classpathConfigName} not found")

        val compileTask =
            project.tasks.findByName(config.compileTaskName) as? KotlinCompilationTask<*>
                ?: error("Compile task ${config.compileTaskName} not found or not a KotlinCompilationTask")

        val options = buildProcessorOptions(extension)
        val executor = KspExecutor(project)
        val success =
            executor.execute(
                taskName = config.outputName,
                sourceRoots = sourceRoots,
                commonSourceRoots = emptyList(),
                classpath = classpath,
                processorOptions = options,
                processorClasspath = kspConfig,
                compileTask = compileTask,
            )

        if (!success) {
            throw GradleException("KSP processing failed")
        }
    }

    private fun buildProcessorOptions(extension: KotlinxSchemaExtension): Map<String, String> {
        val options = mutableMapOf<String, String>()
        if (extension.rootPackage.isPresent) {
            options["kotlinx.schema.rootPackage"] = extension.rootPackage.get()
        }
        options["kotlinx.schema.withSchemaObject"] = extension.withSchemaObject.get().toString()
        return options
    }

    private fun setupTaskDependencies(
        compileTaskName: String,
        kspTask: org.gradle.api.tasks.TaskProvider<org.gradle.api.Task>,
    ) {
        // Use findByName first to check existence, then configure if found
        project.tasks.findByName(compileTaskName)?.let { compileTask ->
            compileTask.dependsOn(kspTask)
            project.logger.log(
                LogLevel.INFO,
                "kotlinx-schema: Added KSP dependency to $compileTaskName (direct)",
            )
        } ?: run {
            // Task doesn't exist yet, configure it when it's added
            project.logger.log(
                LogLevel.INFO,
                "kotlinx-schema: Task $compileTaskName not found, will configure when added",
            )
            project.tasks.configureEach {
                if (it.name == compileTaskName) {
                    it.dependsOn(kspTask)
                    project.logger.log(
                        LogLevel.INFO,
                        "kotlinx-schema: Added KSP dependency to $compileTaskName (deferred)",
                    )
                }
            }
        }
    }

    private fun registerGeneratedSources(
        config: KspTaskConfig,
        kspTask: org.gradle.api.tasks.TaskProvider<org.gradle.api.Task>,
    ) {
        val generatedDir =
            project.layout.buildDirectory.get().asFile.resolve(
                "${PluginConstants.GENERATED_BASE_PATH}/${config.outputName}/kotlin",
            )

        try {
            sourceSetRegistrar.registerGeneratedSources(
                generatedDir = generatedDir,
                targetSourceSet = config.targetSourceSet,
                outputName = config.outputName,
                kspTaskProvider = kspTask,
            )
        } catch (e: GradleException) {
            throw e
        } catch (e: IllegalStateException) {
            project.logger.log(
                LogLevel.ERROR,
                "kotlinx-schema: Failed to register generated sources: ${e.message}",
                e,
            )
            throw GradleException("Failed to register generated sources", e)
        }
    }
}

/**
 * Provides version information for the plugin.
 */
internal object VersionProvider {
    fun getPluginVersion(): String {
        val properties = java.util.Properties()
        val inputStream = VersionProvider::class.java.classLoader.getResourceAsStream("project.properties")
        return if (inputStream != null) {
            properties.load(inputStream)
            properties.getProperty("version", "0.0.4-SNAPSHOT")
        } else {
            VersionProvider::class.java.`package`?.implementationVersion ?: "0.0.4-SNAPSHOT"
        }
    }
}
