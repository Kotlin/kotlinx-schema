package kotlinx.schema.json.property

import io.kotest.matchers.string.shouldContain
import kotlinx.schema.json.jsonSchema
import kotlinx.serialization.json.JsonPrimitive
import kotlin.test.Test
import kotlin.test.assertFailsWith

internal class ArrayPropertyValidationTest {
    @Test
    fun `array property default accepts list`() {
        jsonSchema {
            property("field") {
                array {
                    default = listOf("a", "b")
                }
            }
        }
    }

    @Test
    fun `array property default accepts JsonElement`() {
        jsonSchema {
            property("field") {
                array {
                    default =
                        kotlinx.serialization.json.buildJsonArray {
                            add(JsonPrimitive(1))
                        }
                }
            }
        }
    }

    @Test
    fun `array property default accepts null`() {
        jsonSchema {
            property("field") {
                array {
                    default = null
                }
            }
        }
    }

    @Test
    fun `array property default rejects string`() {
        val error =
            assertFailsWith<IllegalStateException> {
                jsonSchema {
                    property("field") {
                        array {
                            default = "not an array"
                        }
                    }
                }
            }
        error.message shouldContain "Array property default"
        error.message shouldContain "String"
    }

    @Test
    fun `array property default rejects number`() {
        val error =
            assertFailsWith<IllegalStateException> {
                jsonSchema {
                    property("field") {
                        array {
                            default = 123
                        }
                    }
                }
            }
        error.message shouldContain "Array property default"
        error.message shouldContain "Int"
    }

    @Test
    fun `array property default rejects boolean`() {
        val error =
            assertFailsWith<IllegalStateException> {
                jsonSchema {
                    property("field") {
                        array {
                            default = true
                        }
                    }
                }
            }
        error.message shouldContain "Array property default"
        error.message shouldContain "Boolean"
    }

    @Test
    fun `array property default rejects arbitrary object`() {
        data class Foo(
            val x: Int,
        )

        val error =
            assertFailsWith<IllegalStateException> {
                jsonSchema {
                    property("field") {
                        array {
                            default = Foo(42)
                        }
                    }
                }
            }
        error.message shouldContain "Array property default"
        error.message shouldContain "Foo"
    }
}
