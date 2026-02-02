package kotlinx.schema.json

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlin.test.Test

/**
 * Tests for string enum deserialization, serialization, and DSL.
 */
class StringEnumTest {
    private val json = Json { prettyPrint = true }

    // Deserialization Tests

    @Test
    fun `deserialize enum with string values`() {
        // language=json
        val jsonString =
            """
            {
                "type": "string",
                "enum": [
                  "active",
                  "inactive",
                  "pending"
                ]
            }
            """.trimIndent()

        val decoded = deserializeAndSerialize<StringPropertyDefinition>(jsonString, json)
        decoded.enum.shouldNotBeNull {
            this shouldHaveSize 3
            this[0] shouldBe "active"
        }
    }

    @Test
    fun `deserialize enum with number values converts to strings`() {
        // language=json
        val jsonString =
            """
            {
                "type": "string",
                "enum": [
                  1,
                  2,
                  3
                ]
            }
            """.trimIndent()

        val decoded = json.decodeFromString<StringPropertyDefinition>(jsonString)
        decoded.enum.shouldContainExactly("1", "2", "3")
    }

    @Test
    fun `deserialize enum with object values converts to JSON strings`() {
        // language=json
        val jsonString =
            $$"""
            {
                "type": "string",
                "enum": [
                  {
                    "$anchor": "my_anchor",
                    "type": "null"
                  }
                ]
            }
            """.trimIndent()

        val decoded = json.decodeFromString<StringPropertyDefinition>(jsonString)
        decoded.enum.shouldNotBeNull {
            this shouldHaveSize 1
            this[0] shouldBe $$"""{"$anchor":"my_anchor","type":"null"}"""
        }
    }

    // Serialization Tests

    @Test
    fun `serialize enum with string values preserves values`() {
        // language=json
        val jsonString =
            """
            {
                "type": "string",
                "enum": ["active", "inactive"]
            }
            """.trimIndent()

        deserializeAndSerialize<StringPropertyDefinition>(jsonString, json)
    }

    @Test
    fun `serialize enum with mixed values preserves all as strings`() {
        // language=json
        val jsonString =
            """
            {
                "type": "string",
                "enum": ["text", "42", "true"]
            }
            """.trimIndent()

        deserializeAndSerialize<StringPropertyDefinition>(jsonString, json)
    }

    // Backward Compatibility

    @Test
    fun `backward compatibility - constructor with List String`() {
        val schema =
            testSchemaWithProperty {
                string {
                    enum = listOf("active", "inactive", "pending")
                }
            }

        val prop = schema.firstPropertyAs<StringPropertyDefinition>()
        prop.enum.shouldContainExactly("active", "inactive", "pending")
    }

    // DSL Tests

    @Test
    fun `DSL supports string enum`() {
        val schema =
            testSchemaWithProperty("status") {
                string {
                    description = "Status field"
                    enum = listOf("active", "inactive")
                }
            }

        val prop = schema.firstPropertyAs<StringPropertyDefinition>()
        prop.description shouldBe "Status field"
        prop.enum.shouldContainExactly("active", "inactive")
    }

    @Test
    fun `DSL enforces type safety for string enum with JsonPrimitive and plain strings`() {
        val schema =
            testSchemaWithProperty("status") {
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

        val prop = schema.firstPropertyAs<StringPropertyDefinition>()
        prop.enum.shouldNotBeNull {
            this shouldHaveSize 3
        }
    }

    @Test
    fun `DSL rejects mixed types in string enum`() {
        val exception =
            shouldThrow<IllegalArgumentException> {
                testSchemaWithProperty("invalid") {
                    string {
                        enum = listOf("string", 123) // Mixed types not allowed
                    }
                }
            }

        exception.message shouldBe "String property enum must contain only String values or null, but got: Int"
    }
}
