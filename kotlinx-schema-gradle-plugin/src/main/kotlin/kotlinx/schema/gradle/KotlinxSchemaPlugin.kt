package kotlinx.schema.gradle

import com.google.devtools.ksp.gradle.KspExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.logging.LogLevel
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinProjectExtension

/**
 * Gradle plugin for generating JSON schemas using KSP.
 *
 * This plugin automatically:
 * - Applies and configures the KSP plugin
 * - Adds the kotlinx-schema-ksp processor as a KSP dependency
 * - Configures generated source directories
 * - Sets up proper task dependencies
 *
 * The plugin supports both JVM-only and Kotlin Multiplatform projects.
 * For multiplatform projects, schema generation occurs only in commonMain.
 *
 * Usage:
 * ```kotlin
 * plugins {
 *     kotlin("jvm") version "2.2.21"
 *     id("org.jetbrains.kotlinx.schema.ksp") version "0.1.0"
 * }
 *
 * kotlinxSchema {
 *     enabled.set(true)              // Optional, defaults to true
 *     rootPackage.set("com.example") // Optional package filter
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

        // Configure when the relevant Kotlin plugins are applied.
        //
        // Important: a project might apply the JVM plugin *before* the Multiplatform plugin.
        // If we configured JVM immediately in that case, and later the Multiplatform plugin
        // is applied, we'd end up configuring both JVM and Multiplatform, which we must avoid.
        //
        // Strategy:
        // - As soon as the Multiplatform plugin is applied, we configure it eagerly.
        // - For the JVM plugin we only *record the intent* and defer actual configuration
        //   until afterEvaluate, where we know whether Multiplatform is present or not.

        // Multiplatform configuration is always preferred when available.
        target.plugins.withId("org.jetbrains.kotlin.multiplatform") {
            configureKspDependencies(target, extension)
            configureMultiplatformSourceSets(target)
            configureTaskDependencies(target)
        }

        var shouldConfigureJvmAfterEvaluate = false

        target.plugins.withId("org.jetbrains.kotlin.jvm") {
            // Mark that JVM is present; actual configuration is deferred until afterEvaluate
            // so we can see whether Multiplatform ends up being applied as well.
            shouldConfigureJvmAfterEvaluate = true
        }

        // Decide which configuration to use once the build script is fully evaluated.
        target.afterEvaluate {
            // If MPP is applied, it "wins" and JVM-specific configuration must be skipped.
            if (isMultiplatformProject(target)) return@afterEvaluate

            if (shouldConfigureJvmAfterEvaluate) {
                configureKspDependencies(target, extension)
                configureJvmSourceSets(target)
            }
        }

        // Configure KSP args after the build script had a chance to set the extension values
        target.afterEvaluate {
            configureKspArgs(target, extension)
        }
    }

    /**
     * Configures KSP arguments based on the extension configuration.
     * Must be called in afterEvaluate to ensure extension values are set.
     */
    private fun configureKspArgs(
        project: Project,
        extension: KotlinxSchemaExtension,
    ) {
        if (!extension.enabled.get()) {
            project.logger.log(LogLevel.INFO, "Kotlin schema plugin is disabled")
            return
        }

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

    /**
     * Adds the kotlinx-schema-ksp processor as a dependency.
     *
     * For multiplatform projects, adds dependency only to "kspCommonMainMetadata".
     * For JVM projects, adds to "ksp" configuration.
     *
     * Prefers local project dependencies when running inside the monorepo.
     */
    private fun configureKspDependencies(
        project: Project,
        extension: KotlinxSchemaExtension,
    ) {
        if (!extension.enabled.get()) {
            project.logger.log(LogLevel.DEBUG, "Kotlin schema plugin is disabled")
            return
        }

        // Prefer local project dependencies when developing inside the monorepo
        val kspProject = project.rootProject.findProject(":kotlinx-schema-ksp")
        val kspDependency =
            if (kspProject != null) {
                project.project(":kotlinx-schema-ksp")
            } else {
                // External dependency - version will be resolved from published plugin
                mapOf("group" to "org.jetbrains.kotlinx", "name" to "kotlinx-schema-ksp")
            }

        // For multiplatform projects, configure KSP only for commonMain metadata
        if (isMultiplatformProject(project)) {
            project.dependencies.add("kspCommonMainMetadata", kspDependency)
            project.logger.log(
                LogLevel.INFO,
                "kotlinx-schema: Configured KSP for commonMain metadata only",
            )
        } else {
            // For single-target JVM projects
            project.dependencies.add("ksp", kspDependency)
        }
    }

    /**
     * Configures source sets for multiplatform projects.
     *
     * Adds the KSP-generated directory to commonMain, making schemas available
     * to all platform targets without redundant generation.
     */
    private fun configureMultiplatformSourceSets(project: Project) {
        val kotlin = project.extensions.getByType(KotlinMultiplatformExtension::class.java)
        // Configure commonMain generated sources only
        kotlin.sourceSets.named("commonMain") { sourceSet ->
            val dirProvider = project.layout.buildDirectory.dir("generated/ksp/metadata/commonMain/kotlin")
            sourceSet.kotlin.srcDir(dirProvider)
            project.logger.log(
                LogLevel.DEBUG,
                "kotlinx-schema: Registered generated sources dir for 'commonMain' -> ${dirProvider.get().asFile}",
            )
        }
    }

    /**
     * Configures source sets for JVM-only projects.
     *
     * Adds the KSP-generated directory to the main source set.
     */
    private fun configureJvmSourceSets(project: Project) {
        val kotlin = project.extensions.getByType(KotlinProjectExtension::class.java)
        kotlin.sourceSets.named("main") { sourceSet ->
            // Use Provider API so it works even if the directory is created later
            val dirProvider = project.layout.buildDirectory.dir("generated/ksp/main/kotlin")
            sourceSet.kotlin.srcDir(dirProvider)
            project.logger.log(
                LogLevel.DEBUG,
                "kotlinx-schema: Registered generated sources dir for JVM 'main' -> ${dirProvider.get().asFile}",
            )
        }
    }

    /**
     * Configures task dependencies for multiplatform projects.
     *
     * Ensures KSP runs before platform-specific compilation by setting up proper task ordering.
     * This prevents "task uses output without declaring dependency" warnings.
     */
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

    private companion object {
        private const val EXTENSION_NAME = "kotlinxSchema"
    }
}
