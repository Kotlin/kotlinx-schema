package kotlinx.schema.json

import io.kotest.matchers.string.shouldContain
import kotlinx.serialization.json.JsonPrimitive
import kotlin.test.Test
import kotlin.test.assertFailsWith

/**
 * Tests for type validation in property builders.
 * Ensures that default and constValue properties only accept valid types.
 */
internal class PropertyBuilderValidationTest {
    // String Property Tests

    @Test
    fun `string property default accepts string`() {
        jsonSchema {
            name = "Test"
            schema {
                property("field") {
                    string {
                        default = "valid string"
                    }
                }
            }
        }
    }

    @Test
    fun `string property default accepts JsonElement`() {
        jsonSchema {
            name = "Test"
            schema {
                property("field") {
                    string {
                        default = JsonPrimitive("valid")
                    }
                }
            }
        }
    }

    @Test
    fun `string property default accepts null`() {
        jsonSchema {
            name = "Test"
            schema {
                property("field") {
                    string {
                        default = null
                    }
                }
            }
        }
    }

    @Test
    fun `string property default rejects number`() {
        val error =
            assertFailsWith<IllegalStateException> {
                jsonSchema {
                    name = "Test"
                    schema {
                        property("field") {
                            string {
                                default = 123
                            }
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
                    name = "Test"
                    schema {
                        property("field") {
                            string {
                                default = true
                            }
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
                    name = "Test"
                    schema {
                        property("field") {
                            string {
                                default = Foo(42)
                            }
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
            name = "Test"
            schema {
                property("field") {
                    string {
                        constValue = "constant"
                    }
                }
            }
        }
    }

    @Test
    fun `string property constValue rejects number`() {
        val error =
            assertFailsWith<IllegalStateException> {
                jsonSchema {
                    name = "Test"
                    schema {
                        property("field") {
                            string {
                                constValue = 456
                            }
                        }
                    }
                }
            }
        error.message shouldContain "String property constValue"
        error.message shouldContain "Int"
    }

    // Numeric Property Tests

    @Test
    fun `numeric property default accepts integer`() {
        jsonSchema {
            name = "Test"
            schema {
                property("field") {
                    integer {
                        default = 42
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
                        default = 3.14
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
                    integer {
                        default = 9999999999L
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
                            integer {
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
                                default = false
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
                        constValue = 0.5
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
                            integer {
                                constValue = "123"
                            }
                        }
                    }
                }
            }
        error.message shouldContain "Numeric property constValue"
        error.message shouldContain "String"
    }

    // Boolean Property Tests

    @Test
    fun `boolean property default accepts boolean`() {
        jsonSchema {
            name = "Test"
            schema {
                property("field") {
                    boolean {
                        default = true
                    }
                }
            }
        }
    }

    @Test
    fun `boolean property default accepts JsonElement`() {
        jsonSchema {
            name = "Test"
            schema {
                property("field") {
                    boolean {
                        default = JsonPrimitive(false)
                    }
                }
            }
        }
    }

    @Test
    fun `boolean property default accepts null`() {
        jsonSchema {
            name = "Test"
            schema {
                property("field") {
                    boolean {
                        default = null
                    }
                }
            }
        }
    }

    @Test
    fun `boolean property default rejects string`() {
        val error =
            assertFailsWith<IllegalStateException> {
                jsonSchema {
                    name = "Test"
                    schema {
                        property("field") {
                            boolean {
                                default = "true"
                            }
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
                    name = "Test"
                    schema {
                        property("field") {
                            boolean {
                                default = 1
                            }
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
            name = "Test"
            schema {
                property("field") {
                    boolean {
                        constValue = false
                    }
                }
            }
        }
    }

    @Test
    fun `boolean property constValue rejects string`() {
        val error =
            assertFailsWith<IllegalStateException> {
                jsonSchema {
                    name = "Test"
                    schema {
                        property("field") {
                            boolean {
                                constValue = "false"
                            }
                        }
                    }
                }
            }
        error.message shouldContain "Boolean property constValue"
        error.message shouldContain "String"
    }

    @Test
    fun `boolean property constValue rejects number`() {
        val error =
            assertFailsWith<IllegalStateException> {
                jsonSchema {
                    name = "Test"
                    schema {
                        property("field") {
                            boolean {
                                constValue = 0
                            }
                        }
                    }
                }
            }
        error.message shouldContain "Boolean property constValue"
        error.message shouldContain "Int"
    }

    // Array Property Tests

    @Test
    fun `array property default accepts list`() {
        jsonSchema {
            name = "Test"
            schema {
                property("field") {
                    array {
                        default = listOf("a", "b", "c")
                    }
                }
            }
        }
    }

    @Test
    fun `array property default accepts JsonElement`() {
        jsonSchema {
            name = "Test"
            schema {
                property("field") {
                    array {
                        default = JsonPrimitive("test")
                    }
                }
            }
        }
    }

    @Test
    fun `array property default accepts null`() {
        jsonSchema {
            name = "Test"
            schema {
                property("field") {
                    array {
                        default = null
                    }
                }
            }
        }
    }

    @Test
    fun `array property default rejects string`() {
        val error =
            assertFailsWith<IllegalStateException> {
                jsonSchema {
                    name = "Test"
                    schema {
                        property("field") {
                            array {
                                default = "not an array"
                            }
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
                    name = "Test"
                    schema {
                        property("field") {
                            array {
                                default = 123
                            }
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
                    name = "Test"
                    schema {
                        property("field") {
                            array {
                                default = true
                            }
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
                    name = "Test"
                    schema {
                        property("field") {
                            array {
                                default = Foo(42)
                            }
                        }
                    }
                }
            }
        error.message shouldContain "Array property default"
        error.message shouldContain "Foo"
    }

    // Object Property Tests

    @Test
    fun `object property default accepts map`() {
        jsonSchema {
            name = "Test"
            schema {
                property("field") {
                    obj {
                        default = mapOf("key" to "value")
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
                        default = JsonPrimitive("test")
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
                                default = 456
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
                                default = false
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
