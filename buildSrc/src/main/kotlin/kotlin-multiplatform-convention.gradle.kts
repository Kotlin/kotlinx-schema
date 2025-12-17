@file:OptIn(ExperimentalWasmDsl::class)

import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinJsBrowserDsl
import org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinJsNodeDsl
import org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinJsSubTargetDsl

plugins {
    kotlin("multiplatform")
}

kotlin {

    compilerOptions {
        allWarningsAsErrors = true
        extraWarnings = true
        freeCompilerArgs =
            listOf(
                "-Wextra",
                "-Xmulti-dollar-interpolation",
            )
    }

    withSourcesJar(publish = true)
    jvmToolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
    explicitApi()

    jvm {
        testRuns["test"].executionTask.configure {
            useJUnitPlatform()
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
        binaries.library()
        nodejs()
    }

    macosArm64()
    iosArm64()
    iosSimulatorArm64()
}
