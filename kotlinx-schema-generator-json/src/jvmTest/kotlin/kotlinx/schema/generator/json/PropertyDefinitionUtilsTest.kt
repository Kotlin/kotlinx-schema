package kotlinx.schema.generator.json

import io.kotest.matchers.shouldBe
import kotlinx.schema.json.BooleanPropertyDefinition
import kotlinx.schema.json.NumericPropertyDefinition
import kotlinx.schema.json.StringPropertyDefinition
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlin.test.Test

class PropertyDefinitionUtilsTest {
    @Test
    fun `setDefaultValue should handle different types`() {
        val stringProp = StringPropertyDefinition()
        setDefaultValue(stringProp, "hello") shouldBe stringProp.copy(default = JsonPrimitive("hello"))

        val numProp = NumericPropertyDefinition(type = listOf("integer"))
        setDefaultValue(numProp, 42) shouldBe numProp.copy(default = JsonPrimitive(42))

        val boolProp = BooleanPropertyDefinition()
        setDefaultValue(boolProp, true) shouldBe boolProp.copy(default = JsonPrimitive(true))

        setDefaultValue(stringProp, null) shouldBe stringProp.copy(default = JsonNull)
    }

    @Test
    fun `setDescription should update description`() {
        val stringProp = StringPropertyDefinition()
        setDescription(stringProp, "desc") shouldBe stringProp.copy(description = "desc")
    }

    @Test
    fun `removeNullableFlag should set nullable to null`() {
        val stringProp = StringPropertyDefinition(nullable = true)
        removeNullableFlag(stringProp) shouldBe stringProp.copy(nullable = null)
    }
}
