@file:OptIn(ExperimentalWasmDsl::class)

import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    kotlin("multiplatform")
    id("org.jetbrains.kotlinx.schema.ksp")
}

kotlinxSchema {
    rootPackage.set("kotlinx.schema.integration")
}

kotlin {
    compilerOptions {
        freeCompilerArgs.add("-Xannotation-default-target=param-property")
    }

    jvm()
    js(IR) {
        nodejs()
    }
    iosSimulatorArm64()

    sourceSets {
        commonMain {
            dependencies {
                implementation(libs.kotlinx.serialization.json)
                implementation(project(":kotlinx-schema-annotations"))
            }
        }

        commonTest {
            dependencies {
                implementation(kotlin("test"))
                implementation(libs.kotest.assertions.core)
                implementation(libs.kotest.assertions.json)
            }
        }

        jvmMain {
            dependencies {
                // Third-party annotation libraries for testing description extraction
                implementation(libs.jackson.annotations)
                implementation(libs.langchain4j.core)
            }
        }
    }
}
