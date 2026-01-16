// Root project for gradle plugin integration tests
// Actual test modules are in jvm-module and kmp-module

plugins {
    base
    kotlin("plugin.serialization") version libs.versions.kotlin apply false
}

allprojects {
    group = "org.jetbrains.kotlinx"
    version = "0.1-SNAPSHOT"
}
