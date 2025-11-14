package kotlinx.schema.ksp.ir

import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSAnnotation

private val DESCRIPTION_ANNOTATIONS = setOf("description", "llmdescription")
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
 *
 * @return The description string if found, or null otherwise.
 */
internal fun KSAnnotation.descriptionOrNull(): String? =
    if (annotationType
            .resolve()
            .declaration.simpleName
            .asString()
            .lowercase() in DESCRIPTION_ANNOTATIONS
    ) {
        (
            arguments
                .filter { it.name?.asString()?.lowercase() in DESCRIPTION_VALUE_ATTRIBUTES }
                .firstNotNullOfOrNull { it.value as? String }
        )
    } else {
        null
    }

internal fun KSAnnotated.descriptionOrNull(): String? = annotations.mapNotNull { it.descriptionOrNull() }.firstOrNull()
