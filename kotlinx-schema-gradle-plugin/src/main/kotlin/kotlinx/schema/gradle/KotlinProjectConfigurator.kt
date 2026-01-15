package kotlinx.schema.gradle

import org.gradle.api.Project

/**
 * Configures KSP for different Kotlin project types.
 */
internal sealed interface KotlinProjectConfigurator {
    /**
     * Configure KSP for this project type.
     */
    fun configure(
        project: Project,
        extension: KotlinxSchemaExtension,
    )
}

/**
 * Configurator for Kotlin Multiplatform projects.
 *
 * Assumes sources to process are in commonMain source set.
 * Uses metadata compilation task which is always available in multiplatform projects with targets.
 */
internal class MultiplatformConfigurator : KotlinProjectConfigurator {
    override fun configure(
        project: Project,
        extension: KotlinxSchemaExtension,
    ) {
        project.afterEvaluate {
            // Determine classpath - prefer JVM if available, otherwise JS
            val classpathConfigName =
                when {
                    project.configurations.findByName(PluginConstants.CLASSPATH_JVM) != null ->
                        PluginConstants.CLASSPATH_JVM
                    project.configurations.findByName(PluginConstants.CLASSPATH_JS) != null ->
                        PluginConstants.CLASSPATH_JS
                    else ->
                        PluginConstants.CLASSPATH_JVM // Default to JVM
                }

            val config =
                KspTaskConfig(
                    taskName = PluginConstants.KSP_TASK_COMMONMAIN,
                    compileTaskName = PluginConstants.COMPILE_TASK_METADATA,
                    sourceSets = listOf(PluginConstants.SOURCE_SET_COMMONMAIN),
                    targetSourceSet = PluginConstants.SOURCE_SET_COMMONMAIN,
                    classpathConfigName = classpathConfigName,
                    outputName = PluginConstants.SOURCE_SET_COMMONMAIN,
                )

            KspTaskBuilder(project).buildTask(config, extension)
        }
    }
}

/**
 * Configurator for Kotlin JVM projects.
 */
internal class JvmConfigurator : KotlinProjectConfigurator {
    override fun configure(
        project: Project,
        extension: KotlinxSchemaExtension,
    ) {
        project.afterEvaluate {
            val config =
                KspTaskConfig(
                    taskName = PluginConstants.KSP_TASK_KOTLIN,
                    compileTaskName = PluginConstants.COMPILE_TASK_KOTLIN,
                    sourceSets = listOf(PluginConstants.SOURCE_SET_MAIN),
                    targetSourceSet = PluginConstants.SOURCE_SET_MAIN,
                    classpathConfigName = PluginConstants.CLASSPATH_MAIN,
                    outputName = PluginConstants.SOURCE_SET_MAIN,
                )

            KspTaskBuilder(project).buildTask(config, extension)
        }
    }
}
