package kotlinx.schema

import io.kotest.matchers.collections.shouldHaveSize
import kotlin.test.Test

class SchemaAnnotationTest {
    @Schema
    private data class TestClass(
        val foo: String,
    )

    @Test
    fun `@Schema annotation should be erased`() {
        val classAnnotations =
            TestClass::class
                .annotations
                .filterIsInstance<Schema>()
        classAnnotations shouldHaveSize 0
    }
}
