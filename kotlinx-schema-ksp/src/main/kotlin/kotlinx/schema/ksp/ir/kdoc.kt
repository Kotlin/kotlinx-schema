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
