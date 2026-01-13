package kotlinx.schema.ksp

import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.KSDeclaration

/**
 * Filters a declaration based on whether it resides within the specified root package.
 * Logs a message if the declaration is skipped for being outside the root package.
 *
 * @param declaration The Kotlin Symbol Processing (KSP) declaration to filter.
 * @param rootPackage The optional root package name used to constrain the filtering.
 *                     If null, no filtering is applied based on the root package.
 * @param logger The KSP logger instance used for logging messages.
 * @return `true` if the declaration is in the specified root package or no root package is specified,
 *         `false` otherwise.
 */
internal fun filterByRootPackage(
    declaration: KSDeclaration,
    rootPackage: String?,
    logger: KSPLogger,
): Boolean {
    if (rootPackage != null) {
        val pkg = declaration.packageName.asString()
        val inRoot = pkg == rootPackage || pkg.startsWith("$rootPackage.")
        if (!inRoot) {
            logger.info(
                "[kotlinx-schema] Skipping ${declaration.qualifiedName?.asString()} " +
                    "as it is outside rootPackage '$rootPackage'",
            )
            return false
        }
    }
    return true
}

/**
 * Retrieves schema parameters from a specified annotation on a declaration,
 * merging them with provided default parameters.
 *
 * @param declaration The declaration from which to retrieve the annotation parameters.
 * @param annotation The fully qualified name of the annotation to look for.
 * @param defaultParameters A map of default parameters to be merged with the annotation parameters.
 * @return A map containing the merged parameters from the specified annotation and the default parameters.
 */
internal fun getSchemaParameters(
    declaration: KSDeclaration,
    annotation: String,
    defaultParameters: Map<String, Any?>,
): Map<String, Any?> {
    val schemaAnnotation =
        declaration.annotations.firstOrNull {
            it.toString() == annotation
        }
    if (schemaAnnotation == null) {
        return mapOf()
    }

    val parameters =
        schemaAnnotation.arguments
            .mapNotNull { arg ->
                arg.name?.getShortName()?.let { it to arg.value }
            }.toMap()
    return defaultParameters.plus(parameters)
}
