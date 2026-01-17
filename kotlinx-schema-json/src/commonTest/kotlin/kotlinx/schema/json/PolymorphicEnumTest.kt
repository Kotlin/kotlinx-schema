package kotlinx.schema.json

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlin.test.Test

class PolymorphicEnumTest {
    private val json = Json { prettyPrint = true }

    @Test
    fun `deserialize enum with string values`() {
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
            this[0] shouldBe JsonPrimitive("active")
        }
    }

    @Test
    fun `deserialize enum with number values`() {
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
            this[0] shouldBe JsonPrimitive(1)
        }
    }

    @Test
    fun `deserialize enum with object values`() {
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
            this[0].shouldBeInstanceOf<JsonObject>()
        }
    }

    @Test
    fun `deserialize heterogeneous enum`() {
        val jsonString =
            """
            {
                "type": "string",
                "enum": [6, "foo", [], true, {"foo": 12}]
            }
            """.trimIndent()

        val stringProp = json.decodeFromString(StringPropertyDefinition.serializer(), jsonString)

        stringProp.enum.shouldNotBeNull {
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
                        JsonPrimitive("active"),
                        JsonPrimitive("inactive"),
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
                        JsonPrimitive("text"),
                        JsonPrimitive(42),
                        JsonPrimitive(true),
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
            this[0] shouldBe JsonPrimitive("active")
            this[1] shouldBe JsonPrimitive("inactive")
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
                enum[0] shouldBe JsonPrimitive(1)
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
                enum[0] shouldBe JsonPrimitive(true)
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
}
