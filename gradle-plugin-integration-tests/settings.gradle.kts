@file:Suppress("UnstableApiUsage")

pluginManagement {
    // Include parent build to access gradle plugin
    includeBuild("..")

    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }

    plugins {
        kotlin("multiplatform") version "2.2.21"
        id("com.google.devtools.ksp") version "2.3.4"
        // kotlinx-schema plugin loaded from included parent build
        id("org.jetbrains.kotlinx.schema.ksp")
    }
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
}

// Include parent build for dependency resolution
// Gradle will automatically substitute matching group:name dependencies
includeBuild("..")
