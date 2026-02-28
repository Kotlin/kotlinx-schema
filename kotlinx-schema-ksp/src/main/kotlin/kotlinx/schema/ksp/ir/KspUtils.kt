package kotlinx.schema.ksp.ir

import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSValueParameter
import kotlinx.schema.generator.core.ir.Property
import kotlinx.schema.generator.core.ir.TypeRef

/**
 * Extracts description from annotations with KDoc fallback.
 *
 * Unifies the description extraction pattern used across introspectors:
 * 1. Try annotations first (via descriptionOrNull())
 * 2. Fall back to KDoc if available
 *
 * @param annotated The annotated element to extract description from
 * @param kdocFallback Lazy KDoc description provider (only evaluated if annotations don't provide description)
 * @return Description string or null if not found
 */
internal fun extractDescription(
    annotated: KSAnnotated,
    kdocFallback: () -> String?,
): String? =
    annotated.annotations.firstNotNullOfOrNull { it.descriptionOrNull() }
        ?: kdocFallback()

/**
 * Extracts description for a property or parameter with multiple fallback sources.
 *
 * This function supports the following description resolution chain:
 * 1. Annotations on the property/parameter (e.g., @Description)
 * 2. Property's own KDoc (via elementKdocFallback)
 * 3. Parent's KDoc tag (@param or @property) for the property/parameter name
 *
 * @param annotated The annotated property/parameter element
 * @param propertyName The name of the property/parameter (used for parent KDoc lookup)
 * @param parentKdoc The parent's KDoc string (class KDoc for @property or function KDoc for @param)
 * @param kdocTagName The tag name to search in parent KDoc ("param" or "property")
 * @param elementKdocFallback Lazy KDoc description provider for the element itself
 * @return Description string or null if not found in any source
 */
internal fun extractPropertyDescription(
    annotated: KSAnnotated,
    propertyName: String,
    parentKdoc: String?,
    kdocTagName: String,
    elementKdocFallback: () -> String?,
): String? =
    // 1. Try annotations first
    annotated.annotations.firstNotNullOfOrNull { it.descriptionOrNull() }
        // 2. Fall back to property's own KDoc
        ?: elementKdocFallback()
        // 3. Finally, try parent KDoc tag
        ?: extractTagDescriptionFromKdoc(parentKdoc, kdocTagName, propertyName)

/**
 * Extracts description for a constructor parameter with class KDoc fallback.
 *
 * Constructor parameters can be documented using either @param or @property tags
 * in the class KDoc. This function tries both with @param taking precedence.
 *
 * Resolution chain:
 * 1. Annotations on the parameter (e.g., @Description)
 * 2. Class KDoc @param tag
 * 3. Class KDoc @property tag
 *
 * @param param The constructor parameter
 * @param paramName The parameter name
 * @param classKdoc The class KDoc string
 * @return Description string or null if not found in any source
 */
internal fun extractConstructorParamDescription(
    param: KSValueParameter,
    paramName: String,
    classKdoc: String?,
): String? =
    param.annotations.firstNotNullOfOrNull { it.descriptionOrNull() }
        ?: extractTagDescriptionFromKdoc(classKdoc, "param", paramName)
        ?: extractTagDescriptionFromKdoc(classKdoc, "property", paramName)

/**
 * Creates a Property instance with consistent defaults.
 *
 * Shared by KspFunctionIntrospector and KspClassIntrospector to ensure
 * uniform property construction across introspectors.
 *
 * @param name Property name
 * @param type Property type reference
 * @param description Property description (from annotations or KDoc)
 * @param hasDefaultValue Whether the property has a default value
 * @return Property instance with defaultValue set to null (KSP limitation)
 */
internal fun createProperty(
    name: String,
    type: TypeRef,
    description: String?,
    hasDefaultValue: Boolean,
): Property =
    Property(
        name = name,
        type = type,
        description = description,
        hasDefaultValue = hasDefaultValue,
        defaultValue = null, // KSP cannot extract default values at compile-time
    )
