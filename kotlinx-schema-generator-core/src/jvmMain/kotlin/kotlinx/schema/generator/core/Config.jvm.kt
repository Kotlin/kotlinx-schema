package kotlinx.schema.generator.core

import java.io.IOException
import java.util.Properties
import kotlin.io.bufferedReader
import kotlin.use

private const val CONFIG_FILE_NAME = "kotlinx-schema.properties"
private const val DESCRIPTION_NAMES_KEY = "introspector.annotations.description.names"
private const val DESCRIPTION_ATTRIBUTES_KEY = "introspector.annotations.description.attributes"
private const val IGNORE_NAMES_KEY = "introspector.annotations.ignore.names"
private const val NAME_NAMES_KEY = "introspector.annotations.name.names"
private const val NAME_ATTRIBUTES_KEY = "introspector.annotations.name.attributes"

/**
 * Default fallback values if configuration loading fails
 */
private val DEFAULT_ANNOTATION_NAMES =
    listOf(
        "description",
        "llmdescription",
        "jsonpropertydescription",
        "jsonclassdescription",
        "p",
    )

/**
 * Default fallback values if configuration loading fails
 */
private val DEFAULT_VALUE_ATTRIBUTES =
    listOf(
        "value",
        "description",
    )

/**
 * Default fallback values if configuration loading fails
 */
private val DEFAULT_IGNORE_NAMES =
    listOf(
        "schemaignore",
        "serialschemaignore",
        "jsonignoretype",
    )

/**
 * Default fallback values if configuration loading fails
 */
private val DEFAULT_NAME_ANNOTATION_NAMES =
    listOf(
        "kotlinx.serialization.SerialName",
    )

/**
 * Default fallback values if configuration loading fails
 */
private val DEFAULT_NAME_VALUE_ATTRIBUTES =
    listOf(
        "value",
    )

internal actual object Config {
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
    actual val descriptionAnnotationNames: List<String> by lazy {
        loadConfiguration { properties ->
            parseListProperty(properties, DESCRIPTION_NAMES_KEY)
        } ?: DEFAULT_ANNOTATION_NAMES
    }

    /**
     * Ordered list of lowercase parameter names to check for description text.
     *
     * When an annotation matches [descriptionAnnotationNames], its parameters are inspected
     * for these attribute names to extract the description value. The first matching parameter
     * with a non-null String value is returned.
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
    actual val descriptionValueAttributes: List<String> by lazy {
        loadConfiguration { properties ->
            parseListProperty(properties, DESCRIPTION_ATTRIBUTES_KEY)
        } ?: DEFAULT_VALUE_ATTRIBUTES
    }

    actual val ignoreAnnotationNames: List<String> by lazy {
        loadConfiguration { properties ->
            parseListProperty(properties, IGNORE_NAMES_KEY)
        } ?: DEFAULT_IGNORE_NAMES
    }

    actual val nameAnnotationNames: List<String> by lazy {
        loadConfiguration { properties ->
            parseListPropertyPreservingFqnCase(properties, NAME_NAMES_KEY)
        } ?: DEFAULT_NAME_ANNOTATION_NAMES
    }

    actual val nameValueAttributes: List<String> by lazy {
        loadConfiguration { properties ->
            parseListProperty(properties, NAME_ATTRIBUTES_KEY)
        } ?: DEFAULT_NAME_VALUE_ATTRIBUTES
    }

    private fun <T> loadConfiguration(extractor: (Properties) -> T): T? =
        try {
            val properties = loadProperties()
            extractor(properties)
        } catch (e: IOException) {
            // Log and return null to use fallback
            System.err.println("Warning: Failed to load configuration from $CONFIG_FILE_NAME: ${e.message}")
            System.err.println("Using default configuration values")
            null
        }

    private fun parseListProperty(
        properties: Properties,
        key: String,
    ): List<String> {
        val value = properties.getProperty(key)
        require(!value.isNullOrBlank()) {
            "Required property '$key' is missing or empty in $CONFIG_FILE_NAME"
        }

        return value
            .split(',')
            .map { it.trim().lowercase() }
            .filter { it.isNotEmpty() }
            .distinct()
            .also { list ->
                require(list.isNotEmpty()) {
                    "Property '$key' in $CONFIG_FILE_NAME resulted in empty list after parsing"
                }
            }
    }

    /**
     * Parses a comma-separated list property, preserving original case for FQN entries
     * (names containing dots) and lowercasing simple names.
     */
    private fun parseListPropertyPreservingFqnCase(
        properties: Properties,
        key: String,
    ): List<String> {
        val value = properties.getProperty(key)
        require(!value.isNullOrBlank()) {
            "Required property '$key' is missing or empty in $CONFIG_FILE_NAME"
        }

        return value
            .split(',')
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .map { name -> if ('.' in name) name else name.lowercase() }
            .distinct()
            .also { list ->
                require(list.isNotEmpty()) {
                    "Property '$key' in $CONFIG_FILE_NAME resulted in empty list after parsing"
                }
            }
    }

    private fun loadProperties(): Properties {
        val classLoader = Config.javaClass.classLoader
        val stream =
            classLoader.getResourceAsStream(CONFIG_FILE_NAME)
                ?: error(
                    "Configuration file '$CONFIG_FILE_NAME' not found on classpath. " +
                        "Searched using classloader: ${classLoader.javaClass.name}. " +
                        "Ensure the file exists in your resources directory.",
                )

        return try {
            stream.bufferedReader(Charsets.UTF_8).use { reader ->
                Properties().apply { load(reader) }
            }
        } catch (e: IOException) {
            throw IllegalStateException(
                "Failed to parse configuration file '$CONFIG_FILE_NAME': ${e.message}",
                e,
            )
        }
    }
}
