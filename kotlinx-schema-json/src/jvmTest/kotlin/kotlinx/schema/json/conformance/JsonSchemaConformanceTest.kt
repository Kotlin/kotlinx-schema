package kotlinx.schema.json.conformance

import kotlinx.schema.json.JsonSchema
import kotlinx.schema.json.JsonSchemaConstants
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.DynamicContainer
import org.junit.jupiter.api.DynamicNode
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.fail
import java.util.concurrent.ConcurrentHashMap

/**
 * Conformance tests for JsonSchema against the official JSON Schema Test Suite.
 *
 * Tests verify that schemas from the test suite can be successfully parsed into JsonSchema.
 * Tests fail if parsing fails. Error messages are captured and included in the markdown report
 * (`build/reports/spec-coverage.md`).
 */
class JsonSchemaConformanceTest {
    private val json =
        Json {
            ignoreUnknownKeys = false
        }

    companion object {
        @JvmStatic
        private val results = ConcurrentHashMap<String, SchemaTestResult>()

        @JvmStatic
        @AfterAll
        fun tearDown() {
            System.err.println("=== @AfterAll tearDown() called with ${results.size} results ===")
            if (results.isNotEmpty()) {
                ConformanceReportWriter.writeReport(results)
            }
        }

        @JvmStatic
        fun trackResult(result: SchemaTestResult) {
            results["${result.fileName}:${result.testSuiteDescription}"] = result
        }
    }

    @TestFactory
    fun `parse all schemas from draft2020-12`(): List<DynamicNode> {
        val allFiles = TestSuiteLoader.loadAllTestSuiteFiles(includeOptional = true)

        return allFiles.map { (fileName, testSuites) ->
            DynamicContainer.dynamicContainer(
                fileName,
                testSuites
                    // Filter out top-level boolean schemas since JsonSchema only supports object schemas
                    // BooleanSchemaDefinition is supported as PropertyDefinition (nested in objects)
                    .filter { it.schema is JsonObject }
                    .map { testSuite ->
                        DynamicTest.dynamicTest(testSuite.description) {
                            val result =
                                runCatching {
                                    parseSchema(testSuite.schema)
                                }

                            val failure = result.exceptionOrNull()
                            trackResult(
                                SchemaTestResult(
                                    testSuiteDescription = testSuite.description,
                                    fileName = fileName,
                                    passed = result.isSuccess,
                                    error = failure,
                                    stage = if (result.isFailure) "parse" else null,
                                    schema = if (result.isFailure) testSuite.schema else null,
                                ),
                            )

                            if (result.isFailure) {
                                fail("Parse failed: ${failure?.message}", failure)
                            }
                        }
                    },
            )
        }
    }

    @Test
    fun `generate spec coverage report`() {
        // Generate report after all tests have run
        // Named with 'zzz' prefix to ensure it runs last alphabetically
        if (results.isNotEmpty()) {
            ConformanceReportWriter.writeReport(results)
        }
    }

    private fun parseSchema(schema: JsonElement): JsonSchema {
        require(schema is JsonObject) { "Only JsonObject schemas are supported at top level" }

        // Extract unknown keywords as annotations
        val unknownKeywords =
            schema.keys.filterNot { key ->
                key in JsonSchemaConstants.KNOWN_KEYWORDS || key == "annotations"
            }

        val annotations =
            if (unknownKeywords.isEmpty()) {
                null
            } else {
                unknownKeywords.associateWith { schema[it]!! }
            }

        // Create a cleaned JSON object with only known keywords
        val cleanedJson =
            if (annotations != null) {
                buildJsonObject {
                    schema.forEach { (key, value) ->
                        if (key !in unknownKeywords) {
                            put(key, value)
                        }
                    }
                }
            } else {
                schema
            }

        // Deserialize from the cleaned JSON
        val jsonSchema = json.decodeFromJsonElement<JsonSchema>(cleanedJson)

        // Inject annotations if present
        return if (annotations != null) {
            jsonSchema.copy(annotations = annotations)
        } else {
            jsonSchema
        }
    }
}
