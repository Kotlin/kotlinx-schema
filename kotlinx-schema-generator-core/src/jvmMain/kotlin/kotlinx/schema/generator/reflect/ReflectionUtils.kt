package kotlinx.schema.generator.reflect

import kotlinx.schema.generator.core.ir.Introspections
import kotlinx.schema.generator.core.ir.TypeRef
import kotlin.reflect.KClass
import kotlin.reflect.KProperty
import kotlin.reflect.KProperty1
import kotlin.reflect.KType

/**
 * Gets the [KType.classifier], ensuring it is a [KClass].
 */
internal val KType.klass: KClass<*> get() {
    return requireNotNull(classifier as? KClass<*>) {
        "Unsupported classifier: $classifier. " +
            "Only KClass classifiers are supported. Type parameters and other classifiers " +
            "cannot be introspected using reflection."
    }
}

/**
 * Generates a qualified type name for a class, optionally prefixed with parent name.
 * Used for sealed class subclasses to avoid name collisions (e.g., "Parent.Child").
 *
 * @param klass The class to generate a name for
 * @param parentPrefix Optional parent prefix (typically the sealed parent's simple name)
 * @return Qualified name like "Parent.Child" if parentPrefix provided, otherwise simple name
 */
internal fun generateQualifiedName(
    klass: KClass<*>,
    parentPrefix: String?,
): String {
    val simpleName = klass.simpleName ?: "Unknown"
    return if (parentPrefix != null) {
        "$parentPrefix.$simpleName"
    } else {
        simpleName
    }
}

/**
 * Finds a property in a class by name.
 * Used when extracting metadata from constructor parameters to find corresponding property annotations.
 *
 * @param klass The class to search in
 * @param propertyName The name of the property to find
 * @return The property if found, null otherwise
 */
internal fun findPropertyByName(
    klass: KClass<*>,
    propertyName: String,
): KProperty<*>? =
    klass.members
        .filterIsInstance<KProperty<*>>()
        .firstOrNull { it.name == propertyName }

/**
 * Extracts description from annotations.
 *
 * @see [Introspections.getDescriptionFromAnnotation]
 */
internal fun extractDescription(annotations: List<Annotation>): String? =
    annotations.firstNotNullOfOrNull { annotation ->
        val annotationName = annotation.annotationClass.simpleName ?: return@firstNotNullOfOrNull null
        val annotationArguments =
            buildList {
                annotation.annotationClass.members
                    .filterIsInstance<KProperty1<Annotation, *>>()
                    .forEach { property ->
                        add(property.name to property.get(annotation))
                    }
            }
        Introspections.getDescriptionFromAnnotation(annotationName, annotationArguments)
    }

/**
 * Returns a new [TypeRef] with the specified nullable flag.
 */
internal fun TypeRef.withNullable(nullable: Boolean): TypeRef =
    when (this) {
        is TypeRef.Inline -> copy(nullable = nullable)
        is TypeRef.Ref -> copy(nullable = nullable)
    }
