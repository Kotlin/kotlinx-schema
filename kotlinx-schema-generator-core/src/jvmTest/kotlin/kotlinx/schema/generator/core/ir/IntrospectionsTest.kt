package kotlinx.schema.generator.core.ir

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class IntrospectionsTest {
    //region Single-element annotation
    @ParameterizedTest
    @CsvSource(
        "Description, value",
        "SerialDescription, value",
        "LLMDescription, value",
        "JsonPropertyDescription, description",
        "JsonClassDescription, value",
        "P, description",
    )
    fun `extracts description from single-element annotation`(
        name: String,
        attribute: String,
    ) {
        Introspections.getDescriptionFromAnnotation(
            annotationName = name,
            listOf(attribute to "My Description"),
        ) shouldBe "My Description"
    }
    //endregion

    //region Multi-element LLMDescription regression
    @Test
    fun `extracts description from LLMDescription with description= style when value is empty`() {
        // Regression: @LLMDescription has both value and description fields;
        // when used as @LLMDescription(description = "text"), value defaults to ""
        // and should not shadow the non-empty description field.
        val result =
            Introspections.getDescriptionFromAnnotation(
                annotationName = "LLMDescription",
                annotationArguments = listOf("value" to "", "description" to "Product identifier"),
            )
        result shouldBe "Product identifier"
    }

    @Test
    fun `extracts description from LLMDescription with value= shorthand style`() {
        // @LLMDescription("Product name") sets value="Product name", description=""
        val result =
            Introspections.getDescriptionFromAnnotation(
                annotationName = "LLMDescription",
                annotationArguments = listOf("value" to "Product name", "description" to ""),
            )
        result shouldBe "Product name"
    }
    //endregion
}
