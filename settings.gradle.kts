@file:Suppress("UnstableApiUsage")

pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
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
    ":kotlinx-schema-ksp",
    ":kotlinx-schema-gradle-plugin",
    ":ksp-integration-tests",
    ":docs",
)
