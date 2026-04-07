package kotlinx.schema.generator.core

/**
 * Configuration for schema generation.
 *
 * This object encapsulates the configuration that controls which annotations are recognized
 * as description providers and which annotation parameters contain description text.
 *
 * Configuration is loaded lazily from `kotlinx-schema.properties` on the classpath.
 * If loading fails, the system falls back to built-in default values and continues to operate.
 *
 * ## Configuration Properties
 *
 * - `introspector.annotations.description.names`: Comma-separated list of annotation simple names
 *   to recognize as description providers
 * - `introspector.annotations.description.attributes`: Comma-separated list of annotation parameter
 *   names that contain description text
 *
 * ## Fallback Behavior
 *
 * If configuration loading fails (file not found or I/O error), the system automatically uses
 * default values: Description, LLMDescription, JsonPropertyDescription, JsonClassDescription, P
 * for annotation names, and "value", "description" for attributes.
 */
internal expect object Config {
    /**
     * Ordered list of lowercase annotation simple names recognized as description providers.
     *
     * Annotations are matched case-insensitively by their simple name only (not fully qualified name).
     * This allows recognition of description annotations from multiple frameworks (kotlinx-schema,
     * Jackson, LangChain4j, Koog, etc.) without requiring specific imports.
     *
     * Loaded lazily from the `introspector.annotations.description.names` property in
     * `kotlinx-schema.properties`. If loading fails, falls back to built-in defaults.
     *
     * Default value: Description, LLMDescription, JsonPropertyDescription, JsonClassDescription, P
     */
    val descriptionAnnotationNames: List<String>

    /**
     * Ordered list of lowercase parameter names to check for description text.
     *
     * When an annotation matches [descriptionAnnotationNames], its parameters are inspected
     * for these attribute names to extract the description value. The first matching parameter
     * with a non-null String value is returned. Order determines priority — earlier entries
     * take precedence.
     *
     * Loaded lazily from the `introspector.annotations.description.attributes` property in
     * `kotlinx-schema.properties`. If loading fails, falls back to built-in defaults.
     *
     * Default value: "value", "description"
     *
     * ## Examples
     * - For `@Description("User name")`, the "value" parameter contains "User name"
     * - For `@JsonPropertyDescription(description = "User email")`, the "description" parameter contains "User email"
     */
    val descriptionValueAttributes: List<String>

    /**
     * Ordered list of lowercase annotation simple names recognized as ignore markers.
     *
     * Classes annotated with any of these annotations are excluded from schema generation
     * (e.g., sealed subtypes omitted from polymorphic `oneOf` schemas).
     *
     * Loaded lazily from the `introspector.annotations.ignore.names` property in
     * `kotlinx-schema.properties`. If loading fails, falls back to built-in defaults.
     *
     * Default value: SchemaIgnore, SerialSchemaIgnore, JsonIgnoreType
     */
    val ignoreAnnotationNames: List<String>

    /**
     * Ordered list of annotation names recognized as name-override providers (e.g., `@SerialName`).
     *
     * Names containing a dot (`.`) are treated as fully qualified names and matched
     * **case-sensitively** against the annotation's qualified name. Names without a dot
     * are matched **case-insensitively** against the annotation's simple name.
     *
     * Loaded lazily from the `introspector.annotations.name.names` property in
     * `kotlinx-schema.properties`. If loading fails, falls back to built-in defaults.
     *
     * Default value: kotlinx.serialization.SerialName
     */
    val nameAnnotationNames: List<String>

    /**
     * Ordered list of lowercase parameter names to check for name-override text.
     *
     * When an annotation matches [nameAnnotationNames], its parameters are inspected
     * for these attribute names to extract the override name value. Order determines
     * priority — earlier entries take precedence.
     *
     * Loaded lazily from the `introspector.annotations.name.attributes` property in
     * `kotlinx-schema.properties`. If loading fails, falls back to built-in defaults.
     *
     * Default value: "value"
     */
    val nameValueAttributes: List<String>
}
