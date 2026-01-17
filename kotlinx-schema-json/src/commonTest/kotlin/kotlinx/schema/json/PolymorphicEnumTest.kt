package kotlinx.schema.json

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlin.test.Test

class PolymorphicEnumTest {
    private val json = Json { prettyPrint = true }

    @Test
    fun `deserialize enum with string values`() {
        // language=json
        val jsonString =
            """
            {
                "type": "string",
                "enum": ["active", "inactive", "pending"]
            }
            """.trimIndent()

        val stringProp = json.decodeFromString(StringPropertyDefinition.serializer(), jsonString)

        stringProp.enum.shouldNotBeNull {
            this shouldHaveSize 3
            this[0] shouldBe "active"
        }
    }

    @Test
    fun `deserialize enum with number values`() {
        // language=json
        val jsonString =
            """
            {
                "type": "string",
                "enum": [1, 2, 3]
            }
            """.trimIndent()

        val stringProp = json.decodeFromString(StringPropertyDefinition.serializer(), jsonString)

        stringProp.enum.shouldNotBeNull {
            this shouldHaveSize 3
            this[0] shouldBe "1"
        }
    }

    @Test
    fun `deserialize enum with object values`() {
        // language=json
        val jsonString =
            $$"""
            {
                "type": "string",
                "enum": [
                    {"$anchor": "my_anchor", "type": "null"}
                ]
            }
            """.trimIndent()

        val stringProp = json.decodeFromString(StringPropertyDefinition.serializer(), jsonString)

        stringProp.enum.shouldNotBeNull {
            this shouldHaveSize 1
            // JsonObject is converted to its string representation
            this[0] shouldBe $$"""{"$anchor":"my_anchor","type":"null"}"""
        }
    }

    @Test
    fun `deserialize heterogeneous enum`() {
        // language=json
        val jsonString =
            """
            {
                "enum": [6, "foo", [], true, {"foo": 12}]
            }
            """.trimIndent()

        val genericProp = json.decodeFromString(GenericPropertyDefinition.serializer(), jsonString)

        genericProp.enum.shouldNotBeNull {
            this shouldHaveSize 5
            this[0] shouldBe JsonPrimitive(6)
            this[1] shouldBe JsonPrimitive("foo")
            this[2].shouldBeInstanceOf<JsonArray>()
            this[3] shouldBe JsonPrimitive(true)
            this[4].shouldBeInstanceOf<JsonObject>()
        }
    }

    @Test
    fun `serialize enum with string values`() {
        val stringProp =
            StringPropertyDefinition(
                enum =
                    listOf(
                        "active",
                        "inactive",
                    ),
            )

        val jsonString = json.encodeToString(StringPropertyDefinition.serializer(), stringProp)
        val decoded = json.decodeFromString(StringPropertyDefinition.serializer(), jsonString)

        decoded.enum shouldBe stringProp.enum
    }

    @Test
    fun `serialize enum with mixed values`() {
        val stringProp =
            StringPropertyDefinition(
                enum =
                    listOf(
                        "text",
                        "42",
                        "true",
                    ),
            )

        val jsonString = json.encodeToString(StringPropertyDefinition.serializer(), stringProp)
        val decoded = json.decodeFromString(StringPropertyDefinition.serializer(), jsonString)

        decoded.enum shouldBe stringProp.enum
    }

    @Test
    fun `backward compatibility - constructor with List String`() {
        // This should still work with the extension function
        val stringProp =
            StringPropertyDefinition(
                enum = listOf("active", "inactive", "pending"),
            )

        stringProp.enum.shouldNotBeNull {
            this shouldHaveSize 3
            this[0] shouldBe "active"
            this[1] shouldBe "inactive"
        }
    }

    @Test
    fun `DSL supports string enum`() {
        val schema =
            jsonSchema {
                name = "TestSchema"
                schema {
                    property("status") {
                        string {
                            description = "Status field"
                            enum = listOf("active", "inactive")
                        }
                    }
                }
            }

        val props = schema.schema.properties
        props.shouldNotBeNull()
        props.size shouldBe 1
    }

    @Test
    fun `DSL enforces type safety for string enum`() {
        val schema =
            jsonSchema {
                name = "TestSchema"
                schema {
                    property("status") {
                        string {
                            description = "String enum only"
                            enum =
                                listOf(
                                    JsonPrimitive("active"),
                                    JsonPrimitive("inactive"),
                                    "pending", // Also supports plain strings
                                )
                        }
                    }
                }
            }

        val schemaProps = schema.schema.properties
        schemaProps.shouldNotBeNull {
            size shouldBe 1
            val prop = values.first() as? StringPropertyDefinition
            prop.shouldNotBeNull {
                enum!! shouldHaveSize 3
            }
        }
    }

    @Test
    fun `DSL rejects mixed types in string enum`() {
        val exception =
            kotlin
                .runCatching {
                    jsonSchema {
                        name = "TestSchema"
                        schema {
                            property("invalid") {
                                string {
                                    enum = listOf("string", 123) // Mixed types not allowed
                                }
                            }
                        }
                    }
                }.exceptionOrNull()

        exception.shouldBeInstanceOf<IllegalArgumentException>()
        exception.message shouldBe "String property enum must contain only String values or null, but got: Int"
    }

    @Test
    fun `DSL supports numeric enum`() {
        val schema =
            jsonSchema {
                name = "TestSchema"
                schema {
                    property("priority") {
                        integer {
                            description = "Priority level"
                            enum = listOf(1, 2, 3, 5, 8)
                        }
                    }
                }
            }

        schema.schema.properties.shouldNotBeNull {
            val prop = values.first() as? NumericPropertyDefinition
            prop shouldNotBeNull {
                enum!! shouldHaveSize 5
                enum[0] shouldBe 1
            }
        }
    }

    @Test
    fun `DSL supports boolean enum`() {
        val schema =
            jsonSchema {
                name = "TestSchema"
                schema {
                    property("flag") {
                        boolean {
                            description = "Boolean flag"
                            enum = listOf(true, false)
                        }
                    }
                }
            }

        schema.schema.properties.shouldNotBeNull {
            val prop = values.first() as? BooleanPropertyDefinition
            prop.shouldNotBeNull {
                enum!! shouldHaveSize 2
                enum[0] shouldBe true
            }
        }
    }

    @Test
    fun `DSL supports array enum`() {
        val schema =
            jsonSchema {
                name = "TestSchema"
                schema {
                    property("coordinates") {
                        array {
                            description = "Allowed coordinate pairs"
                            enum =
                                listOf(
                                    JsonArray(listOf(JsonPrimitive(0), JsonPrimitive(0))),
                                    JsonArray(listOf(JsonPrimitive(1), JsonPrimitive(1))),
                                )
                        }
                    }
                }
            }

        schema.schema.properties.shouldNotBeNull {
            val prop = values.first() as? ArrayPropertyDefinition
            prop.shouldNotBeNull {
                enum!! shouldHaveSize 2
                enum[0].shouldBeInstanceOf<JsonArray>()
            }
        }
    }

    @Test
    fun `DSL rejects non-array values in array enum`() {
        val exception =
            kotlin
                .runCatching {
                    jsonSchema {
                        name = "TestSchema"
                        schema {
                            property("invalid") {
                                array {
                                    enum = listOf("not an array")
                                }
                            }
                        }
                    }
                }.exceptionOrNull()

        exception.shouldBeInstanceOf<IllegalArgumentException>()
        exception.message shouldBe "Array property enum must contain only JsonArray values or null, but got: String"
    }

    @Test
    fun `DSL supports object enum with JsonObject`() {
        val schema =
            jsonSchema {
                name = "TestSchema"
                schema {
                    property("config") {
                        obj {
                            description = "Allowed configurations"
                            enum =
                                listOf(
                                    JsonObject(mapOf("mode" to JsonPrimitive("read"))),
                                    JsonObject(mapOf("mode" to JsonPrimitive("write"))),
                                )
                        }
                    }
                }
            }

        schema.schema.properties.shouldNotBeNull {
            val prop = values.first() as? ObjectPropertyDefinition
            prop.shouldNotBeNull {
                enum!! shouldHaveSize 2
                enum[0].shouldBeInstanceOf<JsonObject>()
            }
        }
    }

    @Test
    fun `DSL supports object enum with Map for convenience`() {
        val schema =
            jsonSchema {
                name = "TestSchema"
                schema {
                    property("config") {
                        obj {
                            description = "Allowed configurations"
                            enum =
                                listOf(
                                    mapOf("mode" to "read", "timeout" to 30),
                                    mapOf("mode" to "write", "timeout" to 60),
                                )
                        }
                    }
                }
            }

        schema.schema.properties.shouldNotBeNull {
            val prop = values.first() as? ObjectPropertyDefinition
            prop.shouldNotBeNull {
                enum.shouldNotBeNull {
                    this shouldHaveSize 2
                    this[0].shouldBeInstanceOf<JsonObject> { firstEnum ->
                        firstEnum["mode"] shouldBe JsonPrimitive("read")
                        firstEnum["timeout"] shouldBe JsonPrimitive(30)
                    }
                }
            }
        }
    }

    @Test
    fun `DSL rejects non-object values in object enum`() {
        val exception =
            kotlin
                .runCatching {
                    jsonSchema {
                        name = "TestSchema"
                        schema {
                            property("invalid") {
                                obj {
                                    enum = listOf("not an object")
                                }
                            }
                        }
                    }
                }.exceptionOrNull()

        exception.shouldBeInstanceOf<IllegalArgumentException>()
        exception.message shouldBe
            "Object property enum must contain only JsonObject, Map, or null values, but got: String"
    }

    @Test
    fun `DSL supports generic property`() {
        val schema =
            jsonSchema {
                name = "TestSchema"
                schema {
                    property("flexibleValue") {
                        generic {
                            description = "Can be any JSON type"
                        }
                    }
                }
            }

        schema.schema.properties.shouldNotBeNull {
            val prop = values.first() as? GenericPropertyDefinition
            prop.shouldNotBeNull {
                description shouldBe "Can be any JSON type"
                type shouldBe null
            }
        }
    }

    @Test
    fun `DSL supports generic property with heterogeneous enum`() {
        val schema =
            jsonSchema {
                name = "TestSchema"
                schema {
                    property("value") {
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
                }
            }

        schema.schema.properties.shouldNotBeNull {
            val prop = values.first() as? GenericPropertyDefinition
            prop.shouldNotBeNull {
                enum!! shouldHaveSize 13

                // Plain types
                enum[0] shouldBe JsonPrimitive(42)
                enum[1] shouldBe JsonPrimitive(3.14)
                enum[2] shouldBe JsonPrimitive("text")
                enum[3] shouldBe JsonPrimitive(true)
                enum[4] shouldBe JsonPrimitive(false)
                enum[5] shouldBe JsonNull

                // Collections
                enum[6].shouldBeInstanceOf<JsonArray>()
                (enum[6] as JsonArray)[0] shouldBe JsonPrimitive(1)
                enum[7].shouldBeInstanceOf<JsonArray>()
                (enum[7] as JsonArray)[0] shouldBe JsonPrimitive("a")

                // Maps
                enum[8].shouldBeInstanceOf<JsonObject>()
                (enum[8] as JsonObject)["key"] shouldBe JsonPrimitive("value")
                (enum[8] as JsonObject)["count"] shouldBe JsonPrimitive(10)
                enum[9].shouldBeInstanceOf<JsonObject>()

                // JsonElement types
                enum[10] shouldBe JsonPrimitive(99)
                enum[11].shouldBeInstanceOf<JsonArray>()
                enum[12].shouldBeInstanceOf<JsonObject>()
            }
        }
    }

    @Test
    fun `DSL supports generic property with type specified`() {
        val schema =
            jsonSchema {
                name = "TestSchema"
                schema {
                    property("multiType") {
                        generic {
                            description = "Can be string or number"
                            type = listOf("string", "number")
                        }
                    }
                }
            }

        schema.schema.properties.shouldNotBeNull {
            val prop = values.first() as? GenericPropertyDefinition
            prop.shouldNotBeNull {
                type shouldBe listOf("string", "number")
            }
        }
    }

    @Test
    fun `DSL generic property rejects unsupported enum values`() {
        // Custom class that is not supported
        data class UnsupportedType(
            val value: String,
        )

        val exception =
            kotlin
                .runCatching {
                    jsonSchema {
                        name = "TestSchema"
                        schema {
                            property("invalid") {
                                generic {
                                    enum = listOf(UnsupportedType("test")) // Custom types not supported
                                }
                            }
                        }
                    }
                }.exceptionOrNull()

        exception.shouldBeInstanceOf<IllegalArgumentException>()
        exception.message shouldBe
            "Generic property enum must contain JsonElement, String, Number, Boolean, null, List, or Map, " +
            "but got: UnsupportedType"
    }

    @Test
    fun `DSL supports generic property with nested collections and maps`() {
        val schema =
            jsonSchema {
                name = "TestSchema"
                schema {
                    property("complexValues") {
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
                }
            }

        schema.schema.properties.shouldNotBeNull {
            val prop = values.first() as? GenericPropertyDefinition
            prop.shouldNotBeNull {
                enum!! shouldHaveSize 3

                // Nested list
                enum[0].shouldBeInstanceOf<JsonArray>()
                val nestedArray = enum[0] as JsonArray
                nestedArray[0].shouldBeInstanceOf<JsonArray>()

                // Nested map
                enum[1].shouldBeInstanceOf<JsonObject>()
                val nestedObj = enum[1] as JsonObject
                nestedObj["user"].shouldBeInstanceOf<JsonObject>()
                (nestedObj["user"] as JsonObject)["name"] shouldBe JsonPrimitive("Alice")

                // Mixed nesting
                enum[2].shouldBeInstanceOf<JsonObject>()
                val mixedObj = enum[2] as JsonObject
                mixedObj["items"].shouldBeInstanceOf<JsonArray>()
                mixedObj["metadata"].shouldBeInstanceOf<JsonObject>()
            }
        }
    }

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

        val jsonString = json.encodeToString(GenericPropertyDefinition.serializer(), genericProp)
        val decoded = json.decodeFromString(GenericPropertyDefinition.serializer(), jsonString)

        decoded.enum shouldBe genericProp.enum
        decoded.description shouldBe "Mixed types"
    }
}
