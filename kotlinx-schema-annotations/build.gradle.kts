@file:OptIn(ExperimentalWasmDsl::class)

import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    kotlin("multiplatform")
    `dokka-convention`
    `publishing-convention`
}

dokka {
    dokkaSourceSets.configureEach {
    }
}

kotlin {

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

    withSourcesJar(publish = true)

    sourceSets {

        commonTest {
            dependencies {
                implementation(libs.kotlin.test)
                implementation(libs.kotest.assertions.core)
            }
        }

        jvmTest {
            dependencies {
                implementation(kotlin("reflect"))
            }
        }
    }

    explicitApi()
}
