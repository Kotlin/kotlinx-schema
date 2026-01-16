package kotlinx.schema.ksp.gradle.plugin

import org.gradle.api.Project
import org.gradle.api.logging.Logger
import org.gradle.api.provider.Property
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmCompilerOptions
import org.jetbrains.kotlin.gradle.plugin.getKotlinPluginVersion
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask

/**
 * Extracts compiler configuration from a Kotlin compilation task.
 *
 * @param compileTask The Kotlin compilation task
 * @return Extracted compiler configuration as serializable data
 */
internal fun extractCompilerConfig(
    compileTask: KotlinCompilationTask<*>,
    project: Project,
): CompilerConfig {
    val options = compileTask.compilerOptions

    // Extract JVM target (only available on JVM tasks)
    val jvmTarget =
        (options as? KotlinJvmCompilerOptions)
            ?.jvmTarget
            ?.orNull
            ?.toString()
            ?.removePrefix("JVM_")
            ?: PluginConstants.DEFAULT_JVM_TARGET

    // Extract language and API versions
    val languageVersion = extractKotlinVersion(options.languageVersion, project.logger)
    val apiVersion = extractKotlinVersion(options.apiVersion, project.logger)
    val allWarningsAsErrors = options.allWarningsAsErrors.orNull ?: false

    return CompilerConfig(
        jvmTarget = jvmTarget,
        languageVersion = languageVersion,
        apiVersion = apiVersion,
        allWarningsAsErrors = allWarningsAsErrors,
    )
}

private fun extractKotlinVersion(
    property: Property<org.jetbrains.kotlin.gradle.dsl.KotlinVersion>,
    logger: Logger,
): String = property.orNull?.version ?: getKotlinPluginVersion(logger).substringBeforeLast('.')
