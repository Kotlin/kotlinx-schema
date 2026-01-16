@file:Suppress("UnstableApiUsage")

val kotlinxSchemaVersion: String = providers.gradleProperty("kotlinxSchemaVersion").get()

println("ℹ️ Testing with kotlinx.schema version: $kotlinxSchemaVersion")

pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
        maven {
            url = uri("../build/project-repo")
        }
    }

    plugins {
        kotlin("jvm") version "2.2.21"
        kotlin("multiplatform") version "2.2.21"
        kotlin("plugin.serialization") version "2.2.21"
    }

    resolutionStrategy {
        eachPlugin {
            if (requested.id.id == "org.jetbrains.kotlinx.schema.ksp") {
                val kotlinxSchemaVersion: String = providers.gradleProperty("kotlinxSchemaVersion").get()
                println("✅ Resolved plugin: ${requested.id} version: $kotlinxSchemaVersion")
                useModule("org.jetbrains.kotlinx:kotlinx-schema-ksp-gradle-plugin:$kotlinxSchemaVersion")
            }
        }
    }
}

dependencyResolutionManagement {
    repositories {
        mavenCentral() // Must be first for proper Kotlin multiplatform metadata resolution
        maven {
            url = uri(rootDir.resolve("../build/project-repo"))
        }
    }
}

gradle.allprojects {
    configurations.all {
        resolutionStrategy.eachDependency {
            if (requested.group == "org.jetbrains.kotlinx" && requested.name == "kotlinx-schema-annotations") {
                useVersion(kotlinxSchemaVersion)
            }
        }
    }
}

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

// Include submodules
include(
    ":jvm-module",
    ":kmp-module",
)
