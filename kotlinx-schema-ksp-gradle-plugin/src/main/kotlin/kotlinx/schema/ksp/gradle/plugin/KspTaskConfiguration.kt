package kotlinx.schema.ksp.gradle.plugin

import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.TaskProvider
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinTarget
import java.io.File

/**
 * Configuration for a KSP task.
 */
internal data class KspTaskConfig(
    val taskName: String,
    val compileTaskName: String,
    val sourceSets: List<String>,
    val targetSourceSet: String,
    val classpathConfigName: String,
    val outputName: String,
)

/**
 * Constants used throughout the plugin.
 */
internal object PluginConstants {
    const val EXTENSION_NAME = "kotlinxSchema"
    const val KSP_CONFIGURATION = "ksp"
    const val GENERATED_BASE_PATH = "generated/kotlinxSchema"

    // Task names
    const val KSP_TASK_COMMONMAIN = "kspCommonMain"
    const val KSP_TASK_KOTLIN = "kspKotlin"
    const val COMPILE_TASK_METADATA = "compileCommonMainKotlinMetadata"
    const val COMPILE_TASK_JVM = "compileKotlinJvm"
    const val COMPILE_TASK_JS = "compileKotlinJs"
    const val COMPILE_TASK_KOTLIN = "compileKotlin"

    // Source set names
    const val SOURCE_SET_COMMONMAIN = "commonMain"
    const val SOURCE_SET_MAIN = "main"

    // Classpath configuration names
    const val CLASSPATH_JVM = "jvmCompileClasspath"
    const val CLASSPATH_JS = "jsCompileClasspath"
    const val CLASSPATH_COMMONMAIN = "commonMainCompileClasspath"
    const val CLASSPATH_MAIN = "compileClasspath"

    // JVM defaults
    const val DEFAULT_JVM_TARGET = "11"
    const val JVM_TARGET_PREFIX = "JVM_"
    const val JVM_DEFAULT_MODE = "disable"

    // Compilation names
    const val COMPILATION_MAIN = "main"
}

/**
 * Collects source directories from source sets.
 */
internal class SourceCollector(
    private val project: Project,
) {
    fun collectSourceRoots(sourceSets: List<String>): List<File> =
        sourceSets.mapNotNull { sourceSetName ->
            project.file("src/$sourceSetName/kotlin").takeIf { it.exists() }?.also {
                project.logger.info("kotlinx-schema: Adding sources from $sourceSetName")
            }
        }
}

/**
 * Registers generated sources with Gradle source sets.
 */
internal class SourceSetRegistrar(
    private val project: Project,
) {
    fun registerGeneratedSources(
        generatedDir: File,
        targetSourceSet: String,
        outputName: String,
        kspTaskProvider: TaskProvider<out Task>,
    ) {
        val multiplatformExt =
            project.extensions.findByType(KotlinMultiplatformExtension::class.java)
        val jvmExt =
            project.extensions.findByType(KotlinJvmProjectExtension::class.java)

        when {
            multiplatformExt != null -> {
                registerForMultiplatform(
                    multiplatformExt,
                    generatedDir,
                    targetSourceSet,
                    outputName,
                    kspTaskProvider,
                )
            }

            jvmExt != null -> {
                registerForJvm(jvmExt, generatedDir, targetSourceSet)
            }

            else -> {
                project.logger.warn("kotlinx-schema: Unknown Kotlin extension type, cannot add generated sources")
            }
        }
    }

    private fun registerForMultiplatform(
        extension: KotlinMultiplatformExtension,
        generatedDir: File,
        targetSourceSet: String,
        outputName: String,
        kspTaskProvider: TaskProvider<out Task>,
    ) {
        extension.sourceSets
            .findByName(targetSourceSet)
            ?.kotlin
            ?.srcDir(generatedDir)

        project.logger.info(
            "kotlinx-schema: Added generated sources to $targetSourceSet (multiplatform)",
        )

        // For commonMain, ensure all compilation tasks depend on KSP
        if (outputName == PluginConstants.SOURCE_SET_COMMONMAIN) {
            configureCommonMainDependencies(extension, kspTaskProvider)
        }
    }

    private fun configureCommonMainDependencies(
        extension: KotlinMultiplatformExtension,
        kspTaskProvider: TaskProvider<out Task>,
    ) {
        // Metadata compilation
        project.tasks.findByName(PluginConstants.COMPILE_TASK_METADATA)?.let { metadataTask ->
            metadataTask.dependsOn(kspTaskProvider)
            project.logger.info(
                "kotlinx-schema: Added KSP dependency to ${PluginConstants.COMPILE_TASK_METADATA}",
            )
        }

        // All platform compilation tasks via Kotlin Multiplatform API
        extension.targets.forEach { target ->
            configureTargetCompilations(target, kspTaskProvider)
        }
    }

    private fun configureTargetCompilations(
        target: KotlinTarget,
        kspTaskProvider: TaskProvider<out Task>,
    ) {
        target.compilations.forEach { compilation ->
            // Configure "main" compilations (they all use commonMain sources)
            // Skip test compilations
            if (compilation.name == PluginConstants.COMPILATION_MAIN) {
                compilation.compileTaskProvider.configure { compileTask ->
                    compileTask.dependsOn(kspTaskProvider)
                    project.logger.info(
                        "kotlinx-schema: Added KSP dependency to ${compileTask.name}",
                    )
                }
            }
        }
    }

    private fun registerForJvm(
        extension: KotlinJvmProjectExtension,
        generatedDir: File,
        targetSourceSet: String,
    ) {
        extension.sourceSets
            .findByName(targetSourceSet)
            ?.kotlin
            ?.srcDir(generatedDir)

        project.logger.info(
            "kotlinx-schema: Added generated sources to $targetSourceSet (jvm)",
        )
    }
}
