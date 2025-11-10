@file:OptIn(ExperimentalWasmDsl::class)

import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    kotlin("multiplatform")
    alias(libs.plugins.google.ksp)
}

dependencies {
    // Only apply KSP processor to commonMain metadata to avoid duplicate generation
    add("kspCommonMainMetadata", project(":kotlinx-schema-ksp"))
}

kotlin {

    compilerOptions {
        freeCompilerArgs.add("-Xannotation-default-target=param-property")
    }

    jvm()
    js {
        nodejs()
    }
    wasmJs {
        browser()
        nodejs()
    }
    macosArm64()
    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        commonMain {
            dependencies {
                implementation(project(":kotlinx-schema-annotations"))
                implementation(libs.kotlinx.serialization.json)
                // Ensure generated sources are included in compilation
                kotlin.srcDir("build/generated/ksp/metadata/commonMain/kotlin")

                implementation(libs.kotest.assertions.core)
                implementation(libs.kotest.assertions.json)
            }
        }

        commonTest {
            dependencies {
                implementation(kotlin("test"))
            }
        }
    }
}

// Fix Gradle task dependency issues for KSP
// Use afterEvaluate to configure dependencies after KSP tasks are created
afterEvaluate {
    tasks.findByName("compileKotlinWasmJs")?.dependsOn("kspCommonMainKotlinMetadata")
    tasks.findByName("kspKotlinIosArm64")?.dependsOn("kspCommonMainKotlinMetadata")
    tasks.findByName("kspKotlinIosSimulatorArm64")?.dependsOn("kspCommonMainKotlinMetadata")
    tasks.findByName("kspKotlinJs")?.dependsOn("kspCommonMainKotlinMetadata")
    tasks.findByName("kspKotlinJvm")?.dependsOn("kspCommonMainKotlinMetadata")
    tasks.findByName("kspKotlinLinuxArm64")?.dependsOn("kspCommonMainKotlinMetadata")
    tasks.findByName("kspKotlinMacosArm64")?.dependsOn("kspCommonMainKotlinMetadata")
}
