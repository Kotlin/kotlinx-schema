@file:Suppress("FunctionOnlyReturningConstant", "unused")

package kotlinx.schema.integration.functions

import kotlinx.coroutines.delay
import kotlinx.schema.Description
import kotlinx.schema.Schema

/*
 * Top-level functions for testing function schema generation.
 *
 * This file contains top-level functions (both normal and suspend) annotated with @Schema
 * to verify that the KSP processor correctly generates schemas for functions at the package level.
 */

/**
 * Sends a greeting message to a person.
 */
@Schema
@Description("Sends a greeting message to a person")
fun greetPerson(
    @Description("Name of the person to greet")
    name: String,
    @Description("Optional greeting prefix (e.g., 'Hello', 'Hi')")
    greeting: String = "Hello",
): String = "$greeting, $name!"

/**
 * Calculates the sum of two numbers.
 */
@Schema
@Description("Calculates the sum of two numbers")
fun calculateSum(
    @Description("First number")
    a: Int,
    @Description("Second number")
    b: Int,
): Int = a + b

/**
 * Fetches user data asynchronously.
 */
@Schema
@Description("Fetches user data asynchronously from a remote service")
suspend fun fetchUserData(
    @Description("User ID to fetch")
    userId: Long,
    @Description("Whether to include detailed profile information")
    includeDetails: Boolean = false,
): String {
    delay(1)
    // Simulate async operation
    return "User data for $userId (details: $includeDetails)"
}

/**
 * Processes a list of items asynchronously.
 */
@Schema
@Description("Processes a list of items asynchronously")
suspend fun processItems(
    @Description("List of items to process")
    items: List<String>,
    @Description("Processing mode")
    mode: String = "default",
): List<String> {
    delay(1)
    return items.map { "$mode: $it" }
}

/**
 * Searches for products with various filters.
 */
@Schema
@Description("Searches for products with various filters")
fun searchProducts(
    @Description("Search query string")
    query: String,
    @Description("Minimum price filter")
    minPrice: Double?,
    @Description("Maximum price filter")
    maxPrice: Double?,
    @Description("Category filters")
    categories: List<String> = emptyList(),
    @Description("Whether to include out-of-stock items")
    includeOutOfStock: Boolean = false,
): List<String> = listOf("Search results for: $query")

/**
 * Updates configuration settings.
 */
@Schema
@Description("Updates configuration settings")
suspend fun updateConfiguration(
    @Description("Configuration key")
    key: String,
    @Description("Configuration value")
    value: String,
    @Description("Optional metadata")
    metadata: Map<String, String>? = null,
): Boolean {
    delay(1)
    return true
}
