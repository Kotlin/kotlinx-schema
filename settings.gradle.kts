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
    ":kotlinx-schema-generator-json",
    ":kotlinx-schema-compiler-plugin",
    ":integration-tests",
)