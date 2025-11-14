package kotlinx.schema

import io.kotest.matchers.collections.shouldContain
import kotlin.reflect.full.declaredMembers
import kotlin.test.Test

class DescriptionAnnotationTest {
    @Schema
    @Description("Personal information")
    private data class Person(
        @property:Description("Person's first name")
        val firstName: String,
    )

    @Test
    fun `Class should have @Description annotation`() {
        val classAnnotations =
            Person::class
                .annotations
                .filterIsInstance<Description>()
        classAnnotations shouldContain Description("Personal information")
    }

    @Test
    fun `Property should have @Description annotation`() {
        val firstNameProperty = Person::class.declaredMembers.first()
        val propertyAnnotations =
            firstNameProperty.annotations
                .filterIsInstance<Description>()
        propertyAnnotations shouldContain Description("Person's first name")
    }
}
