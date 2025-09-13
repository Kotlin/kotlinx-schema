plugins {
    kotlin("plugin.serialization") version libs.versions.kotlin apply false
    kotlin("jvm") version libs.versions.kotlin apply false
    alias(libs.plugins.androidLibrary) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.vanniktech.mavenPublish) apply false
}