package kotlinx.schema.json.dsl

import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.schema.json.GenericPropertyDefinition
import kotlinx.schema.json.ObjectPropertyDefinition
import kotlinx.schema.json.jsonSchema
import kotlinx.schema.json.obj
import kotlinx.serialization.json.JsonPrimitive
import kotlin.test.Test

/**
 * Tests type-safe accessors (getTypedDefault/getTypedConst) for ValuePropertyDefinition<T>
 * using the JSON Schema DSL.
 *
 * As Roman Elizarov would say: "Make illegal states unrepresentable at compile time."
 * These tests verify that type safety is preserved through DSL construction.
 */
class TypeSafeValuePropertiesDslTest {
    @Test
    fun `string property with typed default and const accessors`() {
        val schema =
            jsonSchema {
                property("apiVersion") {
                    string {
                        description = "API version string"
                        default = "v1.0"
                        constValue = "v1.0"
                        minLength = 1
                    }
                }
            }

        val apiVersionProp = schema.stringProperty("apiVersion")!!

        // Type-safe accessors return String?
        apiVersionProp.getTypedDefault() shouldBe "v1.0"
        apiVersionProp.getTypedConst() shouldBe "v1.0"

        // Raw accessors return JsonElement
        apiVersionProp.default shouldBe JsonPrimitive("v1.0")
        apiVersionProp.constValue shouldBe JsonPrimitive("v1.0")
    }

    @Test
    fun `numeric property with typed accessors for integer`() {
        val schema =
            jsonSchema {
                property("port") {
                    integer {
                        description = "Server port"
                        default = 8080
                        constValue = 8080
                        minimum = 1.0
                        maximum = 65535.0
                    }
                }
            }

        val portProp = schema.numericProperty("port")!!

        // Returns Double (all numerics are Double)
        portProp.getTypedDefault() shouldBe 8080.0
        portProp.getTypedConst() shouldBe 8080.0
    }

    @Test
    fun `numeric property with typed accessors for floating point`() {
        val schema =
            jsonSchema {
                property("temperature") {
                    number {
                        description = "Temperature in Celsius"
                        default = 23.5
                        minimum = -273.15
                    }
                }
            }

        val tempProp = schema.numericProperty("temperature")!!
        tempProp.getTypedDefault() shouldBe 23.5
    }

    @Test
    fun `boolean property with typed accessors`() {
        val schema =
            jsonSchema {
                property("enabled") {
                    boolean {
                        description = "Feature flag"
                        default = true
                    }
                }
                property("disabled") {
                    boolean {
                        constValue = false
                    }
                }
            }

        val enabledProp = schema.booleanProperty("enabled")!!
        val disabledProp = schema.booleanProperty("disabled")!!

        enabledProp.getTypedDefault() shouldBe true
        disabledProp.getTypedConst() shouldBe false
    }

    @Test
    fun `array property preserves JsonArray type`() {
        val schema =
            jsonSchema {
                property("tags") {
                    array {
                        description = "Tag list"
                        minItems = 1
                    }
                }
            }

        val tagsProp = schema.arrayProperty("tags")!!

        // No default set, returns null
        tagsProp.getTypedDefault().shouldBeNull()
        tagsProp.getTypedConst().shouldBeNull()
    }

    @Test
    fun `object property preserves JsonObject type`() {
        val schema =
            jsonSchema {
                property("metadata") {
                    obj {
                        description = "Metadata object"
                        property("created") {
                            string { format = "date-time" }
                        }
                    }
                }
            }

        val metadataProp = schema.objectProperty("metadata")!!

        metadataProp.getTypedDefault().shouldBeNull()
        metadataProp.description shouldBe "Metadata object"
    }

    @Test
    fun `generic property accepts any JsonElement type`() {
        val schema =
            jsonSchema {
                property("flexible") {
                    generic {
                        description = "Can be any type"
                        type = listOf("string", "number", "boolean")
                    }
                }
            }

        val flexibleProp =
            schema.properties["flexible"]
                .shouldBeInstanceOf<GenericPropertyDefinition>()

        // GenericPropertyDefinition<JsonElement> returns JsonElement?
        flexibleProp.getTypedDefault().shouldBeNull()
    }

    @Test
    fun `DSL enforces type safety at construction time`() {
        val schema =
            jsonSchema {
                property("version") {
                    string {
                        description = "Version string"
                        default = "1.0.0" // Type-safe: String goes to string property
                    }
                }
                property("count") {
                    integer {
                        default = 42 // Type-safe: Int goes to numeric property
                    }
                }
            }

        // DSL prevents type mismatches at compile time
        val versionProp = schema.stringProperty("version")!!
        val countProp = schema.numericProperty("count")!!

        versionProp.getTypedDefault() shouldBe "1.0.0"
        countProp.getTypedDefault() shouldBe 42.0
    }

    @Test
    fun `discriminator const values are type-safe`() {
        val schema =
            jsonSchema {
                property("payment") {
                    oneOf {
                        obj {
                            description = "Credit card payment"
                            property("type") {
                                string {
                                    constValue = "credit_card"
                                }
                            }
                            property("cardNumber") {
                                string { pattern = "\\d{16}" }
                            }
                        }

                        obj {
                            description = "PayPal payment"
                            property("type") {
                                string {
                                    constValue = "paypal"
                                }
                            }
                            property("email") {
                                string { format = "email" }
                            }
                        }
                    }
                }
            }

        val paymentProp = schema.oneOfProperty("payment")!!

        // Verify discriminator properties are type-safe
        val firstOption = paymentProp.oneOf[0].shouldBeInstanceOf<ObjectPropertyDefinition>()
        val typeProp = firstOption.stringProperty("type")!!

        typeProp.getTypedConst() shouldBe "credit_card"
    }

    @Test
    fun `null and JsonNull handled correctly`() {
        val schema =
            jsonSchema {
                property("optional") {
                    string {
                        nullable = true
                    }
                }
            }

        val optionalProp = schema.stringProperty("optional")!!

        // No default set
        optionalProp.getTypedDefault().shouldBeNull()
        optionalProp.default.shouldBeNull()
    }

    @Test
    fun `compile-time type safety through generics`() {
        val schema =
            jsonSchema {
                property("name") {
                    string { default = "John" }
                }
                property("age") {
                    integer { default = 30 }
                }
                property("active") {
                    boolean { default = true }
                }
            }

        // Type inference works correctly
        val name: String? = schema.stringProperty("name")?.getTypedDefault()
        val age: Double? = schema.numericProperty("age")?.getTypedDefault()
        val active: Boolean? = schema.booleanProperty("active")?.getTypedDefault()

        name shouldBe "John"
        age shouldBe 30.0
        active shouldBe true
    }
}
