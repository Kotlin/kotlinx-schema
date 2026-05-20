plugins {
    kotlin("plugin.serialization") version libs.versions.kotlin apply false
}

/*
 * Publishes the gradle plugin to local maven repository and syncs the project.
 * After running this, reload your IDE/Gradle to enable gradle-plugin-integration-tests.
 */
tasks.register("publishPluginAndSync") {
    group = "build setup"
    description = "Publishes gradle plugin to local repo (run once, then reload IDE)"

    dependsOn(":kotlinx-schema-ksp-gradle-plugin:publishAllPublicationsToProjectRepository")

    doLast {
        val repoDir =
            layout.buildDirectory
                .dir("project-repo")
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

/*
 * Runs integration tests (requires plugin to be published first via publishPluginAndSync).
 */
tasks.register("testGradlePlugin") {
    group = "verification"
    description = "Tests the gradle plugin (publishes if needed)"

    dependsOn("publishToProjectRepo")

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
