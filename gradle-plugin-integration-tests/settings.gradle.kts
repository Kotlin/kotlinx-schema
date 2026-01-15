@file:Suppress("UnstableApiUsage")

val kotlinxSchemaVersion: String = providers.gradleProperty("kotlinxSchemaVersion").get()

println("ℹ️ Testing with kotlinx.schema version: $kotlinxSchemaVersion")

pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
        mavenLocal()
    }

    plugins {
        kotlin("jvm") version "2.2.21"
        kotlin("multiplatform") version "2.2.21"
        kotlin("plugin.serialization") version "2.2.21"
        id("com.google.devtools.ksp") version "2.3.4"
    }

    resolutionStrategy {
        eachPlugin {
            if (requested.id.id == "org.jetbrains.kotlinx.schema.ksp") {
                val kotlinxSchemaVersion: String = providers.gradleProperty("kotlinxSchemaVersion").get()
                println("✅ Resolved plugin: ${requested.id} version: $kotlinxSchemaVersion")
                useModule("org.jetbrains.kotlinx:kotlinx-schema-gradle-plugin:$kotlinxSchemaVersion")
            }
        }
    }
}

dependencyResolutionManagement {
    repositories {
        mavenCentral() // Must be first for proper Kotlin multiplatform metadata resolution
        mavenLocal()
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
