package kotlinx.schema.json.conformance

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/**
 * Represents a single test suite from the JSON Schema Test Suite.
 * Each test suite contains a schema and multiple test cases.
 */
@Serializable
data class TestSuite(
    val description: String,
    val schema: JsonElement,
)
