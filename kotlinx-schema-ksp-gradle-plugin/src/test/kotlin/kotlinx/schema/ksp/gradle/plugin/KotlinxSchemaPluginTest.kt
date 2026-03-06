package kotlinx.schema.ksp.gradle.plugin

import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import org.gradle.api.internal.project.ProjectInternal
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import kotlin.test.Test

private const val PLUGIN_ID = "org.jetbrains.kotlinx.schema.ksp"

private const val EXTENSION_NAME = "kotlinxSchema"

class KotlinxSchemaPluginTest {
    private fun setupProject(platform: String = "jvm"): Pair<org.gradle.api.Project, KotlinxSchemaExtension> {
        val project = ProjectBuilder.builder().build()
        project.pluginManager.apply("org.jetbrains.kotlin.$platform")
        project.pluginManager.apply(PLUGIN_ID)
        val extension = project.extensions.getByType(KotlinxSchemaExtension::class.java)
        return project to extension
    }

    @Test
    fun `plugin registers extension with default values`() {
        val (project, _) = setupProject()

        val extension = project.extensions.getByType(KotlinxSchemaExtension::class.java)

        extension shouldNotBeNull {
            enabled.get() shouldBe true
            visibility.get() shouldBe ""
            include.isPresent shouldBe false
            exclude.isPresent shouldBe false
            rootPackage.isPresent shouldBe false
            withSchemaObject.get() shouldBe false
        }
    }

    //endregion

    //region Extension registration

    @ParameterizedTest
    @ValueSource(strings = ["multiplatform", "jvm"])
    fun `plugin can be applied to platforms`(platform: String) {
        val (project, extension) = setupProject(platform)

        project.pluginManager.hasPlugin(PLUGIN_ID) shouldBe true

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

    @Test
    fun `plugin configures ksp with rootPackage`() {
        val (project, extension) = setupProject()

        extension.rootPackage.set("com.example")

        (project as ProjectInternal).evaluate()

        extension.rootPackage.get() shouldBe "com.example"
    }

    @Test
    fun `visibility can be set to public`() {
        val (_, extension) = setupProject()

        extension.visibility.set("public")

        extension.visibility.get() shouldBe "public"
    }

    @ParameterizedTest
    @ValueSource(strings = ["internal", "public", "private", ""])
    fun `can set visibility`(visibility: String) {
        val (_, extension) = setupProject()

        extension.visibility.set(visibility)

        extension.visibility.get() shouldBe visibility
    }

    @ParameterizedTest
    @ValueSource(strings = ["foo.bar**", ""])
    fun `can set include`(value: String) {
        val (_, extension) = setupProject()

        extension.include.set(value)

        extension.include.get() shouldBe value
    }

    @ParameterizedTest
    @ValueSource(strings = ["foo.bar**", ""])
    fun `can set exclude`(value: String) {
        val (_, extension) = setupProject()

        extension.exclude.set(value)

        extension.exclude.get() shouldBe value
    }

    // endregion
}
