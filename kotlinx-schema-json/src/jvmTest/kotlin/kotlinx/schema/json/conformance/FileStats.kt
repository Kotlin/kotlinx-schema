package kotlinx.schema.json.conformance

import kotlinx.serialization.json.JsonElement

/**
 * Simple result tracking for report generation.
 * Not used for test execution - JUnit handles that via exceptions.
 */
data class SchemaTestResult(
    val testSuiteDescription: String,
    val fileName: String,
    val passed: Boolean,
    val error: Throwable? = null,
    val stage: String? = null,
    val schema: JsonElement? = null,
)

/**
 * Statistics for a collection of test results.
 */
data class FileStats(
    val fileName: String,
    val results: List<SchemaTestResult>,
) {
    val totalSchemas: Int = results.size
    val successCount: Int = results.count { it.passed }
    val failureCount: Int = results.count { !it.passed }
    val successRate: Double =
        if (totalSchemas > 0) {
            (successCount.toDouble() / totalSchemas) * 100.0
        } else {
            0.0
        }

    val failures: List<SchemaTestResult> = results.filter { !it.passed }
}

/**
 * Aggregated statistics for all test files.
 */
@JvmRecord
data class OverallStats(
    val totalFiles: Int,
    val totalSchemas: Int,
    val totalSuccess: Int,
    val totalFailures: Int,
    val successRate: Double,
) {
    companion object {
        fun from(fileStats: Collection<FileStats>): OverallStats {
            val totalFiles = fileStats.size
            val totalSchemas = fileStats.sumOf { it.totalSchemas }
            val totalSuccess = fileStats.sumOf { it.successCount }
            val totalFailures = fileStats.sumOf { it.failureCount }
            val successRate =
                if (totalSchemas > 0) {
                    (totalSuccess.toDouble() / totalSchemas) * 100.0
                } else {
                    0.0
                }

            return OverallStats(
                totalFiles = totalFiles,
                totalSchemas = totalSchemas,
                totalSuccess = totalSuccess,
                totalFailures = totalFailures,
                successRate = successRate,
            )
        }
    }
}
