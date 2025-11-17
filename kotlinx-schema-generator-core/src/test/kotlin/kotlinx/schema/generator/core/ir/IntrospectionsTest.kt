package kotlinx.schema.generator.core.ir

import io.kotest.matchers.shouldBe
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class IntrospectionsTest {
    @ParameterizedTest
    @CsvSource(
        "Description, value",
        "LLMDescription, value",
        "P, description",
    )
    fun getDescriptionFromAnnotation(
        name: String,
        attribute: String,
    ) {
        Introspections.getDescriptionFromAnnotation(
            annotationName = name,
            listOf(attribute to "My Description"),
        ) shouldBe "My Description"
    }
}
