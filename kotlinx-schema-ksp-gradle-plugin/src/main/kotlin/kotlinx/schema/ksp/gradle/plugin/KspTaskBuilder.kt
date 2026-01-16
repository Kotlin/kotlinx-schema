package kotlinx.schema.ksp.gradle.plugin

import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.tasks.TaskProvider
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
    ) = project.tasks.register(config.taskName, KspTask::class.java) { task ->
        task.group = "kotlinx-schema"
        task.description = "Generate JSON schema extension properties"

        // Configure output directory
        task.outputDirectory.set(
            project.layout.buildDirectory.dir(
                "${PluginConstants.GENERATED_BASE_PATH}/${config.outputName}",
            ),
        )
        task.moduleName.set(config.outputName)

        // Configure source roots
        task.sourceRoots.from(sourceRoots)

        // Configure project base directory
        task.projectBaseDir.set(project.layout.projectDirectory)

        // Resolve classpath during configuration phase
        val classpathConfig =
            project.configurations.findByName(config.classpathConfigName)
                ?: error("Classpath configuration ${config.classpathConfigName} not found")

        // Pass configuration directly to preserve implicit task dependencies
        // Using ConfigurableFileCollection.from(Configuration) allows Gradle to:
        // 1. Infer task dependencies automatically
        // 2. Support configuration cache (FileCollection is serializable)
        // 3. Resolve files lazily during task execution
        task.classpath.from(classpathConfig)

        // Configure processor classpath (already correct)
        task.processorClasspath.from(kspConfig)

        // Configure processor options
        task.processorOptions.set(buildProcessorOptions(extension))

        // Extract compiler config during configuration phase
        val compileTask =
            project.tasks.findByName(config.compileTaskName) as? KotlinCompilationTask<*>
                ?: error("Compile task ${config.compileTaskName} not found or not a KotlinCompilationTask")
        task.compilerConfig.set(extractCompilerConfig(compileTask, project))
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
        kspTask: TaskProvider<KspTask>,
    ) {
        // Setup dependency: compile task depends on KSP task
        project.tasks.findByName(compileTaskName)?.let { compileTask ->
            compileTask.dependsOn(kspTask)
            project.logger.info(
                "kotlinx-schema: Added KSP dependency to $compileTaskName (direct)",
            )
        } ?: run {
            // Task doesn't exist yet, configure it when it's added
            project.logger.info(
                "kotlinx-schema: Task $compileTaskName not found, will configure when added",
            )
            project.tasks.configureEach {
                if (it.name == compileTaskName) {
                    it.dependsOn(kspTask)
                    project.logger.info(
                        "kotlinx-schema: Added KSP dependency to $compileTaskName (deferred)",
                    )
                }
            }
        }
    }

    private fun registerGeneratedSources(
        config: KspTaskConfig,
        kspTask: TaskProvider<KspTask>,
    ) {
        val generatedDir =
            project.layout.buildDirectory
                .dir("${PluginConstants.GENERATED_BASE_PATH}/${config.outputName}/kotlin")
                .get()
                .asFile

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
            project.logger.error(
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
    private const val PROPERTY_FILE = "kotlinxSchema.properties"

    fun getPluginVersion(): String {
        val inputStream =
            VersionProvider::class.java.classLoader
                .getResourceAsStream(PROPERTY_FILE)
                ?: error("$PROPERTY_FILE not found")

        return inputStream.use {
            val properties = java.util.Properties()
            properties.load(it)
            properties.getProperty("plugin.version")
                ?: error("`plugin.version` property not found in $PROPERTY_FILE")
        }
    }
}
