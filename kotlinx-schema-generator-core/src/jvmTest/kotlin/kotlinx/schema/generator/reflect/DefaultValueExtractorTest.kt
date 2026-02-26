package kotlinx.schema.generator.reflect

import io.kotest.matchers.shouldBe
import kotlin.test.Test
import kotlin.test.assertFailsWith

class DefaultValueExtractorTest {
    data class ClassWithDefault(
        val name: String = "John Doe",
    )

    data class ClassWithFailingConstructor(
        val name: String,
    ) {
        init {
            require(name.isNotBlank()) {
                "Name cannot be empty or blank"
            }
        }
    }

    @Test
    fun `extracts default value`() {
        val defaults = DefaultValueExtractor.extractDefaultValues(ClassWithDefault::class)
        defaults shouldBe mapOf("name" to "John Doe")
    }

    @Test
    fun `fails with exception when constructor fails`() {
        assertFailsWith<IllegalArgumentException> {
            DefaultValueExtractor.extractDefaultValues(ClassWithFailingConstructor::class)
        }
    }

    class UnknownType

    data class ClassWithUnknownType(
        val unknown: UnknownType,
        val name: String = "John",
    )

    @Test
    fun `returns empty map when unknown non-nullable type is encountered`() {
        val defaults = DefaultValueExtractor.extractDefaultValues(ClassWithUnknownType::class)
        defaults shouldBe emptyMap()
    }
}
