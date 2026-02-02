package kotlinx.schema.json.dsl

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.schema.json.GenericPropertyDefinition
import kotlinx.schema.json.deserializeAndSerialize
import kotlinx.schema.json.firstPropertyAs
import kotlinx.schema.json.serializeAndDeserialize
import kotlinx.schema.json.testSchemaWithProperty
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlin.test.Test

/**
 * Tests for GenericPropertyDefinition DSL support.
 */
class GenericPropertyDslTest {
    private val json = Json { prettyPrint = true }

    // Basic Generic Property Tests

    @Test
    fun `DSL supports generic property without constraints`() {
        val schema =
            testSchemaWithProperty("flexibleValue") {
                generic {
                    description = "Can be any JSON type"
                }
            }

        val prop = schema.firstPropertyAs<GenericPropertyDefinition>()
        prop.description shouldBe "Can be any JSON type"
        prop.type shouldBe null
    }

    @Test
    fun `DSL supports generic property with type constraint`() {
        val schema =
            testSchemaWithProperty("multiType") {
                generic {
                    description = "String or number"
                    type = listOf("string", "number")
                }
            }

        val prop = schema.firstPropertyAs<GenericPropertyDefinition>()
        prop.type shouldBe listOf("string", "number")
    }

    @Test
    fun `DSL supports generic property with nullable`() {
        val schema =
            testSchemaWithProperty("nullableValue") {
                generic {
                    description = "Can be null"
                    nullable = true
                }
            }

        val prop = schema.firstPropertyAs<GenericPropertyDefinition>()
        prop.nullable shouldBe true
    }

    // Default and Const Value Tests

    @Test
    fun `DSL supports generic property with default value`() {
        val schema =
            testSchemaWithProperty("value") {
                generic {
                    description = "Value with default"
                    default = JsonPrimitive("default_value")
                }
            }

        val prop = schema.firstPropertyAs<GenericPropertyDefinition>()
        prop.default shouldBe JsonPrimitive("default_value")
    }

    @Test
    fun `DSL supports generic property with const value`() {
        val schema =
            testSchemaWithProperty("version") {
                generic {
                    description = "API version"
                    constValue = JsonPrimitive("v1")
                }
            }

        val prop = schema.firstPropertyAs<GenericPropertyDefinition>()
        prop.constValue shouldBe JsonPrimitive("v1")
    }

    // Heterogeneous Enum Tests

    @Test
    fun `DSL supports generic property with heterogeneous enum`() {
        val schema =
            testSchemaWithProperty("value") {
                generic {
                    description = "Mixed type values"
                    enum =
                        listOf(
                            // Plain Kotlin types
                            42,
                            3.14,
                            "text",
                            true,
                            false,
                            null,
                            // Collections (converted to JsonArray)
                            listOf(1, 2, 3),
                            listOf("a", "b"),
                            // Maps (converted to JsonObject)
                            mapOf("key" to "value", "count" to 10),
                            mapOf("nested" to mapOf("inner" to "value")),
                            // JsonElement types
                            JsonPrimitive(99),
                            JsonArray(listOf(JsonPrimitive(1), JsonPrimitive(2))),
                            JsonObject(mapOf("key" to JsonPrimitive("value"))),
                        )
                }
            }

        val prop = schema.firstPropertyAs<GenericPropertyDefinition>()
        prop.enum.shouldNotBeNull {
            this shouldHaveSize 13

            // Plain types
            this[0] shouldBe JsonPrimitive(42)
            this[1] shouldBe JsonPrimitive(3.14)
            this[2] shouldBe JsonPrimitive("text")
            this[3] shouldBe JsonPrimitive(true)
            this[4] shouldBe JsonPrimitive(false)
            this[5] shouldBe JsonNull

            // Collections
            this[6].shouldBeInstanceOf<JsonArray>()
            (this[6] as JsonArray)[0] shouldBe JsonPrimitive(1)
            this[7].shouldBeInstanceOf<JsonArray>()
            (this[7] as JsonArray)[0] shouldBe JsonPrimitive("a")

            // Maps
            this[8].shouldBeInstanceOf<JsonObject>()
            (this[8] as JsonObject)["key"] shouldBe JsonPrimitive("value")
            (this[8] as JsonObject)["count"] shouldBe JsonPrimitive(10)
            this[9].shouldBeInstanceOf<JsonObject>()

            // JsonElement types
            this[10] shouldBe JsonPrimitive(99)
            this[11].shouldBeInstanceOf<JsonArray>()
            this[12].shouldBeInstanceOf<JsonObject>()
        }
    }

    @Test
    fun `DSL supports generic property with nested collections and maps`() {
        val schema =
            testSchemaWithProperty("complexValues") {
                generic {
                    description = "Nested collections and maps"
                    enum =
                        listOf(
                            // Nested lists
                            listOf(listOf(1, 2), listOf(3, 4)),
                            // Nested maps
                            mapOf(
                                "user" to mapOf("name" to "Alice", "age" to 30),
                                "settings" to mapOf("theme" to "dark"),
                            ),
                            // Mixed nesting
                            mapOf(
                                "items" to listOf(1, 2, 3),
                                "metadata" to mapOf("count" to 3),
                            ),
                        )
                }
            }

        val prop = schema.firstPropertyAs<GenericPropertyDefinition>()
        prop.enum.shouldNotBeNull {
            this shouldHaveSize 3

            // Nested list
            this[0].shouldBeInstanceOf<JsonArray>()
            val nestedArray = this[0] as JsonArray
            nestedArray[0].shouldBeInstanceOf<JsonArray>()

            // Nested map
            this[1].shouldBeInstanceOf<JsonObject>()
            val nestedObj = this[1] as JsonObject
            nestedObj["user"].shouldBeInstanceOf<JsonObject>()
            (nestedObj["user"] as JsonObject)["name"] shouldBe JsonPrimitive("Alice")

            // Mixed nesting
            this[2].shouldBeInstanceOf<JsonObject>()
            val mixedObj = this[2] as JsonObject
            mixedObj["items"].shouldBeInstanceOf<JsonArray>()
            mixedObj["metadata"].shouldBeInstanceOf<JsonObject>()
        }
    }

    @Test
    fun `DSL generic property rejects unsupported enum values`() {
        // Custom class that is not supported
        data class UnsupportedType(
            val value: String,
        )

        val exception =
            shouldThrow<IllegalArgumentException> {
                testSchemaWithProperty("invalid") {
                    generic {
                        enum = listOf(UnsupportedType("test"))
                    }
                }
            }

        exception.message shouldBe
            "Generic property enum must contain JsonElement, String, Number, Boolean, null, List, or Map, " +
            "but got: UnsupportedType"
    }

    // Serialization Tests

    @Test
    fun `serialize and deserialize generic property with heterogeneous enum`() {
        val genericProp =
            GenericPropertyDefinition(
                description = "Mixed types",
                enum =
                    listOf(
                        JsonPrimitive(1),
                        JsonPrimitive("text"),
                        JsonPrimitive(true),
                    ),
            )

        serializeAndDeserialize(
            genericProp,
            """
            {
              "description": "Mixed types",
              "enum": [
                1,
                "text",
                true
              ]
            }
            """.trimIndent(),
            json,
        )
    }

    @Test
    fun `serialize and deserialize generic property with default`() {
        val genericProp =
            GenericPropertyDefinition(
                description = "With default",
                default = JsonPrimitive(42),
            )

        serializeAndDeserialize(
            genericProp,
            """
            {
              "description": "With default",
              "default": 42
            }
            """.trimIndent(),
            json,
        )
    }

    @Test
    fun `serialize and deserialize generic property with const`() {
        val genericProp =
            GenericPropertyDefinition(
                description = "With const",
                constValue = JsonPrimitive("constant"),
            )

        serializeAndDeserialize(
            genericProp,
            """
            {
              "description": "With const",
              "const": "constant"
            }
            """.trimIndent(),
            json,
        )
    }

    // Deserialization Test

    @Test
    fun `deserialize heterogeneous enum without type constraint`() {
        // language=json
        val jsonString =
            """
            {
                "enum": [6, "foo", [], true, {"foo": 12}]
            }
            """.trimIndent()

        val genericProp = deserializeAndSerialize<GenericPropertyDefinition>(jsonString, json)

        genericProp.enum.shouldNotBeNull {
            this shouldHaveSize 5
            this[0] shouldBe JsonPrimitive(6)
            this[1] shouldBe JsonPrimitive("foo")
            this[2].shouldBeInstanceOf<JsonArray>()
            this[3] shouldBe JsonPrimitive(true)
            this[4].shouldBeInstanceOf<JsonObject>()
        }
    }
}
