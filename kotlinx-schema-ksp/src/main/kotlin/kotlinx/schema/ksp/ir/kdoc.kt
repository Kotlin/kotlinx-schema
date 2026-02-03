package kotlinx.schema.ksp.ir

import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration

internal fun KSClassDeclaration.descriptionFromKdoc(): String? = extractDescriptionFromKdoc(this.docString)

internal fun KSPropertyDeclaration.descriptionFromKdoc(): String? = extractDescriptionFromKdoc(this.docString)

internal fun KSFunctionDeclaration.descriptionFromKdoc(): String? = extractDescriptionFromKdoc(this.docString)

internal fun extractDescriptionFromKdoc(kdoc: String?): String? =
    kdoc
        ?.lineSequence()
        ?.map { it.trim() }
        // Stop collecting description once a KDoc tag is encountered
        ?.takeWhile { !it.startsWith("@") }
        // Drop empty lines from the description
        ?.filter { it.isNotBlank() }
        ?.joinToString("\n")
        // Normalize empty result to null
        ?.ifBlank { null }

/**
 * Extracts the description for a specific parameter from KDoc.
 *
 * Searches for `@param <paramName>` tag and returns its description.
 * Supports multi-line descriptions that continue until the next tag or end of KDoc.
 *
 * @param kdoc The KDoc string to parse
 * @param paramName The name of the parameter to find
 * @return The parameter description or null if not found
 */
internal fun extractParamDescriptionFromKdoc(
    kdoc: String?,
    paramName: String,
): String? = extractTagDescriptionFromKdoc(kdoc, "param", paramName)

/**
 * Extracts the description for a specific property from KDoc.
 *
 * Searches for `@property <propertyName>` tag and returns its description.
 * Supports multi-line descriptions that continue until the next tag or end of KDoc.
 *
 * @param kdoc The KDoc string to parse
 * @param propertyName The name of the property to find
 * @return The property description or null if not found
 */
internal fun extractPropertyDescriptionFromKdoc(
    kdoc: String?,
    propertyName: String,
): String? = extractTagDescriptionFromKdoc(kdoc, "property", propertyName)

/**
 * Generic function to extract description for a specific KDoc tag.
 *
 * @param kdoc The KDoc string to parse
 * @param tagName The tag name (e.g., "param", "property")
 * @param targetName The name of the parameter/property to find
 * @return The tag description or null if not found
 */
internal fun extractTagDescriptionFromKdoc(
    kdoc: String?,
    tagName: String,
    targetName: String,
): String? =
    kdoc
        ?.lineSequence()
        ?.map { it.trim() }
        ?.toList()
        ?.let { lines ->
            val tagPrefix = "@$tagName"

            // Find the line with the target tag
            val targetIndex =
                lines.indexOfFirst { line ->
                    if (!line.startsWith(tagPrefix)) return@indexOfFirst false
                    val tagContent = line.removePrefix(tagPrefix).trim()
                    val paramName = tagContent.takeWhile { !it.isWhitespace() }
                    paramName == targetName
                }

            if (targetIndex == -1) return@let null

            // Extract description from the tag line and continuation lines
            val firstLine = lines[targetIndex]
            val tagContent = firstLine.removePrefix(tagPrefix).trim()
            val paramName = tagContent.takeWhile { !it.isWhitespace() }
            val descriptionStart = tagContent.drop(paramName.length).trim()

            buildString {
                if (descriptionStart.isNotBlank()) append(descriptionStart)

                // Collect continuation lines until the next tag
                lines
                    .asSequence()
                    .drop(targetIndex + 1)
                    .takeWhile { !it.startsWith("@") }
                    .filter { it.isNotBlank() }
                    .forEach { line ->
                        if (isNotEmpty()) append('\n')
                        append(line)
                    }
            }.ifBlank { null }
        }
