package kotlinx.schema.json.conformance

import java.io.File

/**
 * Writes conformance test reports to disk.
 */
object ConformanceReportWriter {
    /**
     * Generates and writes the spec coverage report to disk.
     *
     * @param results Map of test results keyed by fileName:testDescription
     * @param outputPath Path to write the report (default: kotlinx-schema-json/build/reports/spec-coverage.md)
     */
    fun writeReport(
        results: Map<String, SchemaTestResult>,
        outputPath: String = "build/reports/spec-coverage.md",
    ) {
        val fileStats =
            results.values
                .groupBy { it.fileName }
                .mapValues { (fileName, fileResults) ->
                    FileStats(fileName, fileResults)
                }

        val report = SpecCoverageReportGenerator.generateMarkdown(fileStats)
        val reportFile = File(outputPath)
        reportFile.parentFile.mkdirs()
        reportFile.writeText(report)
        println("\n📊 Spec coverage report generated: ${reportFile.absolutePath}")
    }
}
