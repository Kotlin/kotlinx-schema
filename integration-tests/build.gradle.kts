import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

plugins {
    alias(libs.plugins.kotlinMultiplatform)
}

kotlin {
    jvm {
        testRuns["test"].executionTask.configure {
            useJUnitPlatform()
        }
    }

    sourceSets {
        commonMain {
            dependencies {}
        }
        commonTest {
            dependencies {
                implementation(libs.kotlin.test)
                implementation(libs.kotest.assertions.core)
                implementation(libs.kotest.assertions.json)
            }
        }
        jvmMain {
            dependencies {
                implementation(project(":kotlinx-schema-annotations"))
            }
        }
        jvmTest {
            dependencies {
                implementation(kotlin("reflect"))
            }
        }
    }
}

// Wire the compiler plugin into JVM compilations
val schemaCompilerPlugin by configurations.creating
configurations.named(schemaCompilerPlugin.name) {
    isTransitive = false
}

dependencies {
    // Use the main runtimeElements variant to get exactly the main JAR as a single file
    schemaCompilerPlugin(project(path = ":kotlinx-schema-compiler-plugin", configuration = "runtimeElements"))
}

// Pass the plugin JAR to the Kotlin JVM compiler using the compilerOptions DSL
// The provider below will trigger building the plugin JAR when needed.
tasks.withType(KotlinCompile::class).configureEach {
    val pluginArg =
        project.provider {
            val pluginJar = schemaCompilerPlugin.singleFile
            "-Xplugin=${pluginJar.absolutePath}"
        }
    compilerOptions.freeCompilerArgs.add(pluginArg)
}