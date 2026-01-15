package kotlinx.schema.gradle

import io.kotest.matchers.file.shouldExist
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.Test

/**
 * Tests for KotlinxSchemaPlugin behavior in JVM-only projects.
 *
 * Verifies:
 * - Generated code location
 * - Source set registration
 * - Task dependencies
 * - Package filtering
 */
class KotlinxSchemaPluginJvmTest {
    @TempDir
    lateinit var testProjectDir: File

    @Test
    fun `plugin creates kspKotlin task for JVM project`() {
        // Setup
        setupJvmProject()
        createSampleDataClass() // Must have sources for task to be created

        // Execute
        val result =
            GradleRunner
                .create()
                .withProjectDir(testProjectDir)
                .withArguments("tasks", "--all", "--stacktrace")
                .withPluginClasspath()
                .build()

        // Verify task exists
        result.output shouldContain "kspKotlin"
    }

    @Test
    fun `generated code is placed in correct location for JVM`() {
        // Setup
        setupJvmProject()
        createSampleDataClass()

        // Execute
        val result =
            GradleRunner
                .create()
                .withProjectDir(testProjectDir)
                .withArguments("kspKotlin", "--stacktrace")
                .withPluginClasspath()
                .build()

        // Verify generated code location matches README documentation
        val generatedDir = testProjectDir.resolve("build/generated/kotlinxSchema/main/kotlin")
        generatedDir.shouldExist()

        // Verify success
        result.task(":kspKotlin")?.outcome shouldBe TaskOutcome.SUCCESS
    }

    @Test
    fun `kspKotlin task runs before compileKotlin`() {
        // Setup
        setupJvmProject()
        createSampleDataClass()

        // Execute
        val result =
            GradleRunner
                .create()
                .withProjectDir(testProjectDir)
                .withArguments("compileKotlin", "--dry-run")
                .withPluginClasspath()
                .build()

        // Verify task dependency (dry-run shows task order)
        result.output shouldContain ":kspKotlin"
        result.output shouldContain ":compileKotlin"

        // Verify kspKotlin appears before compileKotlin in output
        val kspIndex = result.output.indexOf(":kspKotlin")
        val compileIndex = result.output.indexOf(":compileKotlin")
        assert(kspIndex < compileIndex) { "kspKotlin should run before compileKotlin" }
    }

    @Test
    fun `generated sources can be compiled in JVM project`() {
        // Setup
        setupJvmProject()
        createSampleDataClass()

        // Execute - build should succeed including compilation of generated code
        val result =
            GradleRunner
                .create()
                .withProjectDir(testProjectDir)
                .withArguments("build", "--stacktrace")
                .withPluginClasspath()
                .build()

        // Verify build success
        result.task(":kspKotlin")?.outcome shouldBe TaskOutcome.SUCCESS
        result.task(":compileKotlin")?.outcome shouldBe TaskOutcome.SUCCESS
        result.output shouldContain "BUILD SUCCESSFUL"
    }

    @Test
    fun `rootPackage filtering works for JVM project`() {
        // Setup
        setupJvmProject(rootPackage = "com.example.api")

        // Create two classes - one in filtered package, one outside
        val srcDir = testProjectDir.resolve("src/main/kotlin")
        srcDir.mkdirs()

        srcDir.resolve("ApiModel.kt").writeText(
            """
            package com.example.api

            import kotlinx.schema.Schema
            import kotlinx.schema.Description

            @Description("API model - should be processed")
            @Schema
            data class ApiModel(val value: String)
            """.trimIndent(),
        )

        srcDir.resolve("InternalModel.kt").writeText(
            """
            package com.example.internal

            import kotlinx.schema.Schema
            import kotlinx.schema.Description

            @Description("Internal model - should be filtered out")
            @Schema
            data class InternalModel(val value: String)
            """.trimIndent(),
        )

        // Execute
        val result =
            GradleRunner
                .create()
                .withProjectDir(testProjectDir)
                .withArguments("kspKotlin", "--stacktrace", "--info")
                .withPluginClasspath()
                .build()

        // Verify: should generate schema for ApiModel but not InternalModel
        val generatedDir = testProjectDir.resolve("build/generated/kotlinxSchema/main/kotlin")
        val apiSchemaFile = generatedDir.resolve("com/example/api/ApiModelSchemaExtensions.kt")
        val internalSchemaFile = generatedDir.resolve("com/example/internal/InternalModelSchemaExtensions.kt")

        apiSchemaFile.shouldExist()
        assert(!internalSchemaFile.exists()) { "Internal model should be filtered out" }
    }

    private fun setupJvmProject(
        rootPackage: String? = null,
        withSchemaObject: Boolean = false,
    ) {
        val buildFile = testProjectDir.resolve("build.gradle.kts")
        buildFile.writeText(
            """
            plugins {
                kotlin("jvm") version "2.2.21"
                id("org.jetbrains.kotlinx.schema.ksp")
            }

            repositories {
                mavenCentral()
                mavenLocal()
            }

            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-schema-annotations:0.0.4-SNAPSHOT")
                ${if (withSchemaObject) "implementation(\"org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3\")" else ""}
            }

            kotlinxSchema {
                enabled.set(true)
                ${if (rootPackage != null) "rootPackage.set(\"$rootPackage\")" else ""}
                ${if (withSchemaObject) "withSchemaObject.set(true)" else ""}
            }
            """.trimIndent(),
        )

        val settingsFile = testProjectDir.resolve("settings.gradle.kts")
        settingsFile.writeText("")
    }

    private fun createSampleDataClass() {
        val srcDir = testProjectDir.resolve("src/main/kotlin")
        srcDir.mkdirs()

        srcDir.resolve("User.kt").writeText(
            """
            package com.example

            import kotlinx.schema.Schema
            import kotlinx.schema.Description

            @Description("A user in the system")
            @Schema
            data class User(
                @Description("User ID")
                val id: Long,

                @Description("Email address")
                val email: String
            )
            """.trimIndent(),
        )
    }
}
