package kotlinx.schema.ksp.gradle.plugin

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.gradle.api.internal.project.ProjectInternal
import org.gradle.testfixtures.ProjectBuilder
import kotlin.test.Test

private const val PLUGIN_ID = "org.jetbrains.kotlinx.schema.ksp"

private const val EXTENSION_NAME = "kotlinxSchema"

/**
 * Unit tests for KotlinxSchemaPlugin.
 */
class KotlinxSchemaPluginTest {
    /**
     * Sets up a test project with the Kotlin JVM and kotlinx-schema plugins applied.
     */
    private fun setupProject(): Pair<org.gradle.api.Project, KotlinxSchemaExtension> {
        val project = ProjectBuilder.builder().build()
        project.pluginManager.apply("org.jetbrains.kotlin.jvm")
        project.pluginManager.apply(PLUGIN_ID)
        val extension = project.extensions.getByType(KotlinxSchemaExtension::class.java)
        return project to extension
    }

    @Test
    fun `plugin registers extension with default values`() {
        val project = ProjectBuilder.builder().build()
        project.pluginManager.apply("org.jetbrains.kotlin.jvm")
        project.pluginManager.apply(PLUGIN_ID)

        val extension = project.extensions.getByType(KotlinxSchemaExtension::class.java)

        extension shouldNotBe null
        extension.enabled.get() shouldBe true
    }

    @Test
    fun `plugin extension allows custom configuration`() {
        val project = ProjectBuilder.builder().build()
        project.pluginManager.apply("org.jetbrains.kotlin.jvm")
        project.pluginManager.apply(PLUGIN_ID)

        val extension = project.extensions.getByType(KotlinxSchemaExtension::class.java)
        extension.enabled.set(false)

        extension.enabled.get() shouldBe false
    }

    @Test
    fun `plugin can be applied to JVM project`() {
        val project = ProjectBuilder.builder().build()
        project.pluginManager.apply("org.jetbrains.kotlin.jvm")
        project.pluginManager.apply(PLUGIN_ID)

        project.pluginManager.hasPlugin(PLUGIN_ID) shouldBe true
    }

    @Test
    fun `plugin can be applied to multiplatform project`() {
        val project = ProjectBuilder.builder().build()
        project.pluginManager.apply("org.jetbrains.kotlin.multiplatform")
        project.pluginManager.apply(PLUGIN_ID)

        project.pluginManager.hasPlugin(PLUGIN_ID) shouldBe true
    }

    @Test
    fun `Check extension name`() {
        val project = ProjectBuilder.builder().build()
        project.pluginManager.apply("org.jetbrains.kotlin.jvm")
        project.pluginManager.apply(PLUGIN_ID)

        val extension = project.extensions.findByName(EXTENSION_NAME)
        extension shouldNotBe null
        extension shouldBe project.extensions.getByType(KotlinxSchemaExtension::class.java)
    }

    @Test
    fun `plugin can be disabled`() {
        val project = ProjectBuilder.builder().build()
        project.pluginManager.apply("org.jetbrains.kotlin.jvm")
        project.pluginManager.apply(PLUGIN_ID)

        val extension = project.extensions.getByType(KotlinxSchemaExtension::class.java)
        extension.enabled.set(false)

        (project as ProjectInternal).evaluate()

        extension.enabled.get() shouldBe false
    }

    @Test
    fun `plugin configures ksp with rootPackage`() {
        val project = ProjectBuilder.builder().build()
        project.pluginManager.apply("org.jetbrains.kotlin.jvm")
        project.pluginManager.apply(PLUGIN_ID)

        val extension = project.extensions.getByType(KotlinxSchemaExtension::class.java)
        extension.rootPackage.set("com.example")

        (project as ProjectInternal).evaluate()

        extension.rootPackage.get() shouldBe "com.example"
    }

    @Test
    fun `visibility property has correct default value (empty string)`() {
        val (_, extension) = setupProject()

        extension.visibility.get() shouldBe ""
    }

    @Test
    fun `visibility can be set to public`() {
        val (_, extension) = setupProject()

        extension.visibility.set("public")

        extension.visibility.get() shouldBe "public"
    }

    @Test
    fun `visibility can be set to internal`() {
        val (_, extension) = setupProject()

        extension.visibility.set("internal")

        extension.visibility.get() shouldBe "internal"
    }

    @Test
    fun `visibility can be set to private`() {
        val (_, extension) = setupProject()

        extension.visibility.set("private")

        extension.visibility.get() shouldBe "private"
    }

    @Test
    fun `visibility can be set to empty string explicitly`() {
        val (_, extension) = setupProject()

        extension.visibility.set("")

        extension.visibility.get() shouldBe ""
    }
}
