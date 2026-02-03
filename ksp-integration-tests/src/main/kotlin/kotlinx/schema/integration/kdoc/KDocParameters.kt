@file:Suppress("unused", "UnusedParameter")

package kotlinx.schema.integration.kdoc

import kotlinx.schema.Schema

/**
 * Searches for users by name and age.
 *
 * @param name User name to search for
 * @param age User age filter
 * @param includeInactive Whether to include inactive users in results
 */
@Schema
fun searchUsers(
    name: String,
    age: Int?,
    includeInactive: Boolean = false,
): String = "Searching for $name, age $age"

/**
 * Creates a new user account.
 *
 * This function creates a new user with the provided details.
 * It validates the email and ensures the username is unique.
 *
 * @param username Unique username for the account
 * @param email User's email address.
 *              Must be a valid email format.
 * @param age User's age in years
 */
@Schema
fun createUser(
    username: String,
    email: String,
    age: Int,
): String = "User created: $username"

/**
 * User profile with basic information.
 *
 * @property id Unique user identifier
 * @property name Full name of the user
 * @property email Email address for notifications
 */
@Schema
data class UserProfile(
    val id: String,
    val name: String,
    val email: String,
)

/**
 * Configuration for the application.
 *
 * @property apiKey API key for authentication
 * @property timeout Connection timeout in milliseconds
 * @property retries Number of retry attempts
 */
@Schema
data class AppConfig(
    val apiKey: String,
    val timeout: Int = 5000,
    val retries: Int = 3,
)

/**
 * Product information.
 *
 * @param name Product name
 * @param price Price in USD
 * @param category Product category
 */
@Schema
data class Product(
    val name: String,
    val price: Double,
    val category: String,
)

/**
 * Updates user settings.
 *
 * @param userId ID of the user to update
 * @param settings Map of settings to update
 */
@Schema
suspend fun updateUserSettings(
    userId: String,
    settings: Map<String, String>,
): Boolean {
    kotlinx.coroutines.delay(1)
    return true
}

/**
 * Response with mixed description sources.
 *
 * Demonstrates fallback chain: annotation > @param tag > @property tag
 *
 * @param statusCode HTTP status code
 * @property data Response payload from @property tag
 */
@Schema
data class ApiResponse(
    @kotlinx.schema.Description("Response message from annotation")
    val message: String,
    val statusCode: Int,
    val data: String,
)

/**
 * Class with properties declared in body (fields).
 *
 * Demonstrates field-level KDoc extraction.
 *
 * @property computed Computed from class KDoc @property tag
 */
@Schema
class FieldsExample {
    /**
     * Field with its own KDoc.
     */
    val id: String = "default-id"

    val name: String = "default-name"

    @Suppress("MagicNumber")
    val computed: Int = 42
}
