package kotlinx.schema

import io.kotest.matchers.collections.shouldContain
import kotlinx.schema.Schema
import kotlin.test.Test

class SchemaAnnotationTest {
    @Test
    fun testClassAnnotation() {
        val classAnnotations =
            Person::class
                .annotations
                .filterIsInstance<Schema>()
        classAnnotations shouldContain Schema("json")
    }
}