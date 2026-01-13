@file:Suppress("FunctionOnlyReturningConstant", "unused")

package kotlinx.schema.integration.functions

import kotlinx.schema.Description
import kotlinx.schema.Schema

/**
 * Class with companion object functions for testing.
 *
 * This demonstrates functions defined in companion objects annotated with @Schema.
 * Companion objects provide factory methods and static-like functions.
 */
class DatabaseConnection(
    var connectionString: String,
) {
    companion object {
        /**
         * Creates a new database connection.
         */
        @Schema
        @Description("Creates a new database connection")
        fun create(
            @Description("Database host")
            host: String,
            @Description("Database port")
            port: Int = 5432,
            @Description("Database name")
            database: String,
            @Description("Connection timeout in seconds")
            timeout: Int = 30,
        ): String = "Connected to $host:$port/$database"

        /**
         * Tests database connectivity.
         */
        @Schema
        @Description("Tests database connectivity")
        fun testConnection(
            @Description("Connection string")
            connectionString: String,
            @Description("Number of retry attempts")
            retries: Int = 3,
        ): Boolean = true

        /**
         * Establishes a connection asynchronously.
         */
        @Schema
        @Description("Establishes a database connection asynchronously")
        suspend fun connectAsync(
            @Description("Database host")
            host: String,
            @Description("Database port")
            port: Int,
            @Description("Username")
            username: String,
            @Description("Password")
            password: String,
            @Description("Connection options")
            options: Map<String, String>? = null,
        ): String = "Connection established"

        /**
         * Closes all connections asynchronously.
         */
        @Schema
        @Description("Closes all database connections asynchronously")
        suspend fun closeAllConnections(
            @Description("Whether to force close active transactions")
            force: Boolean = false,
        ): Int {
            return 0 // Return number of closed connections
        }
    }
}

/**
 * Factory class for creating API clients.
 */
class ApiClient(
    val url: String,
) {
    companion object {
        /**
         * Builds an API client with configuration.
         */
        @Schema
        @Description("Builds an API client with configuration")
        fun build(
            @Description("API base URL")
            baseUrl: String,
            @Description("API key for authentication")
            apiKey: String,
            @Description("Request timeout in milliseconds")
            timeout: Long = 5000,
            @Description("Whether to enable debug logging")
            debug: Boolean = false,
            @Description("Custom headers")
            headers: Map<String, String> = emptyMap(),
        ): String = "API client for $baseUrl"

        /**
         * Creates a default API client.
         */
        @Schema
        @Description("Creates a default API client with standard settings")
        fun createDefault(
            @Description("Environment (dev, staging, prod)")
            environment: String = "prod",
        ): String = "Default API client for $environment"

        /**
         * Initializes API client asynchronously.
         */
        @Schema
        @Description("Initializes API client asynchronously with health check")
        suspend fun initializeAsync(
            @Description("API base URL")
            baseUrl: String,
            @Description("API key")
            apiKey: String,
            @Description("Whether to verify SSL certificates")
            verifySSL: Boolean = true,
        ): String = "Initialized: $baseUrl"

        /**
         * Discovers API endpoints asynchronously.
         */
        @Schema
        @Description("Discovers available API endpoints asynchronously")
        suspend fun discoverEndpoints(
            @Description("API base URL")
            baseUrl: String,
            @Description("API version to query")
            version: String = "v1",
        ): List<String> = listOf("/users", "/products", "/orders")
    }
}

/**
 * Validator class with validation functions in companion.
 */
class DataValidator(
    val strict: Boolean = false,
) {
    companion object {
        /**
         * Validates email format.
         */
        @Schema
        @Description("Validates email format")
        fun validateEmail(
            @Description("Email address to validate")
            email: String,
            @Description("Whether to check DNS records")
            checkDNS: Boolean = false,
        ): Boolean = true

        /**
         * Validates a data object.
         */
        @Schema
        @Description("Validates a data object against rules")
        fun validate(
            @Description("Data fields to validate")
            data: Map<String, String>,
            @Description("Validation rules")
            rules: List<String>,
            @Description("Whether to stop on first error")
            stopOnFirstError: Boolean = true,
        ): List<String> {
            return emptyList() // Return list of validation errors
        }

        /**
         * Validates data asynchronously with remote checks.
         */
        @Schema
        @Description("Validates data asynchronously with remote API checks")
        suspend fun validateAsync(
            @Description("Data to validate")
            data: Map<String, String>,
            @Description("Remote validation endpoints")
            endpoints: List<String>,
            @Description("Timeout for remote checks in milliseconds")
            timeout: Long = 3000,
        ): Map<String, List<String>> {
            return emptyMap() // Return field -> errors mapping
        }

        /**
         * Performs batch validation asynchronously.
         */
        @Schema
        @Description("Performs batch validation asynchronously")
        suspend fun batchValidate(
            @Description("List of items to validate")
            items: List<Map<String, String>>,
            @Description("Validation schema version")
            schemaVersion: String = "1.0",
        ): List<Boolean> = items.map { true }
    }
}
