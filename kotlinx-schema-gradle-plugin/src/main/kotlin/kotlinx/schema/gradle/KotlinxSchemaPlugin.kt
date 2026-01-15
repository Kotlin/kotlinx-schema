package kotlinx.schema.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.logging.LogLevel

private const val KOTLIN_MULTIPLATFORM_PLUGIN = "org.jetbrains.kotlin.multiplatform"
private const val KOTLIN_JVM_PLUGIN = "org.jetbrains.kotlin.jvm"

/**
 * Gradle plugin for generating JSON schemas using KSP2.
 *
 * This plugin invokes KSP2 programmatically without applying the Google KSP Gradle plugin.
 * It configures KSP tasks for each Kotlin compilation and generates schema extension properties.
 *
 * The processor generates extension properties on KClass that provide JSON schema strings:
 * - `KClass<T>.jsonSchemaString: String` - JSON schema as a string
 * - `KClass<T>.jsonSchema: JsonObject` - JSON schema as a JsonObject
 *
 * Generated files are placed under `build/generated/kotlinxSchema/`.
 *
 * Usage:
 * ```kotlin
 * plugins {
 *     kotlin("multiplatform") version "2.2.21"
 *     id("org.jetbrains.kotlinx.schema.ksp") version "0.0.4"
 * }
 *
 * kotlinxSchema {
 *     enabled.set(true)  // Default: true
 *     rootPackage.set("com.example")  // Optional: filter by package
 *     withSchemaObject.set(true)  // Default: false, enables jsonSchema JsonObject property
 * }
 * ```
 */
public class KotlinxSchemaPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        val extension =
            target.extensions.create(
                PluginConstants.EXTENSION_NAME,
                KotlinxSchemaExtension::class.java,
            )

        if (extension.enabled.orNull == false) {
            target.logger.log(LogLevel.DEBUG, "kotlinx-schema: Plugin is disabled")
            return
        }

        // Detect and configure based on Kotlin plugin type
        when {
            target.plugins.hasPlugin(KOTLIN_MULTIPLATFORM_PLUGIN) -> {
                configureMultiplatform(target, extension)
            }

            else -> {
                target.plugins.withId(KOTLIN_JVM_PLUGIN) {
                    configureJvm(target, extension)
                }
            }
        }
    }

    private fun configureMultiplatform(
        project: Project,
        extension: KotlinxSchemaExtension,
    ) {
        project.logger.log(LogLevel.INFO, "kotlinx-schema: Detected Kotlin Multiplatform")
        MultiplatformConfigurator().configure(project, extension)
    }

    private fun configureJvm(
        project: Project,
        extension: KotlinxSchemaExtension,
    ) {
        project.afterEvaluate {
            if (!project.plugins.hasPlugin(KOTLIN_MULTIPLATFORM_PLUGIN)) {
                project.logger.log(LogLevel.INFO, "kotlinx-schema: Detected Kotlin JVM")
                JvmConfigurator().configure(project, extension)
            }
        }
    }
}
