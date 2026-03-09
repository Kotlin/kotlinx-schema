package kotlinx.schema.integration.type

import io.kotest.assertions.json.shouldEqualJson
import kotlin.test.Test

class SealedInheritanceTest {
    @Test
    fun `should include inherited properties from sealed base in KSP`() {
        val schema = SealedBase::class.jsonSchemaString

        // SubclassA should have baseProp
        schema shouldEqualJson
            $$"""
            {
                "$schema": "https://json-schema.org/draft/2020-12/schema",
                "$id": "kotlinx.schema.integration.type.SealedBase",
                "type": "object",
                "additionalProperties": false,
                "oneOf": [
                    {
                        "$ref": "#/$defs/kotlinx.schema.integration.type.SealedBase.SubclassA"
                    },
                    {
                        "$ref": "#/$defs/kotlinx.schema.integration.type.SealedBase.SubclassB"
                    }
                ],
                "$defs": {
                    "kotlinx.schema.integration.type.SealedBase.SubclassA": {
                        "type": "object",
                        "properties": {
                            "type": {
                                "type": "string",
                                "const": "kotlinx.schema.integration.type.SealedBase.SubclassA"
                            },
                            "propA": {
                                "type": "integer",
                                "description": "A's property"
                            },
                            "baseProp": {
                                "type": "string",
                                "description": "Base property"
                            }
                        },
                        "required": [
                            "type",
                            "propA",
                            "baseProp"
                        ],
                        "additionalProperties": false
                    },
                    "kotlinx.schema.integration.type.SealedBase.SubclassB": {
                        "type": "object",
                        "properties": {
                            "type": {
                                "type": "string",
                                "const": "kotlinx.schema.integration.type.SealedBase.SubclassB"
                            },
                            "baseProp": {
                                "type": "string",
                                "description": "Base property"
                            }
                        },
                        "required": [
                            "type",
                            "baseProp"
                        ],
                        "additionalProperties": false
                    }
                }
            }
            """.trimIndent()
    }
}
