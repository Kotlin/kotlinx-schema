package kotlinx.schema.generator.core.ir

/**
 * Utility object for annotation-based introspection, providing methods to process annotations,
 * especially those related to descriptions.
 */
public object Introspections {
    /**
     * A set of lowercase annotation names used for describing metadata or properties.
     * These annotations may be used for documentation or schema generation purposes.
     */
    private val DESCRIPTION_ANNOTATIONS =
        setOf(
            "Description".lowercase(),
            "LLMDescription".lowercase(), // Koog
            "JsonPropertyDescription".lowercase(), // Jackson
            "JsonClassDescription".lowercase(), // Jackson (for classes)
            "P".lowercase(), // LangChain4j @P annotation
        )

    /**
     * A predefined set of annotation argument keys used to extract descriptions or values.
     *
     * The set contains the following keys:
     * - "value": Typically used to identify main or default values in annotations.
     * - "description": Commonly used to provide textual descriptions within annotations.
     *
     * This set is used in inspection logic to filter and extract relevant information
     * from annotation arguments based on the presence of these keys.
     */
    private val DESCRIPTION_VALUE_ATTRIBUTES = setOf("value", "description")

    /**
     * Extracts the description value from this annotation if it matches the predefined description annotations.
     *
     * The method checks if the annotation type's name matches any name in the `DESCRIPTION_ANNOTATIONS` set.
     * If a match is found, it returns the first non-null description value from the annotation's arguments
     * that matches the predefined `DESCRIPTION_VALUE_ATTRIBUTES`.
     *
     * Example:
     * ```kotlin
     * @Description("A purchasable product with pricing and inventory info.")
     * class Product
     *
     * // Inside a symbol processor:
     * val annotation: KSAnnotation = /* obtained from Product declaration */
     * val description = annotation.descriptionOrNull()
     * // description == "A purchasable product with pricing and inventory info."
     * ```
     * @param annotationName The name of the annotation to inspect for description values.
     * @param annotationArguments A list of key-value pairs representing the arguments of the annotation.
     * @return The first description value found from the annotation's arguments or null if no matching value is found.
     */
    @JvmStatic
    public fun getDescriptionFromAnnotation(
        annotationName: String,
        annotationArguments: List<Pair<String, Any?>>,
    ): String? =
        if (annotationName.lowercase() in DESCRIPTION_ANNOTATIONS) {
            annotationArguments
                .filter { it.first.lowercase() in DESCRIPTION_VALUE_ATTRIBUTES }
                .firstNotNullOfOrNull {
                    val value = it.second
                    return (value as? String)
                }
        } else {
            null
        }
}
