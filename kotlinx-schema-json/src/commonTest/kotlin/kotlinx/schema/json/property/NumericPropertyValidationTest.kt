package kotlinx.schema.json.property

import io.kotest.matchers.string.shouldContain
import kotlinx.schema.json.jsonSchema
import kotlinx.serialization.json.JsonPrimitive
import kotlin.test.Test
import kotlin.test.assertFailsWith

internal class NumericPropertyValidationTest {
    @Test
    fun `numeric property default accepts integer`() {
        jsonSchema {
            name = "Test"
            schema {
                property("field") {
                    number {
                        default = 123
                    }
                }
            }
        }
    }

    @Test
    fun `numeric property default accepts double`() {
        jsonSchema {
            name = "Test"
            schema {
                property("field") {
                    number {
                        default = 12.34
                    }
                }
            }
        }
    }

    @Test
    fun `numeric property default accepts long`() {
        jsonSchema {
            name = "Test"
            schema {
                property("field") {
                    number {
                        default = 123L
                    }
                }
            }
        }
    }

    @Test
    fun `numeric property default accepts JsonElement`() {
        jsonSchema {
            name = "Test"
            schema {
                property("field") {
                    number {
                        default = JsonPrimitive(123)
                    }
                }
            }
        }
    }

    @Test
    fun `numeric property default accepts null`() {
        jsonSchema {
            name = "Test"
            schema {
                property("field") {
                    number {
                        default = null
                    }
                }
            }
        }
    }

    @Test
    fun `numeric property default rejects string`() {
        val error =
            assertFailsWith<IllegalStateException> {
                jsonSchema {
                    name = "Test"
                    schema {
                        property("field") {
                            number {
                                default = "not a number"
                            }
                        }
                    }
                }
            }
        error.message shouldContain "Numeric property default"
        error.message shouldContain "String"
    }

    @Test
    fun `numeric property default rejects boolean`() {
        val error =
            assertFailsWith<IllegalStateException> {
                jsonSchema {
                    name = "Test"
                    schema {
                        property("field") {
                            number {
                                default = true
                            }
                        }
                    }
                }
            }
        error.message shouldContain "Numeric property default"
        error.message shouldContain "Boolean"
    }

    @Test
    fun `numeric property constValue accepts number`() {
        jsonSchema {
            name = "Test"
            schema {
                property("field") {
                    number {
                        constValue = 789
                    }
                }
            }
        }
    }

    @Test
    fun `numeric property constValue rejects string`() {
        val error =
            assertFailsWith<IllegalStateException> {
                jsonSchema {
                    name = "Test"
                    schema {
                        property("field") {
                            number {
                                constValue = "not a number"
                            }
                        }
                    }
                }
            }
        error.message shouldContain "Numeric property const"
        error.message shouldContain "String"
    }
}
