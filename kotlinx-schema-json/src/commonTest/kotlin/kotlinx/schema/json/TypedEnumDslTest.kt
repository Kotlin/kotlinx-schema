package kotlinx.schema.json

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlin.test.Test

/**
 * Tests for DSL enum support for numeric, boolean, array, and object property types.
 */
class TypedEnumDslTest {
    // Numeric Enum Tests

    @Test
    fun `DSL supports numeric enum`() {
        val schema =
            testSchemaWithProperty("priority") {
                integer {
                    description = "Priority level"
                    enum = listOf(1, 2, 3, 5, 8)
                }
            }

        val prop = schema.firstPropertyAs<NumericPropertyDefinition>()
        prop.description shouldBe "Priority level"
        prop.enum.shouldContainExactly(1.0, 2.0, 3.0, 5.0, 8.0)
    }

    // Boolean Enum Tests

    @Test
    fun `DSL supports boolean enum`() {
        val schema =
            testSchemaWithProperty("flag") {
                boolean {
                    description = "Boolean flag"
                    enum = listOf(true, false)
                }
            }

        val prop = schema.firstPropertyAs<BooleanPropertyDefinition>()
        prop.description shouldBe "Boolean flag"
        prop.enum.shouldContainExactly(true, false)
    }

    // Array Enum Tests

    @Test
    fun `DSL supports array enum with JsonArray`() {
        val schema =
            testSchemaWithProperty("coordinates") {
                array {
                    description = "Allowed coordinate pairs"
                    enum =
                        listOf(
                            JsonArray(listOf(JsonPrimitive(0), JsonPrimitive(0))),
                            JsonArray(listOf(JsonPrimitive(1), JsonPrimitive(1))),
                        )
                }
            }

        val prop = schema.firstPropertyAs<ArrayPropertyDefinition>()
        prop.description shouldBe "Allowed coordinate pairs"
        prop.enum.shouldNotBeNull {
            this shouldHaveSize 2
            this[0].shouldBeInstanceOf<JsonArray>()
        }
    }

    @Test
    fun `DSL rejects non-array values in array enum`() {
        val exception =
            shouldThrow<IllegalArgumentException> {
                testSchemaWithProperty("invalid") {
                    array {
                        enum = listOf("not an array")
                    }
                }
            }

        exception.message shouldBe "Array property enum must contain only JsonArray values or null, but got: String"
    }

    // Object Enum Tests

    @Test
    fun `DSL supports object enum with JsonObject`() {
        val schema =
            testSchemaWithProperty("config") {
                obj {
                    description = "Allowed configurations"
                    enum =
                        listOf(
                            JsonObject(mapOf("mode" to JsonPrimitive("read"))),
                            JsonObject(mapOf("mode" to JsonPrimitive("write"))),
                        )
                }
            }

        val prop = schema.firstPropertyAs<ObjectPropertyDefinition>()
        prop.description shouldBe "Allowed configurations"
        prop.enum.shouldNotBeNull {
            this shouldHaveSize 2
            this[0].shouldBeInstanceOf<JsonObject>()
        }
    }

    @Test
    fun `DSL supports object enum with Map for convenience`() {
        val schema =
            testSchemaWithProperty("config") {
                obj {
                    description = "Allowed configurations"
                    enum =
                        listOf(
                            mapOf("mode" to "read", "timeout" to 30),
                            mapOf("mode" to "write", "timeout" to 60),
                        )
                }
            }

        val prop = schema.firstPropertyAs<ObjectPropertyDefinition>()
        prop.enum.shouldNotBeNull {
            this shouldHaveSize 2
            this[0].shouldBeInstanceOf<JsonObject> { firstEnum ->
                firstEnum["mode"] shouldBe JsonPrimitive("read")
                firstEnum["timeout"] shouldBe JsonPrimitive(30)
            }
        }
    }

    @Test
    fun `DSL rejects non-object values in object enum`() {
        val exception =
            shouldThrow<IllegalArgumentException> {
                testSchemaWithProperty("invalid") {
                    obj {
                        enum = listOf("not an object")
                    }
                }
            }

        exception.message shouldBe
            "Object property enum must contain only JsonObject, Map, or null values, but got: String"
    }
}
