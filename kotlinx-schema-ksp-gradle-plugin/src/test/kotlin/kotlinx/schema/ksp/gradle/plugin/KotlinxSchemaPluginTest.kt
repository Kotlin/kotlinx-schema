package kotlinx.schema.ksp.gradle.plugin

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.gradle.api.internal.project.ProjectInternal
import org.gradle.testfixtures.ProjectBuilder
import kotlin.test.Test

private const val PLUGIN_ID = "org.jetbrains.kotlinx.schema.ksp"

private const val EXTENSION_NAME = "kotlinxSchema"

class KotlinxSchemaPluginTest {
    private fun setupProject(): Pair<org.gradle.api.Project, KotlinxSchemaExtension> {
        val project = ProjectBuilder.builder().build()
        project.pluginManager.apply("org.jetbrains.kotlin.jvm")
        project.pluginManager.apply(PLUGIN_ID)
        val extension = project.extensions.getByType(KotlinxSchemaExtension::class.java)
        return project to extension
    }

    //region Plugin application

    @Test
    fun `plugin can be applied to JVM project`() {
        val (project, _) = setupProject()

        project.pluginManager.hasPlugin(PLUGIN_ID) shouldBe true
    }

    @Test
    fun `plugin can be applied to multiplatform project`() {
        val project = ProjectBuilder.builder().build()
        project.pluginManager.apply("org.jetbrains.kotlin.multiplatform")
        project.pluginManager.apply(PLUGIN_ID)

        project.pluginManager.hasPlugin(PLUGIN_ID) shouldBe true
    }

    //endregion

    //region Extension registration

    @Test
    fun `plugin registers extension under correct name with defaults`() {
        val (project, extension) = setupProject()

        project.extensions.findByName(EXTENSION_NAME) shouldNotBe null
        project.extensions.findByName(EXTENSION_NAME) shouldBe extension
        extension.enabled.get() shouldBe true
    }

    //endregion

    //region Plugin behaviour

    @Test
    fun `plugin can be disabled`() {
        val (project, extension) = setupProject()
        extension.enabled.set(false)

        (project as ProjectInternal).evaluate()

        extension.enabled.get() shouldBe false
    }

    //endregion
}
