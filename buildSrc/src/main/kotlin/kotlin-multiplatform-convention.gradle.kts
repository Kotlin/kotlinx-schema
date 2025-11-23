@file:OptIn(ExperimentalWasmDsl::class)

import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

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

    js(IR) {
        browser()
        nodejs()
    }

    wasmJs {
        binaries.library()
        browser {}
        nodejs()
    }

    macosArm64()
    iosArm64()
    iosSimulatorArm64()
}
