package kotlinx.schema.generator.core

import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class ConfigTest {
    @Test
    fun `loads annotation names from properties`() {
        Config.descriptionAnnotationNames.shouldNotBeEmpty()

        // Should include default annotations (lowercase)
        Config.descriptionAnnotationNames shouldContain "description"
        Config.descriptionAnnotationNames shouldContain "llmdescription"
        Config.descriptionAnnotationNames shouldContain "p"
    }

    @Test
    fun `loads value attributes from properties`() {
        Config.descriptionValueAttributes.shouldNotBeEmpty()

        // Should include default attributes (lowercase)
        Config.descriptionValueAttributes shouldContain "value"
        Config.descriptionValueAttributes shouldContain "description"
    }

    @Test
    fun `all values are normalized to lowercase`() {
        Config.descriptionAnnotationNames.all { it == it.lowercase() } shouldBe true
        Config.descriptionValueAttributes.all { it == it.lowercase() } shouldBe true
    }
}
