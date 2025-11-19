pluginManagement {
    includeBuild("plugins/gradle/kotlinx-schema-gradle-plugin")
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
    plugins {
        id("com.google.devtools.ksp") version "2.3.2"
    }
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
}

rootProject.name = "kotlinx-schema"
include(
    ":kotlinx-schema-annotations",
    ":kotlinx-schema-json",
    ":kotlinx-schema-generator-core",
    ":kotlinx-schema-generator-json",
    ":plugins:gradle:gradle-plugin-integration-tests",
    ":kotlinx-schema-ksp",
    ":ksp-integration-tests",
    ":docs",
)
