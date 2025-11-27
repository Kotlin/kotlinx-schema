package kotlinx.schema.generator.core.ir

import kotlinx.schema.generator.core.Config

/**
 * Utility object for annotation-based introspection, providing methods to process annotations,
 * especially those related to descriptions.
 *
 * This object provides a configurable mechanism for recognizing description annotations from
 * multiple frameworks (kotlinx-schema, Jackson, LangChain4j, Koog, etc.) by their simple names.
 * Configuration is loaded from `kotlinx-schema.properties` on the classpath.
 *
 * ## Configuration
 *
 * The annotation detection behavior is controlled by two properties in `kotlinx-schema.properties`:
 *
 * - `introspector.annotations.description.names`: Comma-separated list of annotation simple names
 *   to recognize as description providers (e.g., "Description,LLMDescription,P")
 * - `introspector.annotations.description.attributes`: Comma-separated list of annotation parameter
 *   names that contain description text (e.g., "value,description")
 *
 * ## Customizing Configuration
 *
 * To add support for custom annotations, create `kotlinx-schema.properties` in your project's
 * `src/main/resources/` directory (or `src/commonMain/resources/` for multiplatform projects):
 *
 * ```properties
 * introspector.annotations.description.names=Description,MyCustomDescription
 * introspector.annotations.description.attributes=value,description,text
 * ```
 *
 * Your project's properties file will take precedence over the library's default configuration.
 *
 * @see getDescriptionFromAnnotation
 * @see Config
 */
public object Introspections {
    /**
     * Set of lowercase annotation simple names recognized as description providers.
     *
     * @see Config.descriptionAnnotationNames
     */
    private val descriptionAnnotationNames: Set<String> = Config.descriptionAnnotationNames

    /**
     * Set of lowercase annotation parameter names that may contain description text.
     *
     * @see Config.descriptionValueAttributes
     */
    private val descriptionValueAttributes: Set<String> = Config.descriptionValueAttributes

    /**
     * Extracts the description text from an annotation if it matches a recognized description annotation.
     *
     * This method performs case-insensitive matching of the annotation's simple name against
     * [descriptionAnnotationNames]. If matched, it searches the annotation's arguments for any
     * parameter names that match [descriptionValueAttributes] and returns the first non-null
     * String value found.
     *
     * ## Recognition Logic
     *
     * 1. The annotation is matched by **simple name only** (not fully qualified name)
     * 2. Matching is **case-insensitive** for both annotation names and parameter names
     * 3. The first matching parameter with a non-null String value is returned
     *
     * ## Example Usage
     *
     * ```kotlin
     * // With @Description annotation
     * @Description("A purchasable product with pricing and inventory info.")
     * class Product
     *
     * val description = Introspections.getDescriptionFromAnnotation(
     *     annotationName = "Description",
     *     annotationArguments = listOf("value" to "A purchasable product with pricing and inventory info.")
     * )
     * // description == "A purchasable product with pricing and inventory info."
     * ```
     *
     * ```kotlin
     * // With Jackson annotation
     * @JsonPropertyDescription(description = "User's email address")
     * val email: String
     *
     * val description = Introspections.getDescriptionFromAnnotation(
     *     annotationName = "JsonPropertyDescription",
     *     annotationArguments = listOf("description" to "User's email address")
     * )
     * // description == "User's email address"
     * ```
     *
     * @param annotationName The simple name of the annotation to inspect (e.g., "Description", "P")
     * @param annotationArguments List of key-value pairs representing the annotation's parameters
     * @return The description text if found, or null if the annotation is not recognized or
     *         contains no matching description parameter
     */
    @JvmStatic
    public fun getDescriptionFromAnnotation(
        annotationName: String,
        annotationArguments: List<Pair<String, Any?>>,
    ): String? =
        if (annotationName.lowercase() in descriptionAnnotationNames) {
            annotationArguments
                .filter { it.first.lowercase() in descriptionValueAttributes }
                .firstNotNullOfOrNull {
                    val value = it.second
                    return (value as? String)
                }
        } else {
            null
        }
}
