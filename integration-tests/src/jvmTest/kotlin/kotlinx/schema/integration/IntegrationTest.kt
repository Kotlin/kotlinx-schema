package kotlinx.schema.integration

import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import kotlin.test.Test
import kotlin.test.assertEquals

class IntegrationTest {
    @Test
    fun pluginAddsJsonSchemaFieldOnCompanion() {
        // The compiler plugin should have injected a field named `jsonSchemaString` into the companion
        val companion = Sample.Companion
        val field = companion.javaClass.getDeclaredField("jsonSchemaString").apply { isAccessible = true }
        val value = field.get(companion)
        // Current plugin behavior is to set empty string; this test primarily checks the field exists

        value shouldNotBeNull {
            this shouldBe "" // todo
        }
    }
}