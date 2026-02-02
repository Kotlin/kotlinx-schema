package kotlinx.schema.json.dsl

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import kotlinx.schema.json.AllOfPropertyDefinition
import kotlinx.schema.json.AnyOfPropertyDefinition
import kotlinx.schema.json.ArrayPropertyDefinition
import kotlinx.schema.json.NumericPropertyDefinition
import kotlinx.schema.json.ObjectPropertyDefinition
import kotlinx.schema.json.OneOfPropertyDefinition
import kotlinx.schema.json.ReferencePropertyDefinition
import kotlinx.schema.json.StringPropertyDefinition
import kotlinx.schema.json.boolean
import kotlinx.schema.json.integer
import kotlinx.schema.json.jsonSchema
import kotlinx.schema.json.number
import kotlinx.schema.json.obj
import kotlinx.schema.json.oneOf
import kotlinx.schema.json.reference
import kotlinx.schema.json.serializeAndDeserialize
import kotlinx.schema.json.string
import kotlinx.serialization.json.Json
import kotlin.test.Test

/**
 * Tests for polymorphic property definitions DSL usage.
 */
@Suppress("LargeClass", "LongMethod")
internal class PolymorphicDslTest {
    private val json = Json { prettyPrint = true }

    @Test
    fun `oneOf DSL with simple options`() {
        val schema =
            jsonSchema {
                property("value") {
                    required = true
                    oneOf {
                        description = "String or number"
                        string { minLength = 1 }
                        number { minimum = 0.0 }
                    }
                }
            }

        val properties = schema.properties
        properties.size shouldBe 1

        val valueProp = properties["value"] as OneOfPropertyDefinition
        valueProp.oneOf.size shouldBe 2
        valueProp.description shouldBe "String or number"
    }

    @Test
    fun `oneOf DSL with discriminator`() {
        val schema =
            jsonSchema {
                property("pet") {
                    required = true
                    oneOf {
                        description = "A pet animal"
                        discriminator("petType") {
                            "dog" mappedTo "#/definitions/Dog"
                            "cat" mappedTo "#/definitions/Cat"
                        }
                    }
                }
            }

        val petProp = schema.properties["pet"] as OneOfPropertyDefinition
        petProp.discriminator shouldNotBeNull {
            propertyName shouldBe "petType"
            mapping?.size shouldBe 2
            mapping?.get("dog") shouldBe "#/definitions/Dog"
        }
    }

    @Test
    fun `oneOf DSL with inline objects`() {
        val schema =
            jsonSchema {
                property("payment") {
                    required = true
                    oneOf {
                        description = "Payment method"
                        obj {
                            property("type") {
                                required = true
                                string { constValue = "credit_card" }
                            }
                            property("cardNumber") {
                                required = true
                                string()
                            }
                        }
                        obj {
                            property("type") {
                                required = true
                                string { constValue = "paypal" }
                            }
                            property("email") {
                                required = true
                                string { format = "email" }
                            }
                        }
                    }
                }
            }

        val paymentProp = schema.properties["payment"] as OneOfPropertyDefinition
        paymentProp.oneOf.size shouldBe 2
        paymentProp.description shouldBe "Payment method"

        val firstOption = paymentProp.oneOf[0] as ObjectPropertyDefinition
        firstOption.properties?.size shouldBe 2
        firstOption.required?.size shouldBe 2
    }

    @Test
    fun `oneOf DSL with discriminator and inline objects using concise mappedTo`() {
        val schema =
            jsonSchema {
                property("payment") {
                    required = true
                    oneOf {
                        description = "Payment method"
                        discriminator("type") {
                            // Concise form - no obj keyword needed
                            "credit_card" mappedTo {
                                property("type") {
                                    required = true
                                    string { constValue = "credit_card" }
                                }
                                property("cardNumber") {
                                    required = true
                                    string()
                                }
                            }
                            "paypal" mappedTo {
                                property("type") {
                                    required = true
                                    string { constValue = "paypal" }
                                }
                                property("email") {
                                    required = true
                                    string { format = "email" }
                                }
                            }
                        }
                    }
                }
            }

        val paymentProp = schema.properties["payment"] as OneOfPropertyDefinition
        paymentProp.oneOf.size shouldBe 2
        paymentProp.description shouldBe "Payment method"

        // Discriminator is set with property name
        paymentProp.discriminator shouldNotBeNull {
            propertyName shouldBe "type"
            // Inline schemas don't have explicit mapping
            mapping shouldBe null
        }

        val firstOption = paymentProp.oneOf[0] as ObjectPropertyDefinition
        firstOption.properties?.size shouldBe 2
        firstOption.required?.size shouldBe 2

        val secondOption = paymentProp.oneOf[1] as ObjectPropertyDefinition
        secondOption.properties?.size shouldBe 2
        secondOption.required?.size shouldBe 2
    }

    @Test
    fun `oneOf DSL with discriminator mixing references and inline schemas`() {
        val schema =
            jsonSchema {
                property("value") {
                    required = true
                    oneOf {
                        discriminator("kind") {
                            // Mix references and inline schemas
                            "external" mappedTo "#/definitions/ExternalType"
                            "inline" mappedTo {
                                property("kind") {
                                    required = true
                                    string { constValue = "inline" }
                                }
                                property("data") {
                                    required = true
                                    string()
                                }
                            }
                        }
                    }
                }
            }

        val valueProp = schema.properties["value"] as OneOfPropertyDefinition
        valueProp.oneOf.size shouldBe 2

        valueProp.discriminator shouldNotBeNull {
            propertyName shouldBe "kind"
            // Only the reference is in the mapping
            mapping?.size shouldBe 1
            mapping?.get("external") shouldBe "#/definitions/ExternalType"
        }

        // First option is a reference
        val firstOption = valueProp.oneOf[0] as ReferencePropertyDefinition
        firstOption.ref shouldBe "#/definitions/ExternalType"

        // Second option is an inline object
        val secondOption = valueProp.oneOf[1] as ObjectPropertyDefinition
        secondOption.properties?.size shouldBe 2
    }

    @Test
    fun `anyOf DSL with mixed types`() {
        val schema =
            jsonSchema {
                property("id") {
                    required = true
                    anyOf {
                        description = "UUID or integer ID"
                        string { format = "uuid" }
                        integer { minimum = 1.0 }
                    }
                }
            }

        val idProp = schema.properties["id"] as AnyOfPropertyDefinition
        idProp.anyOf.size shouldBe 2
        idProp.description shouldBe "UUID or integer ID"

        val stringOption = idProp.anyOf[0] as StringPropertyDefinition
        stringOption.format shouldBe "uuid"

        val intOption = idProp.anyOf[1] as NumericPropertyDefinition
        intOption.type shouldBe listOf("integer")
    }

    @Test
    fun `allOf DSL with composition`() {
        val schema =
            jsonSchema {
                property("user") {
                    required = true
                    allOf {
                        description = "Admin user extends base user"
                        reference("#/definitions/BaseUser")
                        obj {
                            property("role") {
                                required = true
                                string { enum = listOf("admin", "superadmin") }
                            }
                            property("permissions") {
                                required = true
                                array {
                                    ofString()
                                }
                            }
                        }
                    }
                }
            }

        val userProp = schema.properties["user"] as AllOfPropertyDefinition
        userProp.allOf.size shouldBe 2
        userProp.description shouldBe "Admin user extends base user"

        val refOption = userProp.allOf[0] as ReferencePropertyDefinition
        refOption.ref shouldBe "#/definitions/BaseUser"

        val objOption = userProp.allOf[1] as ObjectPropertyDefinition
        objOption.properties?.size shouldBe 2
    }

    @Test
    fun `nested polymorphism DSL - oneOf inside allOf`() {
        val schema =
            jsonSchema {
                property("complex") {
                    allOf {
                        reference("#/definitions/Base")
                        oneOf {
                            string { description = "String variant" }
                            integer { description = "Integer variant" }
                        }
                    }
                }
            }

        val complexProp = schema.properties["complex"] as AllOfPropertyDefinition
        complexProp.allOf.size shouldBe 2

        val nestedOneOf = complexProp.allOf[1] as OneOfPropertyDefinition
        nestedOneOf.oneOf.size shouldBe 2
    }

    @Test
    fun `DSL round-trip serialization for oneOf`() {
        val schema =
            jsonSchema {
                property("value") {
                    oneOf {
                        string()
                        integer()
                    }
                }
            }

        // language=json
        val expectedJson =
            """
            {
                "type": "object",
                "properties": {
                  "value": {
                    "oneOf": [
                      {
                        "type": "string"
                      },
                      {
                        "type": "integer"
                      }
                    ]
                  }
                }
            }
            """.trimIndent()

        serializeAndDeserialize(schema, expectedJson, json)
    }

    @Test
    fun `oneOf validation - requires at least 2 options`() {
        shouldThrow<IllegalArgumentException> {
            jsonSchema {
                property("value") {
                    oneOf {
                        string()
                    }
                }
            }
        }
    }

    @Test
    fun `anyOf validation - requires at least 2 options`() {
        shouldThrow<IllegalArgumentException> {
            jsonSchema {
                property("value") {
                    anyOf {
                        string()
                    }
                }
            }
        }
    }

    @Test
    fun `allOf validation - requires at least 1 option`() {
        shouldThrow<IllegalArgumentException> {
            jsonSchema {
                property("value") {
                    allOf {
                        // No options
                    }
                }
            }
        }
    }

    @Test
    fun `discriminator validation - requires non-empty propertyName`() {
        shouldThrow<IllegalArgumentException> {
            jsonSchema {
                property("value") {
                    oneOf {
                        discriminator("") // Empty propertyName should fail
                        reference("#/definitions/A")
                        reference("#/definitions/B")
                    }
                }
            }
        }
    }

    @Test
    fun `allOf with single option is valid`() {
        val schema =
            jsonSchema {
                property("value") {
                    allOf {
                        reference("#/definitions/Base")
                    }
                }
            }

        val valueProp = schema.properties["value"] as AllOfPropertyDefinition
        valueProp.allOf.size shouldBe 1
    }

    @Test
    fun `oneOf with three options`() {
        val schema =
            jsonSchema {
                property("value") {
                    oneOf {
                        string()
                        integer()
                        boolean()
                    }
                }
            }

        val valueProp = schema.properties["value"] as OneOfPropertyDefinition
        valueProp.oneOf.size shouldBe 3
    }

    @Test
    fun `discriminator without mapping`() {
        val schema =
            jsonSchema {
                property("value") {
                    oneOf {
                        discriminator("type")
                        reference("#/definitions/TypeA")
                        reference("#/definitions/TypeB")
                    }
                }
            }

        val valueProp = schema.properties["value"] as OneOfPropertyDefinition
        valueProp.discriminator?.propertyName shouldBe "type"
        valueProp.discriminator?.mapping shouldBe null
    }

    @Test
    fun `complex real-world example - order schema with polymorphic items`() {
        val schema =
            jsonSchema {
                description = "Order with polymorphic line items"
                additionalProperties = false
                property("orderId") {
                    required = true
                    string { format = "uuid" }
                }
                property("items") {
                    required = true
                    array {
                        description = "Order line items"
                        ofObject {
                            property("itemType") {
                                required = true
                                oneOf {
                                    discriminator {
                                        "product" mappedTo "#/definitions/ProductItem"
                                        "service" mappedTo "#/definitions/ServiceItem"
                                        "discount" mappedTo "#/definitions/DiscountItem"
                                    }
                                }
                            }
                        }
                    }
                }
            }

        schema.required.size shouldBe 2

        val itemsProp = schema.properties["items"] as ArrayPropertyDefinition
        val itemsObj = itemsProp.items as ObjectPropertyDefinition
        val itemTypeProp = itemsObj.properties?.get("itemType") as OneOfPropertyDefinition
        itemTypeProp.discriminator?.propertyName shouldBe "type"
        itemTypeProp.oneOf.size shouldBe 3
    }
}
