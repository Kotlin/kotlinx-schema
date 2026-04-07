package kotlinx.schema.ksp.ir

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
    val declaration = annotationType.resolve().declaration
    val simpleName = declaration.simpleName.asString()
    val qualifiedName = declaration.qualifiedName?.asString()

    val args: List<Pair<String, Any?>> =
        arguments.mapNotNull {
            val name = it.name?.asString() ?: return@mapNotNull null
            name to it.value
        }

    return Introspections.getDescriptionFromAnnotation(
        simpleName = simpleName,
        qualifiedName = qualifiedName,
        annotationArguments = args,
    )
}

/**
 * Retrieves the name-override value from the annotation, if available.
 *
 * Used to extract custom names from annotations like `@SerialName`.
 *
 * @return The name override extracted from the annotation or null if not a name-override annotation.
 */
internal fun KSAnnotation.nameOverrideOrNull(): String? {
    val declaration = annotationType.resolve().declaration
    val simpleName = declaration.simpleName.asString()
    val qualifiedName = declaration.qualifiedName?.asString()

    val args: List<Pair<String, Any?>> =
        arguments.mapNotNull {
            val name = it.name?.asString() ?: return@mapNotNull null
            name to it.value
        }

    return Introspections.getNameOverride(
        simpleName = simpleName,
        qualifiedName = qualifiedName,
        annotationArguments = args,
    )
}
