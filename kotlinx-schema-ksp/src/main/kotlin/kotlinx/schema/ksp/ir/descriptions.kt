package kotlinx.schema.ksp.ir

import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSAnnotation
import kotlinx.schema.generator.core.ir.Introspections

/**
 * Retrieves the description value from the annotation, if available.
 *
 * The method resolves the annotation type, collects its arguments, and attempts to extract
 * the description by delegating to the `getDescriptionFromAnnotation` function.
 * If no description is present, returns null.
 *
 * @return The description extracted from the annotation or null if no description is found.
 */
internal fun KSAnnotation.descriptionOrNull(): String? {
    val annotationName =
        annotationType
            .resolve()
            .declaration.simpleName
            .asString()

    val args: List<Pair<String, Any?>> =
        arguments
            .filter { it.name?.asString() != null }
            .map { it.name?.asString()!! to it.value }

    return Introspections.getDescriptionFromAnnotation(
        annotationName = annotationName,
        annotationArguments = args,
    )
}

internal fun KSAnnotated.descriptionOrNull(): String? = annotations.mapNotNull { it.descriptionOrNull() }.firstOrNull()
