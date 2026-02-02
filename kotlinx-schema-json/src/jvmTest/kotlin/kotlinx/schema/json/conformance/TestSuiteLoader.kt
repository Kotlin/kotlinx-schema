package kotlinx.schema.json.conformance

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import java.io.File

/**
 * Loads and parses JSON Schema Test Suite files from the draft2020-12 directory.
 */
object TestSuiteLoader {
    private val json =
        Json {
            ignoreUnknownKeys = true
        }

    /**
     * Base directory for test suite files relative to the module root.
     */
    private val testSuiteBasePath = File("test-suite/tests/draft2020-12")

    /**
     * Loads a single test suite file by name.
     * @param fileName Name of the JSON file (e.g., "type.json")
     * @return List of test suites from that file
     */
    fun loadTestSuiteFile(fileName: String): List<TestSuite> {
        val file = File(testSuiteBasePath, fileName)
        require(file.exists()) { "Test suite file not found: ${file.absolutePath}" }

        val content = file.readText()
        val jsonArray = json.parseToJsonElement(content)

        return json.decodeFromJsonElement<List<TestSuite>>(jsonArray)
    }

    /**
     * Loads all test suite files from the draft2020-12 directory.
     * @param includeOptional Whether to include files from the optional/ subdirectory
     * @return Map of file name to list of test suites
     */
    fun loadAllTestSuiteFiles(includeOptional: Boolean = false): Map<String, List<TestSuite>> {
        val files =
            testSuiteBasePath
                .listFiles { file ->
                    file.isFile && file.extension == "json"
                }?.sorted()
                .orEmpty()

        val result = mutableMapOf<String, List<TestSuite>>()

        files.forEach { file ->
            try {
                result[file.name] = loadTestSuiteFile(file.name)
            } catch (e: Exception) {
                println("Warning: Failed to load ${file.name}: ${e.message}")
            }
        }

        if (includeOptional) {
            val optionalDir = File(testSuiteBasePath, "optional")
            if (optionalDir.exists()) {
                val optionalFiles =
                    optionalDir
                        .listFiles { file ->
                            file.isFile && file.extension == "json"
                        }?.sorted()
                        .orEmpty()

                optionalFiles.forEach { file ->
                    try {
                        val testSuites = loadTestSuiteFile("optional/${file.name}")
                        result["optional/${file.name}"] = testSuites
                    } catch (e: Exception) {
                        println("Warning: Failed to load optional/${file.name}: ${e.message}")
                    }
                }
            }
        }

        return result
    }
}
