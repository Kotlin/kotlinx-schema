package kotlinx.schema.ksp.ir

import com.google.devtools.ksp.symbol.KSAnnotated
import kotlinx.schema.generator.core.ir.Introspections

/**
 * Checks whether the symbol is annotated with a recognized ignore annotation
 * (e.g., `@SchemaIgnore`, `@SerialSchemaIgnore`, `@JsonIgnoreType`).
 *
 * Recognition is delegated to [Introspections.isIgnoreAnnotation], which performs
 * case-insensitive matching by simple name against a configurable set loaded
 * from `kotlinx-schema.properties`.
 *
 * @return `true` if any annotation on this symbol is recognized as an ignore marker
 */
internal fun KSAnnotated.isSchemaIgnored(): Boolean =
    annotations.any { annotation ->
        val name =
            annotation.annotationType
                .resolve()
                .declaration.simpleName
                .asString()
        Introspections.isIgnoreAnnotation(name)
    }
