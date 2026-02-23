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
        freeCompilerArgs =
            listOf(
                "-Wextra",
                "-Xmulti-dollar-interpolation",
                "-Xexpect-actual-classes",
            )
    }

    withSourcesJar(publish = true)
    jvmToolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
    explicitApi()

    jvm {
        compilerOptions {
            jvmTarget = JvmTarget.JVM_17
            javaParameters = true
            jvmDefault = JvmDefaultMode.ENABLE
            freeCompilerArgs.addAll(
                "-Xdebug",
            )
        }
        testRuns["test"].executionTask.configure {
            useJUnitPlatform()

            testLogging {
                exceptionFormat = TestExceptionFormat.SHORT
                events("failed")
            }
            systemProperty("kotest.output.ansi", "true")
            reports {
                junitXml.required.set(true)
                junitXml.includeSystemOutLog.set(true)
                junitXml.includeSystemErrLog.set(true)
            }
        }
    }

    fun KotlinJsSubTargetDsl.configureJsTesting() {
        testTask {
            useMocha {
                timeout = "30s"
            }
        }
    }

    js(IR) {
        browser {
            configureJsTesting()
        }
        nodejs {
            configureJsTesting()
        }
    }

    wasmJs {
        browser()
        binaries.library()
        nodejs()
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
    watchosX64()
    tvosSimulatorArm64()
    tvosArm64()

    // Tier 3
    mingwX64()
    // androidNativeArm32()
    // androidNativeArm64()
    // androidNativeX86()
    // androidNativeX64()
    macosX64()
    iosX64()
    tvosX64()
}

tasks.named("detekt").configure {
    dependsOn("detektMainJvm", "detektTestJvm")
}

tasks.withType<JavaCompile> {
    // Preserve constructor parameter names in Java
    options.compilerArgs.add("-parameters")
}
