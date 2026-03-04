package kotlinx.schema.ksp.gradle.plugin

import io.kotest.matchers.maps.shouldContain
import io.kotest.matchers.maps.shouldNotContainKey
import org.gradle.api.internal.project.ProjectInternal
import org.gradle.testfixtures.ProjectBuilder
import kotlin.test.Test

/**
 * Verifies that [KspTaskBuilder] maps [KotlinxSchemaExtension] properties to the correct
 * KSP processor options on the registered [KspTask].
 *
 * Each test creates a project with a real `src/main/kotlin` source directory so that
 * [SourceCollector] finds sources and [KspTaskBuilder.buildTask] registers the task.
 * Extension properties are configured before [ProjectInternal.evaluate], at which point
 * the lazy task configuration lambda runs and [KspTask.processorOptions] is resolved.
 */
class KspTaskBuilderTest {
    private fun setupProject(configure: KotlinxSchemaExtension.() -> Unit = {}): Map<String, String> {
        val project = ProjectBuilder.builder().build()
        project.file("src/main/kotlin").mkdirs()
        project.pluginManager.apply("org.jetbrains.kotlin.jvm")
        project.pluginManager.apply("org.jetbrains.kotlinx.schema.ksp")
        project.extensions.getByType(KotlinxSchemaExtension::class.java).configure()
        (project as ProjectInternal).evaluate()
        val kspTask = project.tasks.getByName(PluginConstants.KSP_TASK_KOTLIN) as KspTask
        return kspTask.processorOptions.get()
    }

    //region enabled

    @Test
    fun `enabled defaults to true in processor options`() {
        setupProject() shouldContain (PluginConstants.OPTION_ENABLED to "true")
    }

    @Test
    fun `enabled false is passed as processor option`() {
        setupProject { enabled.set(false) } shouldContain (PluginConstants.OPTION_ENABLED to "false")
    }

    //endregion

    //region rootPackage

    @Test
    fun `rootPackage is absent from options when not set`() {
        setupProject() shouldNotContainKey PluginConstants.OPTION_ROOT_PACKAGE
    }

    @Test
    fun `rootPackage is passed as processor option when set`() {
        setupProject { rootPackage.set("com.example") } shouldContain
            (PluginConstants.OPTION_ROOT_PACKAGE to "com.example")
    }

    //endregion

    //region include / exclude

    @Test
    fun `include is absent from options when not set`() {
        setupProject() shouldNotContainKey PluginConstants.OPTION_INCLUDE
    }

    @Test
    fun `exclude is absent from options when not set`() {
        setupProject() shouldNotContainKey PluginConstants.OPTION_EXCLUDE
    }

    @Test
    fun `include is passed as processor option when set`() {
        setupProject { include.set("com.example.api.**") } shouldContain
            (PluginConstants.OPTION_INCLUDE to "com.example.api.**")
    }

    @Test
    fun `exclude is passed as processor option when set`() {
        setupProject { exclude.set("**.internal.**") } shouldContain
            (PluginConstants.OPTION_EXCLUDE to "**.internal.**")
    }

    //endregion

    //region always-present options

    @Test
    fun `withSchemaObject defaults to false in processor options`() {
        setupProject() shouldContain (PluginConstants.OPTION_WITH_SCHEMA_OBJECT to "false")
    }

    @Test
    fun `visibility defaults to empty string in processor options`() {
        setupProject() shouldContain (PluginConstants.OPTION_VISIBILITY to "")
    }

    //endregion
}
