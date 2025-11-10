package kotlinx.schema.gradle

import com.google.devtools.ksp.gradle.KspExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinProjectExtension

/**
 * Gradle plugin for generating JSON schemas using KSP.
 *
 * This plugin automatically:
 * - Applies and configures the KSP plugin
 * - Adds the kotlinx-schema-ksp processor as a KSP dependency
 * - Configures generated source directories
 * - Sets up proper task dependencies for multiplatform projects
 *
 * Usage:
 * ```kotlin
 * plugins {
 *     id("kotlinx.schema") version "x.y.z"
 * }
 *
 * kotlinxSchema {
 *     // Optional configuration
 * }
 * ```
 */
public class KotlinxSchemaPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        // Apply KSP plugin automatically
        target.pluginManager.apply("com.google.devtools.ksp")

        // Create an extension for plugin configuration
        val extension =
            target.extensions.create(
                EXTENSION_NAME,
                KotlinxSchemaExtension::class.java,
            )

        // Configure when the relevant Kotlin plugins are applied
        target.plugins.withId("org.jetbrains.kotlin.multiplatform") {
            configureKspDependencies(target, extension)
            configureMultiplatformSourceSets(target)
            configureTaskDependencies(target)
        }

        target.plugins.withId("org.jetbrains.kotlin.jvm") {
            // Avoid double-configuring if this is actually a multiplatform project
            if (!target.pluginManager.hasPlugin("org.jetbrains.kotlin.multiplatform")) {
                configureKspDependencies(target, extension)
                configureJvmSourceSets(target)
            }
        }

        // Configure KSP args after the build script had a chance to set the extension values
        target.afterEvaluate {
            configureKspArgs(target, extension)
        }
    }

    private fun configureKspArgs(
        project: Project,
        extension: KotlinxSchemaExtension,
    ) {
        if (!extension.enabled.get()) return

        val ksp = project.extensions.getByType(KspExtension::class.java)
        val root =
            extension.rootPackage.orNull
                ?.trim()
                ?.takeIf { it.isNotEmpty() }
        if (root != null) {
            // Pass the root package to KSP so the processor can filter symbols
            ksp.arg("kotlinx.schema.rootPackage", root)
        }
    }

    private fun configureKspDependencies(
        project: Project,
        extension: KotlinxSchemaExtension,
    ) {
        if (!extension.enabled.get()) {
            return
        }

        // Get the dependencies from the plugin's own buildscript configuration
        // These are already declared as api dependencies of the plugin
        // Prefer local project dependencies when developing inside the monorepo
        val kspProject = project.rootProject.findProject(":kotlinx-schema-ksp")
        val annotationsProject = project.rootProject.findProject(":kotlinx-schema-annotations")

        // Add KSP processor dependency
        val kspConfiguration =
            when {
                isMultiplatformProject(project) -> "kspCommonMainMetadata"
                else -> "ksp"
            }

        if (kspProject != null) {
            project.dependencies.add(kspConfiguration, project.project(":kotlinx-schema-ksp"))
        } else {
            project.dependencies.add(
                kspConfiguration,
                mapOf("group" to "org.jetbrains.kotlinx", "name" to "kotlinx-schema-ksp"),
            )
        }

        // Add annotations dependency to main source set
        val annotationsConfig =
            when {
                isMultiplatformProject(project) -> "commonMainImplementation"
                else -> "implementation"
            }

        if (annotationsProject != null) {
            project.dependencies.add(annotationsConfig, project.project(":kotlinx-schema-annotations"))
        } else {
            project.dependencies.add(
                annotationsConfig,
                mapOf("group" to "org.jetbrains.kotlinx", "name" to "kotlinx-schema-annotations"),
            )
        }
    }

    private fun getDependencyFromClasspath(
        classLoader: ClassLoader,
        moduleName: String,
    ): Map<String, String> {
        // Verify the dependency is on the classpath
        val className =
            when (moduleName) {
                "kotlinx-schema-ksp" -> "kotlinx.schema.ksp.SchemaExtensionProcessorProvider"
                "kotlinx-schema-annotations" -> "kotlinx.schema.Schema"
                else -> throw IllegalArgumentException("Unknown module: $moduleName")
            }

        try {
            Class.forName(className, false, classLoader)
        } catch (e: ClassNotFoundException) {
            throw IllegalStateException(
                "Could not find $moduleName on plugin classpath. " +
                    "This indicates a plugin configuration issue.",
                e,
            )
        }

        // Return dependency notation as a map that Gradle can resolve
        // Since these are api dependencies, Gradle will use the version from the plugin's dependency graph
        return mapOf(
            "group" to "org.jetbrains.kotlinx",
            "name" to moduleName,
        )
    }

    private fun configureSourceSets(project: Project) {
        when {
            isMultiplatformProject(project) -> configureMultiplatformSourceSets(project)
            isKotlinJvmProject(project) -> configureJvmSourceSets(project)
        }
    }

    private fun configureMultiplatformSourceSets(project: Project) {
        val kotlin = project.extensions.getByType(KotlinMultiplatformExtension::class.java)

        kotlin.sourceSets.named("commonMain") { sourceSet ->
            val buildDir =
                project.layout.buildDirectory
                    .get()
                    .asFile
            sourceSet.kotlin.srcDir("$buildDir/generated/ksp/metadata/commonMain/kotlin")
        }
    }

    private fun configureJvmSourceSets(project: Project) {
        val kotlin = project.extensions.getByType(KotlinProjectExtension::class.java)
        kotlin.sourceSets.named("main") { sourceSet ->
            val buildDir =
                project.layout.buildDirectory
                    .get()
                    .asFile
            sourceSet.kotlin.srcDir("$buildDir/generated/ksp/main/kotlin")
        }
    }

    private fun configureTaskDependencies(project: Project) {
        if (!isMultiplatformProject(project)) {
            return
        }

        // Set up task dependencies for multiplatform projects
        // to ensure KSP runs before platform-specific compilation
        val kspMetadataTask = "kspCommonMainKotlinMetadata"

        project.tasks.configureEach { task ->
            val name = task.name
            when {
                // Ensure all KSP per-platform tasks depend on common metadata KSP
                (name.matches(Regex("kspKotlin.*")) || name.matches(Regex("kspTestKotlin.*"))) &&
                    name != kspMetadataTask -> {
                    task.dependsOn(kspMetadataTask)
                }
            }
        }
    }

    private fun isMultiplatformProject(project: Project): Boolean =
        project.pluginManager.hasPlugin("org.jetbrains.kotlin.multiplatform")

    private fun isKotlinJvmProject(project: Project): Boolean =
        project.pluginManager.hasPlugin("org.jetbrains.kotlin.jvm")

    private companion object {
        private const val EXTENSION_NAME = "kotlinxSchema"
    }
}
