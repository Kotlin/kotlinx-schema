@file:Suppress("FunctionOnlyReturningConstant", "unused")

package kotlinx.schema.integration.functions

import kotlinx.schema.Description
import kotlinx.schema.Schema

/**
 * Singleton object with functions for testing object function schema generation.
 *
 * This demonstrates functions defined in Kotlin objects (singletons) annotated with @Schema.
 */
object ConfigurationManager {
    /**
     * Loads configuration from a file.
     */
    @Schema
    @Description("Loads configuration from a file")
    fun loadConfig(
        @Description("Configuration file path")
        filePath: String,
        @Description("Whether to create file if it doesn't exist")
        createIfMissing: Boolean = false,
        @Description("Default values to use")
        defaults: Map<String, String>? = null,
    ): Map<String, String> = mapOf("loaded" to "true")

    /**
     * Saves configuration to a file.
     */
    @Schema
    @Description("Saves configuration to a file")
    fun saveConfig(
        @Description("Configuration file path")
        filePath: String,
        @Description("Configuration key-value pairs")
        config: Map<String, String>,
        @Description("Whether to create backup")
        createBackup: Boolean = true,
    ): Boolean = true

    /**
     * Validates configuration asynchronously.
     */
    @Schema
    @Description("Validates configuration against schema asynchronously")
    suspend fun validateConfig(
        @Description("Configuration to validate")
        config: Map<String, String>,
        @Description("Schema version")
        schemaVersion: String = "1.0",
    ): List<String> {
        return emptyList() // Return list of validation errors
    }

    /**
     * Reloads configuration asynchronously.
     */
    @Schema
    @Description("Reloads configuration from all sources asynchronously")
    suspend fun reloadConfiguration(
        @Description("Whether to clear cache")
        clearCache: Boolean = true,
        @Description("Sources to reload from")
        sources: List<String> = listOf("file", "env"),
    ): Boolean = true
}

/**
 * Another singleton object for logging operations.
 */
object LogManager {
    /**
     * Writes a log entry.
     */
    @Schema
    @Description("Writes a log entry")
    fun writeLog(
        @Description("Log level (DEBUG, INFO, WARN, ERROR)")
        level: String,
        @Description("Log message")
        message: String,
        @Description("Optional exception details")
        exception: String? = null,
        @Description("Additional context tags")
        tags: List<String> = emptyList(),
    ): Boolean = true

    /**
     * Flushes log buffer asynchronously.
     */
    @Schema
    @Description("Flushes log buffer to persistent storage asynchronously")
    suspend fun flushLogs(
        @Description("Whether to force flush even if buffer is not full")
        force: Boolean = false,
    ): Int {
        return 0 // Return number of entries flushed
    }

    /**
     * Queries logs.
     */
    @Schema
    @Description("Queries logs with filters")
    fun queryLogs(
        @Description("Start timestamp")
        startTime: Long,
        @Description("End timestamp")
        endTime: Long,
        @Description("Log levels to include")
        levels: List<String> = listOf("INFO", "WARN", "ERROR"),
        @Description("Maximum number of results")
        limit: Int = 100,
    ): List<String> = emptyList()

    /**
     * Archives old logs asynchronously.
     */
    @Schema
    @Description("Archives old logs asynchronously")
    suspend fun archiveLogs(
        @Description("Archive logs older than this timestamp")
        beforeTimestamp: Long,
        @Description("Compression format")
        compressionFormat: String = "gzip",
    ): Long {
        return 0L // Return number of archived entries
    }
}
