plugins {
    kotlin("plugin.serialization") version libs.versions.kotlin apply false
    alias(libs.plugins.androidLibrary) apply false
    alias(libs.plugins.kover)
    alias(libs.plugins.detekt)
}

dependencies {
    kover(project(":kotlinx-schema-annotations"))
    kover(project(":kotlinx-schema-generator-core"))
    kover(project(":kotlinx-schema-generator-json"))
    kover(project(":kotlinx-schema-gradle-plugin"))
    kover(project(":kotlinx-schema-json"))
    kover(project(":kotlinx-schema-ksp"))
    kover(project(":ksp-integration-tests"))
}

/**
 * Publishes the gradle plugin to local maven repository and syncs the project.
 * After running this, reload your IDE/Gradle to enable gradle-plugin-integration-tests.
 */
tasks.register("publishPluginAndSync") {
    group = "build setup"
    description = "Publishes gradle plugin to local repo (run once, then reload IDE)"

    dependsOn(":kotlinx-schema-gradle-plugin:publishAllPublicationsToLocalRepository")

    doLast {
        val repoDir =
            layout.buildDirectory
                .dir("local-repo")
                .get()
                .asFile
        println("✓ Plugin published to: $repoDir")
        println()
        println("Next steps:")
        println("1. Reload Gradle/IDE to enable :gradle-plugin-integration-tests module")
        println("2. Run: ./gradlew testGradlePlugin")
        println()
        println("The integration tests module is now available and will remain enabled.")
    }
}

/**
 * Runs integration tests (requires plugin to be published first via publishPluginAndSync).
 */
tasks.register("testGradlePlugin") {
    group = "verification"
    description = "Tests the gradle plugin (publishes if needed)"

    dependsOn(":kotlinx-schema-gradle-plugin:publishAllPublicationsToLocalRepository")

    val integrationTestsProject = project.findProject(":gradle-plugin-integration-tests")
    if (integrationTestsProject != null) {
        dependsOn(":gradle-plugin-integration-tests:allTests")
    } else {
        doLast {
            throw GradleException(
                "Integration tests not available. Run './gradlew publishPluginAndSync' first and reload IDE.",
            )
        }
    }
}

subprojects {
    apply(plugin = "org.jetbrains.kotlinx.kover")
    apply {
        plugin("dev.detekt")
    }

    detekt {
        config = files("$rootDir/detekt.yml")
        buildUponDefaultConfig = true
    }
}

kover {
    reports {
        filters {
            includes.classes("kotlinx.schema.*")
            excludes.classes("kotlinx.schema.ksp.ir.*") // tested indirectly
        }
        total {
            log {
            }
            verify {
                rule {
                    minBound(72)
                }
            }
        }
    }
}
