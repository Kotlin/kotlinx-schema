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

    //region Negative cases
    @Test
    fun `returns null for unrecognized annotation name`() {
        Introspections.getDescriptionFromAnnotation(
            annotationName = "UnknownAnnotation",
            annotationArguments = listOf("value" to "Some text"),
        ) shouldBe null
    }

    @Test
    fun `returns null when no recognized attribute name matches`() {
        Introspections.getDescriptionFromAnnotation(
            annotationName = "Description",
            annotationArguments = listOf("unknownAttr" to "Some text"),
        ) shouldBe null
    }

    @Test
    fun `returns null for empty string description value`() {
        Introspections.getDescriptionFromAnnotation(
            annotationName = "Description",
            annotationArguments = listOf("value" to ""),
        ) shouldBe null
    }
    //endregion

    //region Ignore annotation recognition

    @ParameterizedTest
    @CsvSource(
        "SchemaIgnore",
        "SerialSchemaIgnore",
        "JsonIgnoreType",
    )
    fun `recognizes ignore annotations by simple name`(name: String) {
        Introspections.isIgnoreAnnotation(name) shouldBe true
    }

    @ParameterizedTest
    @CsvSource(
        "schemaignore",
        "SCHEMAIGNORE",
        "SchemaIgnore",
        "jsonignoretype",
        "JSONIGNORETYPE",
    )
    fun `ignore annotation matching is case-insensitive`(name: String) {
        Introspections.isIgnoreAnnotation(name) shouldBe true
    }

    @ParameterizedTest
    @CsvSource(
        "Ignore",
        "JsonIgnore",
        "Transient",
        "Description",
        "UnknownAnnotation",
    )
    fun `does not match unrecognized annotation names as ignore`(name: String) {
        Introspections.isIgnoreAnnotation(name) shouldBe false
    }

    //endregion
}
