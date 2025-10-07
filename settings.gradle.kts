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
    ":kotlinx-schema-generator-core",
    ":kotlinx-schema-generator-json",
    ":kotlinx-schema-ksp",
    ":ksp-integration-tests",
    // ":kotlinx-schema-compiler-plugin",
    // ":integration-tests",
)