@file:Suppress("FunctionOnlyReturningConstant", "unused")

package kotlinx.schema.integration.functions

import kotlinx.coroutines.delay
import kotlinx.schema.Description
import kotlinx.schema.Schema

/**
 * Service class with instance functions (both normal and suspend) for testing.
 *
 * This class demonstrates instance/member functions annotated with @Schema
 * to verify that the KSP processor correctly generates schemas for class methods.
 */
class UserService {
    /**
     * Registers a new user.
     */
    @Schema
    @Description("Registers a new user in the system")
    fun registerUser(
        @Description("Username for the new account")
        username: String,
        @Description("Email address")
        email: String,
        @Description("User's age")
        age: Int?,
        @Description("Whether to send welcome email")
        sendWelcomeEmail: Boolean = true,
    ): String = "User registered: $username"

    /**
     * Updates user profile information.
     */
    @Schema
    @Description("Updates user profile information")
    fun updateProfile(
        @Description("User ID")
        userId: Long,
        @Description("New display name")
        displayName: String,
        @Description("Bio text")
        bio: String? = null,
        @Description("Profile tags")
        tags: List<String> = emptyList(),
    ): Boolean = true

    /**
     * Authenticates a user asynchronously.
     */
    @Schema
    @Description("Authenticates a user asynchronously")
    suspend fun authenticateUser(
        @Description("Username or email")
        identifier: String,
        @Description("Password")
        password: String,
        @Description("Remember session")
        rememberMe: Boolean = false,
    ): String {
        delay(1)
        // Simulate async authentication
        return "Auth token for $identifier"
    }

    /**
     * Loads user preferences asynchronously.
     */
    @Schema
    @Description("Loads user preferences asynchronously")
    suspend fun loadUserPreferences(
        @Description("User ID")
        userId: Long,
        @Description("Preference categories to load")
        categories: List<String>? = null,
    ): Map<String, String> {
        delay(1)
        return mapOf("theme" to "dark", "language" to "en")
    }

    /**
     * Deletes a user account.
     */
    @Schema
    @Description("Deletes a user account permanently")
    fun deleteAccount(
        @Description("User ID to delete")
        userId: Long,
        @Description("Confirmation token")
        confirmationToken: String,
        @Description("Reason for deletion")
        reason: String? = null,
    ): Boolean = true

    /**
     * Sends a notification to multiple users asynchronously.
     */
    @Schema
    @Description("Sends a notification to multiple users asynchronously")
    suspend fun sendBulkNotification(
        @Description("List of user IDs to notify")
        userIds: List<Long>,
        @Description("Notification title")
        title: String,
        @Description("Notification message")
        message: String,
        @Description("Priority level")
        priority: String = "normal",
    ): Int {
        delay(1)
        return userIds.size
    }
}

/**
 * Repository class with data access functions.
 */
class ProductRepository {
    /**
     * Finds products by category.
     */
    @Schema
    @Description("Finds products by category")
    fun findByCategory(
        @Description("Category name")
        category: String,
        @Description("Maximum number of results")
        limit: Int = 10,
        @Description("Page offset")
        offset: Int = 0,
    ): List<String> = emptyList()

    /**
     * Saves a product asynchronously.
     */
    @Schema
    @Description("Saves a product to the database asynchronously")
    suspend fun saveProduct(
        @Description("Product ID (null for new products)")
        id: Long?,
        @Description("Product name")
        name: String,
        @Description("Product price")
        price: Double,
        @Description("Stock quantity")
        stock: Int = 0,
    ): Long {
        delay(1)
        @Suppress("MagicNumber")
        return 12345L
    }
}
