@file:Suppress("unused")

package kotlinx.schema.generator.json

import io.kotest.assertions.json.shouldEqualJson
import kotlinx.serialization.json.Json
import kotlin.test.Test

class NestedSealedHierarchyTest {
    //region Fixture

    sealed class Vehicle {
        sealed class Motorized : Vehicle() {
            data class Car(
                val doors: Int,
            ) : Motorized()

            data class Truck(
                val payload: Double,
            ) : Motorized()
        }

        data class Bicycle(
            val gears: Int,
        ) : Vehicle()
    }

    data class Delivery(val vehicle: Vehicle?)

    //endregion

    private val generator =
        ReflectionClassJsonSchemaGenerator(
            json = Json { encodeDefaults = false },
            config = JsonSchemaConfig.Strict,
        )

    @Test
    fun `nullable vehicle uses oneOf with null and ref to sealed Vehicle`() {
        val schema = generator.generateSchemaString(Delivery::class)

        // language=JSON
        schema shouldEqualJson
            $$"""
            {
              "$schema": "https://json-schema.org/draft/2020-12/schema",
              "$id": "kotlinx.schema.generator.json.NestedSealedHierarchyTest.Delivery",
              "type": "object",
              "properties": {
                "vehicle": {
                  "oneOf": [
                    { "type": "null" },
                    { "$ref": "#/$defs/kotlinx.schema.generator.json.NestedSealedHierarchyTest.Vehicle" }
                  ]
                }
              },
              "required": ["vehicle"],
              "additionalProperties": false,
              "$defs": {
                "kotlinx.schema.generator.json.NestedSealedHierarchyTest.Vehicle": {
                  "oneOf": [
                    { "$ref": "#/$defs/kotlinx.schema.generator.json.NestedSealedHierarchyTest.Vehicle.Bicycle" },
                    { "$ref": "#/$defs/kotlinx.schema.generator.json.NestedSealedHierarchyTest.Vehicle.Motorized" }
                  ]
                },
                "kotlinx.schema.generator.json.NestedSealedHierarchyTest.Vehicle.Bicycle": {
                  "type": "object",
                  "properties": {
                    "type": {
                      "type": "string",
                      "const": "kotlinx.schema.generator.json.NestedSealedHierarchyTest.Vehicle.Bicycle"
                    },
                    "gears": { "type": "integer" }
                  },
                  "required": ["type", "gears"],
                  "additionalProperties": false
                },
                "kotlinx.schema.generator.json.NestedSealedHierarchyTest.Vehicle.Motorized.Car": {
                  "type": "object",
                  "properties": {
                    "type": {
                      "type": "string",
                      "const": "kotlinx.schema.generator.json.NestedSealedHierarchyTest.Vehicle.Motorized.Car"
                    },
                    "doors": { "type": "integer" }
                  },
                  "required": ["type", "doors"],
                  "additionalProperties": false
                },
                "kotlinx.schema.generator.json.NestedSealedHierarchyTest.Vehicle.Motorized.Truck": {
                  "type": "object",
                  "properties": {
                    "type": {
                      "type": "string",
                      "const": "kotlinx.schema.generator.json.NestedSealedHierarchyTest.Vehicle.Motorized.Truck"
                    },
                    "payload": { "type": "number" }
                  },
                  "required": ["type", "payload"],
                  "additionalProperties": false
                },
                "kotlinx.schema.generator.json.NestedSealedHierarchyTest.Vehicle.Motorized": {
                  "oneOf": [
                    { "$ref": "#/$defs/kotlinx.schema.generator.json.NestedSealedHierarchyTest.Vehicle.Motorized.Car" },
                    { "$ref": "#/$defs/kotlinx.schema.generator.json.NestedSealedHierarchyTest.Vehicle.Motorized.Truck" }
                  ]
                }
              }
            }
            """.trimIndent()
    }

    @Test
    fun `nested sealed class should be oneOf in defs`() {
        val schema = generator.generateSchemaString(Vehicle::class)

        // language=JSON
        schema shouldEqualJson
            $$"""
            {
              "$schema": "https://json-schema.org/draft/2020-12/schema",
              "$id": "kotlinx.schema.generator.json.NestedSealedHierarchyTest.Vehicle",
              "type": "object",
              "additionalProperties": false,
              "oneOf": [
                { "$ref": "#/$defs/kotlinx.schema.generator.json.NestedSealedHierarchyTest.Vehicle.Bicycle" },
                { "$ref": "#/$defs/kotlinx.schema.generator.json.NestedSealedHierarchyTest.Vehicle.Motorized" }
              ],
              "$defs": {
                "kotlinx.schema.generator.json.NestedSealedHierarchyTest.Vehicle.Bicycle": {
                  "type": "object",
                  "properties": {
                    "type": {
                      "type": "string",
                      "const": "kotlinx.schema.generator.json.NestedSealedHierarchyTest.Vehicle.Bicycle"
                    },
                    "gears": { "type": "integer" }
                  },
                  "required": ["type", "gears"],
                  "additionalProperties": false
                },
                "kotlinx.schema.generator.json.NestedSealedHierarchyTest.Vehicle.Motorized.Car": {
                  "type": "object",
                  "properties": {
                    "type": {
                      "type": "string",
                      "const": "kotlinx.schema.generator.json.NestedSealedHierarchyTest.Vehicle.Motorized.Car"
                    },
                    "doors": { "type": "integer" }
                  },
                  "required": ["type", "doors"],
                  "additionalProperties": false
                },
                "kotlinx.schema.generator.json.NestedSealedHierarchyTest.Vehicle.Motorized.Truck": {
                  "type": "object",
                  "properties": {
                    "type": {
                      "type": "string",
                      "const": "kotlinx.schema.generator.json.NestedSealedHierarchyTest.Vehicle.Motorized.Truck"
                    },
                    "payload": { "type": "number" }
                  },
                  "required": ["type", "payload"],
                  "additionalProperties": false
                },
                "kotlinx.schema.generator.json.NestedSealedHierarchyTest.Vehicle.Motorized": {
                  "oneOf": [
                    { "$ref": "#/$defs/kotlinx.schema.generator.json.NestedSealedHierarchyTest.Vehicle.Motorized.Car" },
                    { "$ref": "#/$defs/kotlinx.schema.generator.json.NestedSealedHierarchyTest.Vehicle.Motorized.Truck" }
                  ]
                }
              }
            }
            """.trimIndent()
    }
}
