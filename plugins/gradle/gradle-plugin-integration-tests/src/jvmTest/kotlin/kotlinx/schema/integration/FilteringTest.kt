package kotlinx.schema.integration

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FilteringTest {
    @Test
    fun `OutOfScopeModel should not have static field jsonSchemaString`() {
        val clazz = kotlinx.schema.outside.OutOfScopeModel::class.java
        val hasStaticJsonSchemaStringField =
            clazz.declaredFields.any { field ->
                field.name == "jsonSchemaString"
            }
        assertFalse(
            hasStaticJsonSchemaStringField,
            "OutOfScopeModel must not declare a static jsonSchemaString field",
        )

        val notFound =
            runCatching {
                Class.forName("kotlinx.schema.outside.ExternalModelSchemaExtensionsKt")
            }.isFailure
        assertTrue(notFound, "Expected no generated Extensions facade")
    }
}
