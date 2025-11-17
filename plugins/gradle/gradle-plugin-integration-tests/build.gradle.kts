@file:OptIn(ExperimentalWasmDsl::class)

import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    kotlin("multiplatform")
    id("kotlinx.schema")
}

kotlinxSchema {
    rootPackage.set("kotlinx.schema.integration")
}

kotlin {
    compilerOptions {
        freeCompilerArgs.add("-Xannotation-default-target=param-property")
    }

    // For plugin integration tests simplification, only configure JVM target.
    // This ensures both common and JVM schemas are generated in JVM source route,
    // and avoids running kspCommonMainMetadata/deduplication in this project.
    jvm()
    js {
        nodejs()
    }
    wasmJs {
        browser()
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
