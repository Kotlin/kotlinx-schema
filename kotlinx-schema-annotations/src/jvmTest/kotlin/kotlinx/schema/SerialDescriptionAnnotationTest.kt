package kotlinx.schema

import io.kotest.matchers.collections.shouldContain
import kotlin.reflect.full.declaredMembers
import kotlin.test.Test

class SerialDescriptionAnnotationTest {
    @Schema
    @SerialDescription("User information")
    private data class User(
        @property:SerialDescription("User name")
        val firstName: String,
    )

    @Test
    fun `Class should have @SerialDescription annotation`() {
        val classAnnotations =
            User::class
                .annotations
                .filterIsInstance<SerialDescription>()
        classAnnotations shouldContain SerialDescription("User information")
    }

    @Test
    fun `Property should have @SerialDescription annotation`() {
        val firstNameProperty = User::class.declaredMembers.first()
        val propertyAnnotations =
            firstNameProperty.annotations
                .filterIsInstance<SerialDescription>()
        propertyAnnotations shouldContain SerialDescription("User name")
    }
}
