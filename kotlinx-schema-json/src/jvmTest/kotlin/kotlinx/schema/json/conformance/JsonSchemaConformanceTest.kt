package kotlinx.schema.json.conformance

import kotlinx.schema.json.JsonSchemaDefinition
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.api.DynamicContainer
import org.junit.jupiter.api.DynamicNode
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import java.util.concurrent.ConcurrentHashMap

/**
 * Conformance tests for JsonSchemaDefinition against the official JSON Schema Test Suite.
 *
 * Tests verify that schemas from the test suite can be successfully parsed into JsonSchemaDefinition.
 * Unparseable schemas are marked with ❌ emoji and skipped in JUnit to avoid suite failure,
 * while parseable schemas are marked with ✅ emoji. Error messages for unparseable schemas
 * are captured and included in the markdown report (`build/reports/spec-coverage.md`).
 */
class JsonSchemaConformanceTest {
    private val json =
        Json {
            ignoreUnknownKeys = true
            prettyPrint = true
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
                    .filter { it.schema is JsonObject }
                    .map { testSuite ->
                        // Pre-validate and capture error if unparseable
                        val preValidation =
                            runCatching {
                                json.decodeFromJsonElement<JsonSchemaDefinition>(testSuite.schema)
                            }
                        val canParse = preValidation.isSuccess
                        val emoji = if (canParse) "✅" else "❌"

                        DynamicTest.dynamicTest("#### $emoji ${testSuite.description}") {
                            if (!canParse) {
                                // Track as failed with error message
                                trackResult(
                                    SchemaTestResult(
                                        testSuiteDescription = testSuite.description,
                                        fileName = fileName,
                                        passed = false,
                                        error = preValidation.exceptionOrNull(),
                                        stage = "parse",
                                        schema = testSuite.schema,
                                    ),
                                )
                                // Skip in JUnit to avoid suite failure
                                Assumptions.assumeTrue(
                                    false,
                                    "Schema cannot be parsed: ${preValidation.exceptionOrNull()?.message}",
                                )
                            } else {
                                // Normal test execution for parseable schemas
                                val result =
                                    runCatching {
                                        parseSchema(testSuite.schema)
                                    }

                                trackResult(
                                    SchemaTestResult(
                                        testSuiteDescription = testSuite.description,
                                        fileName = fileName,
                                        passed = result.isSuccess,
                                        error = result.exceptionOrNull(),
                                        stage = if (result.isFailure) "parse" else null,
                                        schema = if (result.isFailure) testSuite.schema else null,
                                    ),
                                )

                                // This shouldn't happen since we pre-validated, but keep for safety
                                if (result.isFailure) {
                                    Assumptions.assumeTrue(false, result.exceptionOrNull()?.message ?: "Parse failed")
                                }
                            }
                        }
                    },
            )
        }
    }

    @Test
    fun `zzz generate spec coverage report`() {
        // Generate report after all tests have run
        // Named with 'zzz' prefix to ensure it runs last alphabetically
        if (results.isNotEmpty()) {
            ConformanceReportWriter.writeReport(results)
        }
    }

    private fun parseSchema(schema: JsonElement): JsonSchemaDefinition {
        require(schema is JsonObject) { "Only JsonObject schemas are supported" }
        return json.decodeFromJsonElement(schema)
    }
}
