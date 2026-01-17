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
                name = "Test"
                schema {
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
            }

        val objDef = schema.schema.objectProperty("obj")
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
                minItems shouldBe 1u
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
                name = "Test"
                schema {
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
            }

        val schemaDef = schema.schema

        schemaDef.stringProperty("str") shouldNotBeNull {
            description shouldBe "string description"
        }
        schemaDef.numericProperty("num") shouldNotBeNull {
            minimum shouldBe 10.0
        }
        schemaDef.booleanProperty("bool").shouldNotBeNull()
        schemaDef.objectProperty("obj").shouldNotBeNull()
        schemaDef.arrayProperty("arr") shouldNotBeNull {
            minItems shouldBe 1u
        }
        schemaDef.referenceProperty("ref") shouldNotBeNull {
            ref shouldBe "#/defs/Other"
        }
        schemaDef.oneOfProperty("oneOf") shouldNotBeNull {
            oneOf.size shouldBe 2
        }
        schemaDef.anyOfProperty("anyOf") shouldNotBeNull {
            anyOf.size shouldBe 2
        }
        schemaDef.allOfProperty("allOf") shouldNotBeNull {
            allOf.size shouldBe 2
        }

        assertNull(schemaDef.stringProperty("num"))
    }
}
