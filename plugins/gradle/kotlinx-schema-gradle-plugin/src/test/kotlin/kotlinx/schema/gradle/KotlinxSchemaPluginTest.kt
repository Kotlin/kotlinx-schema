package kotlinx.schema.gradle

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.gradle.testfixtures.ProjectBuilder
import kotlin.test.Test

/**
 * Unit tests for KotlinxSchemaPlugin.
 *
 * Note: Full integration testing with KSP should be done manually or in separate integration test projects
 * due to complexities with Gradle TestKit and plugin dependencies.
 */
class KotlinxSchemaPluginTest {
    @Test
    fun `plugin registers extension with default values`() {
        val project = ProjectBuilder.builder().build()
        project.pluginManager.apply("org.jetbrains.kotlin.jvm")
        project.pluginManager.apply("kotlinx.schema")

        val extension = project.extensions.getByType(KotlinxSchemaExtension::class.java)

        extension shouldNotBe null
        extension.enabled.get() shouldBe true
    }

    @Test
    fun `plugin extension allows custom configuration`() {
        val project = ProjectBuilder.builder().build()
        project.pluginManager.apply("org.jetbrains.kotlin.jvm")
        project.pluginManager.apply("kotlinx.schema")

        val extension = project.extensions.getByType(KotlinxSchemaExtension::class.java)
        extension.enabled.set(false)

        extension.enabled.get() shouldBe false
    }

    @Test
    fun `plugin can be applied to JVM project`() {
        val project = ProjectBuilder.builder().build()
        project.pluginManager.apply("org.jetbrains.kotlin.jvm")
        project.pluginManager.apply("kotlinx.schema")

        project.pluginManager.hasPlugin("kotlinx.schema") shouldBe true
    }

    @Test
    fun `plugin can be applied to multiplatform project`() {
        val project = ProjectBuilder.builder().build()
        project.pluginManager.apply("org.jetbrains.kotlin.multiplatform")
        project.pluginManager.apply("kotlinx.schema")

        project.pluginManager.hasPlugin("kotlinx.schema") shouldBe true
    }

    @Test
    fun `extension name is kotlinxSchema`() {
        val project = ProjectBuilder.builder().build()
        project.pluginManager.apply("org.jetbrains.kotlin.jvm")
        project.pluginManager.apply("kotlinx.schema")

        val extension = project.extensions.findByName("kotlinxSchema")
        extension shouldNotBe null
        extension shouldBe project.extensions.getByType(KotlinxSchemaExtension::class.java)
    }
}
