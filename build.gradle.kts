plugins {
    kotlin("plugin.serialization") version libs.versions.kotlin apply false
    alias(libs.plugins.androidLibrary) apply false
//    alias(libs.plugins.kotlinJvm) apply false
//    alias(libs.plugins.kotlinMultiplatform) apply false
//    alias(libs.plugins.vanniktech.maven.publish) apply false
    alias(libs.plugins.kover)
    alias(libs.plugins.detekt) apply false
}

dependencies {
    kover(project(":kotlinx-schema-annotations"))
    kover(project(":kotlinx-schema-generator-core"))
    kover(project(":kotlinx-schema-generator-json"))
    kover(project(":kotlinx-schema-ksp"))
    kover(project(":ksp-integration-tests"))
}

subprojects {
    apply(plugin = "org.jetbrains.kotlinx.kover")
    apply(plugin = "io.gitlab.arturbosch.detekt")
}

kover {
    reports {
        filters {
            includes.classes("kotlinx.schema.*")
        }
        total {
            log {
            }
            verify {
                rule {
                    minBound(28)
                }
            }
        }
    }
}
