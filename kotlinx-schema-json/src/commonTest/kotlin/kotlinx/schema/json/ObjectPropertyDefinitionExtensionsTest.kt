package kotlinx.schema.json

import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import kotlin.test.Test
import kotlin.test.assertNull

class ObjectPropertyDefinitionExtensionsTest {
    @Test
    fun `should access all types of properties in ObjectPropertyDefinition`() {
        val schema =
            jsonSchema {
                property("obj") {
                    obj {
                        property("str") { string { description = "string description" } }
                        property("num") { number { minimum = 10.0 } }
                        property("bool") { boolean { } }
                        property("nestedObj") { obj { } }
                        property("arr") { array { minItems = 1 } }
                        property("ref") { reference("#/defs/Other") }
                        property("oneOf") {
                            oneOf {
                                string()
                                integer()
                            }
                        }
                        property("anyOf") {
                            anyOf {
                                string()
                                integer()
                            }
                        }
                        property("allOf") {
                            allOf {
                                string()
                                integer()
                            }
                        }
                    }
                }
            }

        val objDef = schema.objectProperty("obj")
        assertObjectPropertyDefinition(objDef)

        assertNull(objDef?.stringProperty("num"))
    }

    private fun assertObjectPropertyDefinition(objDef: ObjectPropertyDefinition?) {
        objDef.shouldNotBeNull {
            stringProperty("str") shouldNotBeNull {
                description shouldBe "string description"
            }
            numericProperty("num") shouldNotBeNull {
                minimum shouldBe 10.0
            }
            booleanProperty("bool") shouldNotBeNull {
                type shouldBe listOf("boolean")
            }
            objectProperty("nestedObj").shouldNotBeNull()
            arrayProperty("arr") shouldNotBeNull {
                minItems shouldBe 1
            }
            referenceProperty("ref") shouldNotBeNull {
                ref shouldBe "#/defs/Other"
            }
            oneOfProperty("oneOf") shouldNotBeNull {
                oneOf.size shouldBe 2
            }
            anyOfProperty("anyOf") shouldNotBeNull {
                anyOf.size shouldBe 2
            }
            allOfProperty("allOf") shouldNotBeNull {
                allOf.size shouldBe 2
            }
        }
    }

    @Test
    fun `should access all types of properties in JsonSchemaDefinition`() {
        val schema =
            jsonSchema {
                property("str") { string { description = "string description" } }
                property("num") { number { minimum = 10.0 } }
                property("bool") { boolean { } }
                property("obj") { obj { } }
                property("arr") { array { minItems = 1 } }
                property("ref") { reference("#/defs/Other") }
                property("oneOf") {
                    oneOf {
                        string()
                        integer()
                    }
                }
                property("anyOf") {
                    anyOf {
                        string()
                        integer()
                    }
                }
                property("allOf") {
                    allOf {
                        string()
                        integer()
                    }
                }
            }

        schema.stringProperty("str") shouldNotBeNull {
            description shouldBe "string description"
        }
        schema.numericProperty("num") shouldNotBeNull {
            minimum shouldBe 10.0
        }
        schema.booleanProperty("bool").shouldNotBeNull()
        schema.objectProperty("obj").shouldNotBeNull()
        schema.arrayProperty("arr") shouldNotBeNull {
            minItems shouldBe 1
        }
        schema.referenceProperty("ref") shouldNotBeNull {
            ref shouldBe "#/defs/Other"
        }
        schema.oneOfProperty("oneOf") shouldNotBeNull {
            oneOf.size shouldBe 2
        }
        schema.anyOfProperty("anyOf") shouldNotBeNull {
            anyOf.size shouldBe 2
        }
        schema.allOfProperty("allOf") shouldNotBeNull {
            allOf.size shouldBe 2
        }

        assertNull(schema.stringProperty("num"))
    }
}
