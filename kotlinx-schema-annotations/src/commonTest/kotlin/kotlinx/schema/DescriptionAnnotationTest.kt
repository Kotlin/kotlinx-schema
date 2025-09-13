package kotlinx.schema

import io.kotest.matchers.collections.shouldContain
import kotlinx.schema.Description
import kotlin.reflect.full.declaredMembers
import kotlin.test.Test

class DescriptionAnnotationTest {
    @Test
    fun testClassAnnotation() {
        val classAnnotations =
            Person::class
                .annotations
                .filterIsInstance<Description>()
        classAnnotations shouldContain Description("Personal information")
    }

    @Test
    fun testPropertyAnnotation() {
        val firstNameProperty = Person::class.declaredMembers.first()
        val propertyAnnotations =
            firstNameProperty.annotations
                .filterIsInstance<Description>()
        propertyAnnotations shouldContain Description("Person's first name")
    }
}