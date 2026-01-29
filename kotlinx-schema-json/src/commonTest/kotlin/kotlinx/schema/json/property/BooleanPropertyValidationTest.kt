package kotlinx.schema.json.property

import io.kotest.matchers.string.shouldContain
import kotlinx.schema.json.jsonSchema
import kotlinx.serialization.json.JsonPrimitive
import kotlin.test.Test
import kotlin.test.assertFailsWith

internal class BooleanPropertyValidationTest {
    @Test
    fun `boolean property default accepts boolean`() {
        jsonSchema {
            property("field") {
                boolean {
                    default = true
                }
            }
        }
    }

    @Test
    fun `boolean property default accepts JsonElement`() {
        jsonSchema {
            property("field") {
                boolean {
                    default = JsonPrimitive(false)
                }
            }
        }
    }

    @Test
    fun `boolean property default accepts null`() {
        jsonSchema {
            property("field") {
                boolean {
                    default = null
                }
            }
        }
    }

    @Test
    fun `boolean property default rejects string`() {
        val error =
            assertFailsWith<IllegalStateException> {
                jsonSchema {
                    property("field") {
                        boolean {
                            default = "not a boolean"
                        }
                    }
                }
            }
        error.message shouldContain "Boolean property default"
        error.message shouldContain "String"
    }

    @Test
    fun `boolean property default rejects number`() {
        val error =
            assertFailsWith<IllegalStateException> {
                jsonSchema {
                    property("field") {
                        boolean {
                            default = 123
                        }
                    }
                }
            }
        error.message shouldContain "Boolean property default"
        error.message shouldContain "Int"
    }

    @Test
    fun `boolean property constValue accepts boolean`() {
        jsonSchema {
            property("field") {
                boolean {
                    constValue = true
                }
            }
        }
    }

    @Test
    fun `boolean property constValue rejects string`() {
        val error =
            assertFailsWith<IllegalStateException> {
                jsonSchema {
                    property("field") {
                        boolean {
                            constValue = "not a boolean"
                        }
                    }
                }
            }
        error.message shouldContain "Boolean property const"
        error.message shouldContain "String"
    }

    @Test
    fun `boolean property constValue rejects number`() {
        val error =
            assertFailsWith<IllegalStateException> {
                jsonSchema {
                    property("field") {
                        boolean {
                            constValue = 123
                        }
                    }
                }
            }
        error.message shouldContain "Boolean property const"
        error.message shouldContain "Int"
    }
}
