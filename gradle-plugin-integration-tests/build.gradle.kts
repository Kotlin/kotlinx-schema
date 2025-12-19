@file:OptIn(ExperimentalWasmDsl::class)

import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    kotlin("multiplatform")
    id("org.jetbrains.kotlinx.schema.ksp")
}

// Configure the kotlinx-schema plugin
kotlinxSchema {
    rootPackage.set("kotlinx.schema.integration")
}

// Configure KSP arguments
ksp {
    arg("kotlinx.schema.withSchemaObject", "true")
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
                // Gradle automatically substitutes this with the project from the included build
                implementation(libs.kotlinx.schema.annotations)
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
