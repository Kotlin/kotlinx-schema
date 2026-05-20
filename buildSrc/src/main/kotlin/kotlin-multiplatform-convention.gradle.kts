@file:OptIn(ExperimentalWasmDsl::class)

import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmDefaultMode
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.abi.ExperimentalAbiValidation
import org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinJsSubTargetDsl

plugins {
    kotlin("multiplatform")
}

kotlin {

    @OptIn(ExperimentalAbiValidation::class)
    abiValidation {
        enabled = true
    }

    compilerOptions {
        allWarningsAsErrors = true
        extraWarnings = true
        freeCompilerArgs = listOf("-Xexpect-actual-classes")
    }

    withSourcesJar(publish = true)
    jvmToolchain(17)
    explicitApi()

    jvm {
        compilerOptions {
            jvmTarget = JvmTarget.JVM_17
            javaParameters = true
            jvmDefault = JvmDefaultMode.ENABLE
        }
        tasks.withType<Test>().configureEach {
            useJUnitPlatform()

            testLogging {
                exceptionFormat = TestExceptionFormat.SHORT
                events("failed")
            }
            systemProperty("kotest.output.ansi", "true")
        }
    }

    fun KotlinJsSubTargetDsl.configureJsTesting() {
        testTask {
            useMocha {
                timeout = "30s"
            }
        }
    }

    js {
        browser {
            configureJsTesting()
        }
        nodejs {
            configureJsTesting()
        }
    }

    wasmJs {
        browser()
        nodejs()
        binaries.library()
    }

    // https://kotlinlang.org/docs/native-target-support.html
    // Kotlin Native Tier 1
    macosArm64()
    iosSimulatorArm64()
    iosArm64()

    // Kotlin Native Tier 2
    linuxX64()
    linuxArm64()

    watchosSimulatorArm64()
    watchosArm32()
    watchosArm64()
    tvosSimulatorArm64()
    tvosArm64()

    // Tier 3
    mingwX64()
    iosX64()
}

tasks.withType<JavaCompile> {
    // Preserve constructor parameter names in Java
    options.compilerArgs.add("-parameters")
}
