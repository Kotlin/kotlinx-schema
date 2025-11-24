package kotlinx.schema.generator.reflect

import kotlinx.schema.generator.core.ir.EnumNode
import kotlinx.schema.generator.core.ir.Introspections
import kotlinx.schema.generator.core.ir.PrimitiveKind
import kotlinx.schema.generator.core.ir.TypeId
import kotlinx.schema.generator.core.ir.TypeRef
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1

/*
 * Utility functions for reflection-based introspection.
 *
 * These functions are shared between [ReflectionIntrospector] and [ReflectionFunctionIntrospector]
 * to avoid code duplication.
 */

/**
 * Creates a [TypeId] from a [KClass], using qualified name or simple name as fallback.
 */
internal fun createTypeId(klass: KClass<*>): TypeId = TypeId(klass.qualifiedName ?: klass.simpleName ?: "Anonymous")

/**
 * Checks if a class is list-like (List, Collection, or Iterable).
 */
internal fun isListLike(klass: KClass<*>): Boolean =
    klass == List::class || klass == Collection::class || klass == Iterable::class

/**
 * Checks if a class is an enum class.
 */
internal fun isEnumClass(klass: KClass<*>): Boolean = klass.isData == false && klass.java.isEnum

/**
 * Maps a Kotlin primitive class to its corresponding [PrimitiveKind].
 * Returns null if the class is not a supported primitive type.
 */
internal fun primitiveKindFor(klass: KClass<*>): PrimitiveKind? =
    when (klass) {
        String::class -> PrimitiveKind.STRING
        Boolean::class -> PrimitiveKind.BOOLEAN
        Byte::class, Short::class, Int::class -> PrimitiveKind.INT
        Long::class -> PrimitiveKind.LONG
        Float::class -> PrimitiveKind.FLOAT
        Double::class -> PrimitiveKind.DOUBLE
        Char::class -> PrimitiveKind.STRING
        else -> null
    }

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
                runCatching {
                    annotation.annotationClass.members
                        .filterIsInstance<KProperty1<Annotation, *>>()
                        .forEach { property ->
                            runCatching { add(property.name to property.get(annotation)) }
                        }
                }
            }
        Introspections.getDescriptionFromAnnotation(annotationName, annotationArguments)
    }

/**
 * Creates an [EnumNode] from an enum class.
 */
internal fun createEnumNode(klass: KClass<*>): EnumNode {
    @Suppress("UNCHECKED_CAST")
    val enumConstants = (klass.java as Class<out Enum<*>>).enumConstants
    return EnumNode(
        name = klass.simpleName ?: "UnknownEnum",
        entries = enumConstants.map { it.name },
        description = extractDescription(klass.annotations),
    )
}

/**
 * Returns a new [TypeRef] with the specified nullable flag.
 */
internal fun TypeRef.withNullable(nullable: Boolean): TypeRef =
    when (this) {
        is TypeRef.Inline -> copy(nullable = nullable)
        is TypeRef.Ref -> copy(nullable = nullable)
    }
