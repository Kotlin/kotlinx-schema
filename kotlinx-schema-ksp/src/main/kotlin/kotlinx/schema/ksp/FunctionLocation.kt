package kotlinx.schema.ksp

import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFile
import com.google.devtools.ksp.symbol.KSFunctionDeclaration

/**
 * Represents the location/context of a function declaration.
 *
 * This enum is used to determine how to generate schema code for a function,
 * as different locations require different generation strategies:
 * - Top-level functions generate standalone functions
 * - Instance methods generate KClass extensions on the parent class
 * - Companion methods generate KClass extensions on the parent class
 * - Object methods generate KClass extensions on the object
 */
internal enum class FunctionLocation {
    /**
     * Function is declared at the top level (package level).
     *
     * Example:
     * ```kotlin
     * package com.example
     *
     * @Schema
     * fun greetPerson(name: String): String = "Hello, $name"
     * ```
     *
     * Generated code:
     * ```kotlin
     * fun greetPersonJsonSchemaString(): String = """..."""
     * ```
     */
    TOP_LEVEL,

    /**
     * Function is an instance method in a regular class.
     *
     * Example:
     * ```kotlin
     * class UserService {
     *     @Schema
     *     fun registerUser(username: String): String = "registered"
     * }
     * ```
     *
     * Generated code:
     * ```kotlin
     * fun KClass<UserService>.registerUserJsonSchemaString(): String = """..."""
     * ```
     */
    INSTANCE_METHOD,

    /**
     * Function is declared in a companion object.
     *
     * Example:
     * ```kotlin
     * class DatabaseConnection {
     *     companion object {
     *         @Schema
     *         fun create(host: String): DatabaseConnection = DatabaseConnection(host)
     *     }
     * }
     * ```
     *
     * Generated code:
     * ```kotlin
     * fun KClass<DatabaseConnection>.createJsonSchemaString(): String = """..."""
     * ```
     */
    COMPANION_METHOD,

    /**
     * Function is declared in a singleton object.
     *
     * Example:
     * ```kotlin
     * object ConfigurationManager {
     *     @Schema
     *     fun loadConfig(filePath: String): Map<String, String> = ...
     * }
     * ```
     *
     * Generated code:
     * ```kotlin
     * fun KClass<ConfigurationManager>.loadConfigJsonSchemaString(): String = """..."""
     * ```
     */
    OBJECT_METHOD,
}

/**
 * Detects the location/context of this function declaration.
 *
 * This extension function analyzes the parent declaration to determine where
 * the function is defined, which determines how the schema code should be generated.
 *
 * **Detection logic:**
 * 1. If parent is null or KSFile → TOP_LEVEL
 * 2. If parent is KSClassDeclaration:
 *    - If isCompanionObject → COMPANION_METHOD
 *    - If classKind is OBJECT → OBJECT_METHOD
 *    - Otherwise → INSTANCE_METHOD
 * 3. Fallback → TOP_LEVEL (for safety)
 *
 * **Edge cases handled:**
 * - Extension functions are treated based on their declaration location
 * - Local functions (inside function bodies) fallback to TOP_LEVEL but should never be annotated with @Schema
 * - Nested classes follow the same rules
 *
 * @return The location/context of this function
 */
internal fun KSFunctionDeclaration.detectLocation(): FunctionLocation {
    val parent = parentDeclaration

    return when {
        // Top-level function (declared at package level)
        parent == null || parent is KSFile -> FunctionLocation.TOP_LEVEL

        // Function inside a class-like declaration
        parent is KSClassDeclaration ->
            when {
                // Companion object function
                parent.isCompanionObject -> FunctionLocation.COMPANION_METHOD

                // Singleton object function
                parent.classKind == ClassKind.OBJECT -> FunctionLocation.OBJECT_METHOD

                // Regular class instance method (including data classes, sealed classes, etc.)
                else -> FunctionLocation.INSTANCE_METHOD
            }

        // Fallback for any other cases (should be rare)
        // This handles edge cases like local functions, but they shouldn't have @Schema
        else -> FunctionLocation.TOP_LEVEL
    }
}
