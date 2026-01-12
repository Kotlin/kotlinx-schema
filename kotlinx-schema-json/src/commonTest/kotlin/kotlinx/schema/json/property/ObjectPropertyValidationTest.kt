package kotlinx.schema.json.property

import io.kotest.matchers.string.shouldContain
import kotlinx.schema.json.jsonSchema
import kotlinx.serialization.json.JsonPrimitive
import kotlin.test.Test
import kotlin.test.assertFailsWith

internal class ObjectPropertyValidationTest {
    @Test
    fun `object property default accepts map`() {
        jsonSchema {
            name = "Test"
            schema {
                property("field") {
                    obj {
                        default = mapOf("a" to 1, "b" to 2)
                    }
                }
            }
        }
    }

    @Test
    fun `object property default accepts JsonElement`() {
        jsonSchema {
            name = "Test"
            schema {
                property("field") {
                    obj {
                        default =
                            kotlinx.serialization.json.buildJsonObject {
                                put("x", JsonPrimitive(10))
                            }
                    }
                }
            }
        }
    }

    @Test
    fun `object property default accepts null`() {
        jsonSchema {
            name = "Test"
            schema {
                property("field") {
                    obj {
                        default = null
                    }
                }
            }
        }
    }

    @Test
    fun `object property default rejects string`() {
        val error =
            assertFailsWith<IllegalStateException> {
                jsonSchema {
                    name = "Test"
                    schema {
                        property("field") {
                            obj {
                                default = "not an object"
                            }
                        }
                    }
                }
            }
        error.message shouldContain "Object property default"
        error.message shouldContain "String"
    }

    @Test
    fun `object property default rejects number`() {
        val error =
            assertFailsWith<IllegalStateException> {
                jsonSchema {
                    name = "Test"
                    schema {
                        property("field") {
                            obj {
                                default = 123
                            }
                        }
                    }
                }
            }
        error.message shouldContain "Object property default"
        error.message shouldContain "Int"
    }

    @Test
    fun `object property default rejects boolean`() {
        val error =
            assertFailsWith<IllegalStateException> {
                jsonSchema {
                    name = "Test"
                    schema {
                        property("field") {
                            obj {
                                default = true
                            }
                        }
                    }
                }
            }
        error.message shouldContain "Object property default"
        error.message shouldContain "Boolean"
    }

    @Test
    fun `object property default rejects arbitrary object`() {
        data class Bar(
            val y: String,
        )

        val error =
            assertFailsWith<IllegalStateException> {
                jsonSchema {
                    name = "Test"
                    schema {
                        property("field") {
                            obj {
                                default = Bar("test")
                            }
                        }
                    }
                }
            }
        error.message shouldContain "Object property default"
        error.message shouldContain "Bar"
    }
}
