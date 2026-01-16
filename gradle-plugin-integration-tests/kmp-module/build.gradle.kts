@file:OptIn(ExperimentalWasmDsl::class)

import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
    id("org.jetbrains.kotlinx.schema.ksp")
}

kotlinxSchema {
    enabled.set(true)
    rootPackage.set("kotlinx.schema.integration")
    withSchemaObject.set(true)
}

kotlin {
    compilerOptions {
        apiVersion = KotlinVersion.KOTLIN_2_2
        languageVersion = KotlinVersion.KOTLIN_2_2
        freeCompilerArgs.add("-Xannotation-default-target=param-property")
    }

    jvm()

    js {
        nodejs()
    }

    iosSimulatorArm64()

    wasmJs {
        nodejs()
    }

    linuxX64()

    val kotlinxSchemaVersion = project.properties["kotlinxSchemaVersion"]

    sourceSets {
        commonMain {
            dependencies {
                api(libs.kotlinx.serialization.json)
                implementation(
                    "org.jetbrains.kotlinx:kotlinx-schema-annotations:$kotlinxSchemaVersion",
                )
            }
        }

        commonTest {
            dependencies {
                implementation(kotlin("test"))
                implementation(libs.kotest.assertions.core)
                implementation(libs.kotest.assertions.json)
            }
        }
    }
}
