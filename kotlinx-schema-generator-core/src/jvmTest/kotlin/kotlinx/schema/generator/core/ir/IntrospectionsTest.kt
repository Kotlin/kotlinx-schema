package kotlinx.schema.generator.core.ir

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class IntrospectionsTest {

    //region Description extraction — simple name matching

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
            simpleName = name,
            qualifiedName = null,
            listOf(attribute to "My Description"),
        ) shouldBe "My Description"
    }

    //endregion

    //region Multi-element LLMDescription regression

    @Test
    fun `extracts description from LLMDescription with description= style when value is empty`() {
        val result =
            Introspections.getDescriptionFromAnnotation(
                simpleName = "LLMDescription",
                qualifiedName = null,
                annotationArguments = listOf("value" to "", "description" to "Product identifier"),
            )
        result shouldBe "Product identifier"
    }

    @Test
    fun `extracts description from LLMDescription with value= shorthand style`() {
        val result =
            Introspections.getDescriptionFromAnnotation(
                simpleName = "LLMDescription",
                qualifiedName = null,
                annotationArguments = listOf("value" to "Product name", "description" to ""),
            )
        result shouldBe "Product name"
    }

    @Test
    fun `attribute priority follows config order regardless of annotation argument order`() {
        // Config attribute order is: value, description
        // Annotation argument order has "description" first, but "value" should win because
        // it has higher priority in the config
        val result =
            Introspections.getDescriptionFromAnnotation(
                simpleName = "LLMDescription",
                qualifiedName = null,
                annotationArguments = listOf("description" to "lower priority", "value" to "higher priority"),
            )
        result shouldBe "higher priority"
    }

    //endregion

    //region Description negative cases

    @ParameterizedTest
    @CsvSource(
        "UnknownAnnotation, value, Some text",
        "Description, unknownAttr, Some text",
        "Description, value, ''",
    )
    fun `getDescriptionFromAnnotation returns null for non-matching cases`(
        name: String,
        attribute: String,
        text: String,
    ) {
        Introspections.getDescriptionFromAnnotation(
            simpleName = name,
            qualifiedName = null,
            annotationArguments = listOf(attribute to text),
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

    //region FQN matching — backward compatibility

    @Test
    fun `simple-name description annotation still matches when qualifiedName is provided`() {
        Introspections.getDescriptionFromAnnotation(
            simpleName = "Description",
            qualifiedName = "kotlinx.schema.Description",
            annotationArguments = listOf("value" to "test"),
        ) shouldBe "test"
    }

    @Test
    fun `simple-name ignore annotation still matches when qualifiedName is provided`() {
        Introspections.isIgnoreAnnotation(
            simpleName = "SchemaIgnore",
            qualifiedName = "kotlinx.schema.SchemaIgnore",
        ) shouldBe true
    }

    //endregion

    //region Name override extraction

    @ParameterizedTest
    @CsvSource(
        "SerialName, kotlinx.serialization.SerialName, custom_name, custom_name",
        "SerialName, kotlinx.serialization.SerialName, user_email, user_email",
    )
    fun `getNameOverride extracts value when FQN matches`(
        simpleName: String,
        qualifiedName: String,
        inputValue: String,
        expectedResult: String,
    ) {
        Introspections.getNameOverride(
            simpleName = simpleName,
            qualifiedName = qualifiedName,
            annotationArguments = listOf("value" to inputValue),
        ) shouldBe expectedResult
    }

    @ParameterizedTest
    @CsvSource(
        "SomeOther, com.example.SomeOther, custom_name",
        "SerialName, , custom_name",
        "serialname, kotlinx.serialization.serialname, custom_name",
        "SerialName, kotlinx.serialization.SerialName, ''",
    )
    fun `getNameOverride returns null for non-matching cases`(
        simpleName: String,
        qualifiedName: String?,
        inputValue: String,
    ) {
        Introspections.getNameOverride(
            simpleName = simpleName,
            qualifiedName = qualifiedName?.takeIf { it.isNotEmpty() },
            annotationArguments = listOf("value" to inputValue),
        ) shouldBe null
    }

    //endregion
}
