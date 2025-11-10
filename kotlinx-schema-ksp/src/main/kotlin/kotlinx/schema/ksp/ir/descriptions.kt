package kotlinx.schema.ksp.ir

import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSAnnotation

internal fun KSAnnotation.descriptionOrNull(): String? =
    if (annotationType
            .resolve()
            .declaration.simpleName
            .asString() == "Description"
    ) {
        (arguments.firstOrNull { it.name?.asString() == "value" }?.value as? String)
    } else {
        null
    }

internal fun KSAnnotated.descriptionOrNull(): String? = annotations.mapNotNull { it.descriptionOrNull() }.firstOrNull()
