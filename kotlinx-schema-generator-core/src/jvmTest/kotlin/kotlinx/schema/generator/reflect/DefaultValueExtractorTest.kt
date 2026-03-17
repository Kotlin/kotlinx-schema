package kotlinx.schema.generator.reflect

import io.kotest.matchers.shouldBe
import kotlin.test.Test

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

    // Nullable required param (no default) + optional param with default
    data class ClassWithNullableRequiredParam(
        val tag: String?,
        val count: Int = 5,
    )

    class UnknownType

    data class ClassWithUnknownType(
        val unknown: UnknownType,
        val name: String = "John",
    )

    @Test
    fun `extracts default value`() {
        val defaults = DefaultValueExtractor.extractDefaultValues(ClassWithDefault::class)
        defaults shouldBe mapOf("name" to "John Doe")
    }

    @Test
    fun `returns empty map when constructor fails`() {
        val defaults = DefaultValueExtractor.extractDefaultValues(ClassWithFailingConstructor::class)
        defaults shouldBe emptyMap()
    }

    @Test
    fun `extracts optional defaults when a required parameter is nullable`() {
        val defaults = DefaultValueExtractor.extractDefaultValues(ClassWithNullableRequiredParam::class)
        defaults shouldBe mapOf("count" to 5)
    }

    @Test
    fun `returns empty map when unknown non-nullable type is encountered`() {
        val defaults = DefaultValueExtractor.extractDefaultValues(ClassWithUnknownType::class)
        defaults shouldBe emptyMap()
    }
}
