package kotlinx.schema.gradle

import io.kotest.matchers.file.shouldExist
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.Ignore
import kotlin.test.Test

/**
 * Tests for KotlinxSchemaPlugin behavior in Kotlin Multiplatform projects.
 *
 * Verifies:
 * - Generated code location in commonMain
 * - JVM target requirement
 * - Task dependencies
 * - Source set registration for commonMain
 */
@Ignore
class KotlinxSchemaPluginMultiplatformTest {
    @TempDir
    lateinit var testProjectDir: File

    @Test
    fun `plugin creates kspCommonMain task for multiplatform project`() {
        // Setup
        setupMultiplatformProject()
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
        result.output shouldContain "kspCommonMain"
    }

    @Test
    fun `generated code is placed in commonMain for multiplatform`() {
        // Setup
        setupMultiplatformProject()
        createSampleDataClass()

        // Execute
        val result =
            GradleRunner
                .create()
                .withProjectDir(testProjectDir)
                .withArguments("kspCommonMain", "--stacktrace")
                .withPluginClasspath()
                .build()

        // Verify generated code location matches README documentation
        val generatedDir = testProjectDir.resolve("build/generated/kotlinxSchema/commonMain/kotlin")
        generatedDir.shouldExist()

        // Verify success
        result.task(":kspCommonMain")?.outcome shouldBe TaskOutcome.SUCCESS
    }

    @Test
    fun `kspCommonMain runs before compileKotlinJvm`() {
        // Setup
        setupMultiplatformProject()
        createSampleDataClass()

        // Execute
        val result =
            GradleRunner
                .create()
                .withProjectDir(testProjectDir)
                .withArguments("compileKotlinJvm", "--dry-run")
                .withPluginClasspath()
                .build()

        // Verify task dependency
        result.output shouldContain ":kspCommonMain"
        result.output shouldContain ":compileKotlinJvm"

        // Verify kspCommonMain appears before compileKotlinJvm
        val kspIndex = result.output.indexOf(":kspCommonMain")
        val compileIndex = result.output.indexOf(":compileKotlinJvm")
        assert(kspIndex < compileIndex) { "kspCommonMain should run before compileKotlinJvm" }
    }

    @Test
    fun `plugin works with JS-only multiplatform project`() {
        // Setup - multiplatform with only JS target (no JVM required)
        val buildFile = testProjectDir.resolve("build.gradle.kts")
        buildFile.writeText(
            """
            plugins {
                kotlin("multiplatform") version "2.2.21"
                id("org.jetbrains.kotlinx.schema.ksp")
            }

            repositories {
                mavenCentral()
                mavenLocal()
            }

            kotlin {
                js(IR) {
                    nodejs()
                }

                sourceSets {
                    commonMain {
                        dependencies {
                            implementation("org.jetbrains.kotlinx:kotlinx-schema-annotations:0.0.4-SNAPSHOT")
                        }
                    }
                }
            }

            kotlinxSchema {
                enabled.set(true)
            }
            """.trimIndent(),
        )

        val settingsFile = testProjectDir.resolve("settings.gradle.kts")
        settingsFile.writeText("")

        // Create source file
        val commonMainDir = testProjectDir.resolve("src/commonMain/kotlin")
        commonMainDir.mkdirs()
        commonMainDir.resolve("Model.kt").writeText(
            """
            package com.example

            import kotlinx.schema.Schema

            @Schema
            data class Model(val value: String)
            """.trimIndent(),
        )

        // Execute
        val result =
            GradleRunner
                .create()
                .withProjectDir(testProjectDir)
                .withArguments("tasks", "--all", "--stacktrace")
                .withPluginClasspath()
                .build()

        // Verify: plugin works without JVM target
        result.output shouldContain "kspCommonMain"
    }

    @Test
    fun `plugin fails gracefully for common-only multiplatform project`() {
        // Setup - multiplatform with no specific targets (common-only)
        // This is a known limitation - plugin requires at least one platform target
        val buildFile = testProjectDir.resolve("build.gradle.kts")
        buildFile.writeText(
            """
            plugins {
                kotlin("multiplatform") version "2.2.21"
                id("org.jetbrains.kotlinx.schema.ksp")
            }

            repositories {
                mavenCentral()
                mavenLocal()
            }

            kotlin {
                // No specific targets - common-only project (unsupported)
                sourceSets {
                    commonMain {
                        dependencies {
                            implementation("org.jetbrains.kotlinx:kotlinx-schema-annotations:0.0.4-SNAPSHOT")
                        }
                    }
                }
            }

            kotlinxSchema {
                enabled.set(true)
            }
            """.trimIndent(),
        )

        val settingsFile = testProjectDir.resolve("settings.gradle.kts")
        settingsFile.writeText("")

        // Create source file
        val commonMainDir = testProjectDir.resolve("src/commonMain/kotlin")
        commonMainDir.mkdirs()
        commonMainDir.resolve("Model.kt").writeText(
            """
            package com.example

            import kotlinx.schema.Schema

            @Schema
            data class Model(val value: String)
            """.trimIndent(),
        )

        // Execute - expect this to fail because kspCommonMain task will exist but fail
        val result =
            GradleRunner
                .create()
                .withProjectDir(testProjectDir)
                .withArguments("tasks", "--all", "--stacktrace")
                .withPluginClasspath()
                .build()

        // Verify: kspCommonMain task is created (it will fail if executed)
        result.output shouldContain "kspCommonMain"
    }

    @Test
    fun `generated sources can be compiled in multiplatform project`() {
        // Setup
        setupMultiplatformProject()
        createSampleDataClass()

        // Execute - build all targets
        val result =
            GradleRunner
                .create()
                .withProjectDir(testProjectDir)
                .withArguments("build", "--stacktrace")
                .withPluginClasspath()
                .build()

        // Verify build success for all targets
        result.task(":kspCommonMain")?.outcome shouldBe TaskOutcome.SUCCESS
        result.task(":compileKotlinJvm")?.outcome shouldBe TaskOutcome.SUCCESS
        result.task(":compileKotlinJs")?.outcome shouldBe TaskOutcome.SUCCESS
        result.output shouldContain "BUILD SUCCESSFUL"
    }

    @Test
    fun `commonMain generated code is available to all targets`() {
        // Setup
        setupMultiplatformProject()
        createSampleDataClass()

        // Create test code in jvmMain that uses generated schema
        val jvmMainDir = testProjectDir.resolve("src/jvmMain/kotlin")
        jvmMainDir.mkdirs()
        jvmMainDir.resolve("UseSchema.kt").writeText(
            """
            package com.example

            fun useSchema() {
                // This should compile because generated code is in commonMain
                val schema: String = User::class.jsonSchemaString
                println(schema)
            }
            """.trimIndent(),
        )

        // Execute
        val result =
            GradleRunner
                .create()
                .withProjectDir(testProjectDir)
                .withArguments("compileKotlinJvm", "--stacktrace")
                .withPluginClasspath()
                .build()

        // Verify compilation succeeds - proves generated code is accessible
        result.task(":compileKotlinJvm")?.outcome shouldBe TaskOutcome.SUCCESS
    }

    @Test
    fun `rootPackage filtering works for multiplatform project`() {
        // Setup
        setupMultiplatformProject(rootPackage = "com.example.api")

        // Create two classes in commonMain - one in filtered package, one outside
        val commonMainDir = testProjectDir.resolve("src/commonMain/kotlin")
        commonMainDir.mkdirs()

        commonMainDir.resolve("ApiModel.kt").writeText(
            """
            package com.example.api

            import kotlinx.schema.Schema
            import kotlinx.schema.Description

            @Description("API model - should be processed")
            @Schema
            data class ApiModel(val value: String)
            """.trimIndent(),
        )

        commonMainDir.resolve("InternalModel.kt").writeText(
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
                .withArguments("kspCommonMain", "--stacktrace", "--info")
                .withPluginClasspath()
                .build()

        // Verify: should generate schema for ApiModel but not InternalModel
        val generatedDir = testProjectDir.resolve("build/generated/kotlinxSchema/commonMain/kotlin")
        val apiSchemaFile = generatedDir.resolve("com/example/api/ApiModelSchemaExtensions.kt")
        val internalSchemaFile = generatedDir.resolve("com/example/internal/InternalModelSchemaExtensions.kt")

        apiSchemaFile.shouldExist()
        assert(!internalSchemaFile.exists()) { "Internal model should be filtered out" }
    }

    private fun setupMultiplatformProject(
        rootPackage: String? = null,
        withSchemaObject: Boolean = false,
    ) {
        val buildFile = testProjectDir.resolve("build.gradle.kts")
        buildFile.writeText(
            """
            plugins {
                kotlin("multiplatform") version "2.2.21"
                id("org.jetbrains.kotlinx.schema.ksp")
            }

            repositories {
                mavenCentral()
                mavenLocal()
            }

            kotlin {
                jvm()
                js(IR) {
                    nodejs()
                }

                sourceSets {
                    commonMain {
                        dependencies {
                            implementation("org.jetbrains.kotlinx:kotlinx-schema-annotations:0.0.4-SNAPSHOT")
                            ${if (withSchemaObject) "implementation(\"org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3\")" else ""}
                        }
                    }
                }
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
        val commonMainDir = testProjectDir.resolve("src/commonMain/kotlin")
        commonMainDir.mkdirs()

        commonMainDir.resolve("User.kt").writeText(
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
