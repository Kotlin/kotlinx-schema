package kotlinx.schema.generator.json.internal

import kotlinx.schema.Description
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject

/**
 * Holds intermediate and stateful information about the current schema generation process.
 *
 * @property json [kotlinx.serialization.json.Json] instance used for serialization,
 * provides required meta info needed for schema generation that
 * is not available in [kotlinx.serialization.descriptors.SerialDescriptor].
 * @property descriptor The [kotlinx.serialization.descriptors.SerialDescriptor] currently being processed.
 * @property processedTypeDefs A mutable map of [kotlinx.serialization.descriptors.SerialDescriptor]
 * to [kotlinx.serialization.json.JsonObject], maintaining schema fragments for previously processed types
 * to avoid redundant schema generation.
 * @property currentDefPath A list of [kotlinx.serialization.descriptors.SerialDescriptor] representing
 * the current path in the descriptor hierarchy during schema generation processing.
 * @property descriptionOverrides A map of user-defined properties and types descriptions to override Description
 * from the provided class.
 * @property excludedProperties A set of property names to exclude from the schema generation.
 * @property currentDescription Description for the current element
 */
public data class GenerationContext(
    public val json: Json,
    public val descriptor: SerialDescriptor,
    public val processedTypeDefs: MutableMap<SerialDescriptor, JsonObject>,
    public val currentDefPath: List<SerialDescriptor>,
    public val descriptionOverrides: Map<String, String>,
    public val excludedProperties: Set<String>,
    public val currentDescription: String?,
) {
    /**
     * Helper method that gets description for [descriptor] type from [descriptionOverrides] map
     * or [Description] annotation.
     *
     * @return Description or `null` if no description is specified.
     */
    public fun getTypeDescription(): String? {
        val typeDescriptionOverride = descriptionOverrides[descriptor.serialName]
        val typeDescriptionAnnotation =
            descriptor.annotations
                .filterIsInstance<Description>()
                .firstOrNull()
                ?.value

        return typeDescriptionOverride ?: typeDescriptionAnnotation
    }

    /**
     * Helper method that gets description for an element in [descriptor] from [descriptionOverrides] map
     * or [Description] annotation.
     *
     * @param index Index of the element in [descriptor]
     *
     * @return Description or `null` if no description is specified.
     */
    public fun getElementDescription(index: Int): String? {
        val elementName = descriptor.getElementName(index)
        val elementAnnotations = descriptor.getElementAnnotations(index)

        val lookupKey = "${descriptor.serialName}.$elementName"
        val elementDescriptionOverride = descriptionOverrides[lookupKey]
        val elementDescriptionAnnotation =
            elementAnnotations
                .filterIsInstance<Description>()
                .firstOrNull()
                ?.value

        return elementDescriptionOverride ?: elementDescriptionAnnotation
    }
}
