package kotlinx.schema.json.property

import io.kotest.matchers.string.shouldContain
import kotlinx.schema.json.jsonSchema
import kotlinx.serialization.json.JsonPrimitive
import kotlin.test.Test
import kotlin.test.assertFailsWith

internal class StringPropertyValidationTest {
    @Test
    fun `string property default accepts string`() {
        jsonSchema {
            property("field") {
                string {
                    default = "valid string"
                }
            }
        }
    }

    @Test
    fun `string property default accepts JsonElement`() {
        jsonSchema {
            property("field") {
                string {
                    default = JsonPrimitive("valid")
                }
            }
        }
    }

    @Test
    fun `string property default accepts null`() {
        jsonSchema {
            property("field") {
                string {
                    default = null
                }
            }
        }
    }

    @Test
    fun `string property default rejects number`() {
        val error =
            assertFailsWith<IllegalStateException> {
                jsonSchema {
                    property("field") {
                        string {
                            default = 123
                        }
                    }
                }
            }
        error.message shouldContain "String property default"
        error.message shouldContain "Int"
    }

    @Test
    fun `string property default rejects boolean`() {
        val error =
            assertFailsWith<IllegalStateException> {
                jsonSchema {
                    property("field") {
                        string {
                            default = true
                        }
                    }
                }
            }
        error.message shouldContain "String property default"
        error.message shouldContain "Boolean"
    }

    @Test
    fun `string property default rejects arbitrary object`() {
        data class Foo(
            val x: Int,
        )

        val error =
            assertFailsWith<IllegalStateException> {
                jsonSchema {
                    property("field") {
                        string {
                            default = Foo(42)
                        }
                    }
                }
            }
        error.message shouldContain "String property default"
        error.message shouldContain "Foo"
    }

    @Test
    fun `string property constValue accepts string`() {
        jsonSchema {
            property("field") {
                string {
                    constValue = "fixed value"
                }
            }
        }
    }

    @Test
    fun `string property constValue rejects number`() {
        val error =
            assertFailsWith<IllegalStateException> {
                jsonSchema {
                    property("field") {
                        string {
                            constValue = 456
                        }
                    }
                }
            }
        error.message shouldContain "String property const"
        error.message shouldContain "Int"
    }
}
