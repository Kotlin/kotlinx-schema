package kotlinx.schema.generator.core.ir

import kotlinx.schema.generator.core.Config
import kotlinx.schema.generator.core.InternalSchemaGeneratorApi
import kotlinx.schema.generator.core.ir.Introspections.getDescriptionFromAnnotation
import kotlinx.schema.generator.core.ir.Introspections.getNameOverride
import kotlinx.schema.generator.core.ir.Introspections.isIgnoreAnnotation
import kotlin.jvm.JvmStatic

/**
 * Utility object for annotation-based introspection, providing methods to process annotations
 * for descriptions, ignore markers, and name overrides.
 *
 * This object provides a configurable mechanism for recognizing annotations from
 * multiple frameworks (kotlinx-schema, Jackson, LangChain4j, Koog, kotlinx.serialization, etc.)
 * Configuration is loaded from `kotlinx-schema.properties` on the classpath.
 *
 * ## Annotation name matching
 *
 * Annotation names are matched in two ways depending on the configured name format:
 *
 * - **Simple names** (no dots): Matched **case-insensitively** against the annotation's simple name.
 *   Example: `"Description"` matches `@Description`, `@description`, `@DESCRIPTION`.
 * - **Fully qualified names** (contains dots): Matched **case-sensitively** against the annotation's
 *   qualified name. Example: `"kotlinx.serialization.SerialName"` matches only
 *   `@kotlinx.serialization.SerialName`, not a different `@SerialName` from another package.
 *
 * ## Configuration
 *
 * The annotation detection behavior is controlled by properties in `kotlinx-schema.properties`:
 *
 * - `introspector.annotations.description.names`: Comma-separated list of annotation names
 *   to recognize as description providers (e.g., "Description,LLMDescription,P")
 * - `introspector.annotations.description.attributes`: Comma-separated list of annotation parameter
 *   names that contain description text (e.g., "value,description")
 * - `introspector.annotations.ignore.names`: Comma-separated list of annotation names
 *   to recognize as ignore markers (e.g., "SchemaIgnore,JsonIgnoreType")
 * - `introspector.annotations.name.names`: Comma-separated list of annotation names
 *   to recognize as name-override providers (e.g., "kotlinx.serialization.SerialName")
 * - `introspector.annotations.name.attributes`: Comma-separated list of annotation parameter
 *   names that contain name-override text (e.g., "value")
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
 * @see isIgnoreAnnotation
 * @see getNameOverride
 * @see Config
 */
@InternalSchemaGeneratorApi
public object Introspections {
    //region Annotation name sets

    /**
     * Holds pre-split simple and FQN name sets for an annotation category.
     *
     * Simple names are stored lowercase for case-insensitive matching.
     * FQN names are stored in original case for case-sensitive matching.
     */
    private data class AnnotationNameSets(
        val simpleNames: Set<String>,
        val fqnNames: Set<String>,
    )

    /**
     * Splits a list of annotation names into simple names (lowercase) and FQN names (exact case).
     * Names containing a dot are treated as fully qualified names.
     */
    private fun splitByFqn(names: List<String>): AnnotationNameSets {
        val simple = mutableSetOf<String>()
        val fqn = mutableSetOf<String>()
        for (name in names) {
            if ('.' in name) fqn.add(name) else simple.add(name.lowercase())
        }
        return AnnotationNameSets(simple, fqn)
    }

    /**
     * Checks whether an annotation matches a set of recognized names.
     *
     * Simple names are matched case-insensitively against [simpleName].
     * FQN names are matched case-sensitively against [qualifiedName].
     */
    private fun matchesAnnotation(
        simpleName: String,
        qualifiedName: String?,
        nameSets: AnnotationNameSets,
    ): Boolean =
        simpleName.lowercase() in nameSets.simpleNames ||
            (qualifiedName != null && qualifiedName in nameSets.fqnNames)

    //endregion

    //region Description annotation config

    private val descriptionNames: AnnotationNameSets by lazy {
        splitByFqn(Config.descriptionAnnotationNames)
    }

    /**
     * Ordered list of lowercase annotation parameter names that may contain description text.
     * Order determines priority — earlier entries take precedence.
     *
     * @see Config.descriptionValueAttributes
     */
    private val descriptionValueAttributes: List<String> = Config.descriptionValueAttributes

    //endregion

    //region Ignore annotation config

    private val ignoreNames: AnnotationNameSets by lazy {
        splitByFqn(Config.ignoreAnnotationNames)
    }

    //endregion

    //region Name-override annotation config

    private val nameNames: AnnotationNameSets by lazy {
        splitByFqn(Config.nameAnnotationNames)
    }

    /**
     * Ordered list of lowercase annotation parameter names that may contain name-override text.
     * Order determines priority — earlier entries take precedence.
     *
     * @see Config.nameValueAttributes
     */
    private val nameValueAttributes: List<String> = Config.nameValueAttributes

    //endregion

    /**
     * Extracts the description text from an annotation if it matches a recognized description annotation.
     *
     * Simple annotation names are matched **case-insensitively**; fully qualified names are matched
     * **case-sensitively** (exact match).
     *
     * @param simpleName The simple name of the annotation (e.g., "Description")
     * @param qualifiedName The fully qualified name of the annotation (e.g., "kotlinx.schema.Description"),
     *   or null if unavailable
     * @param annotationArguments List of key-value pairs representing the annotation's parameters
     * @return The description text if found, or null if the annotation is not recognized or
     *         contains no matching description parameter
     */
    @JvmStatic
    public fun getDescriptionFromAnnotation(
        simpleName: String,
        qualifiedName: String?,
        annotationArguments: List<Pair<String, Any?>>,
    ): String? =
        if (matchesAnnotation(simpleName, qualifiedName, descriptionNames)) {
            extractFirstStringAttribute(annotationArguments, descriptionValueAttributes)
        } else {
            null
        }

    /**
     * Checks whether the given annotation is recognized as an ignore marker.
     *
     * Simple annotation names are matched **case-insensitively**; fully qualified names are matched
     * **case-sensitively** (exact match).
     *
     * @param simpleName The simple name of the annotation (e.g., "SchemaIgnore")
     * @param qualifiedName The fully qualified name of the annotation, or null if unavailable
     * @return `true` if the annotation is recognized as an ignore marker
     */
    @JvmStatic
    public fun isIgnoreAnnotation(
        simpleName: String,
        qualifiedName: String? = null,
    ): Boolean = matchesAnnotation(simpleName, qualifiedName, ignoreNames)

    /**
     * Extracts the name-override value from an annotation if it matches a recognized
     * name-override annotation (e.g., `@SerialName`).
     *
     * Simple annotation names are matched **case-insensitively**; fully qualified names are matched
     * **case-sensitively** (exact match).
     *
     * @param simpleName The simple name of the annotation (e.g., "SerialName")
     * @param qualifiedName The fully qualified name of the annotation
     *   (e.g., "kotlinx.serialization.SerialName"), or null if unavailable
     * @param annotationArguments List of key-value pairs representing the annotation's parameters
     * @return The override name if found, or null if the annotation is not recognized or
     *         contains no matching name parameter
     */
    @JvmStatic
    public fun getNameOverride(
        simpleName: String,
        qualifiedName: String?,
        annotationArguments: List<Pair<String, Any?>>,
    ): String? =
        if (matchesAnnotation(simpleName, qualifiedName, nameNames)) {
            extractFirstStringAttribute(annotationArguments, nameValueAttributes)
        } else {
            null
        }

    /**
     * Extracts the first non-empty string value from annotation arguments whose key matches
     * the given attribute names.
     *
     * Iterates [attributeNames] in order, so the first attribute name in the list has the
     * highest priority. For each attribute name, finds the matching annotation argument and
     * returns its value if non-empty.
     */
    private fun extractFirstStringAttribute(
        annotationArguments: List<Pair<String, Any?>>,
        attributeNames: List<String>,
    ): String? {
        val argsByName = annotationArguments.associateBy({ it.first.lowercase() }, { it.second })
        return attributeNames.firstNotNullOfOrNull { attrName ->
            (argsByName[attrName] as? String)?.takeIf { it.isNotEmpty() }
        }
    }
}

/**
 * Functional interface describing a strategy for extracting a property/type description from a list of annotations
 * associated with it.
 * It's used to allow custom description annotations.
 */
public fun interface DescriptionExtractor {
    /**
     * Extracts a description from a list of annotations.
     *
     * @param annotations List of annotations to inspect for a description
     * @return The description text if found, or null if no description is present
     */
    public fun extract(annotations: List<Annotation>): String?
}
