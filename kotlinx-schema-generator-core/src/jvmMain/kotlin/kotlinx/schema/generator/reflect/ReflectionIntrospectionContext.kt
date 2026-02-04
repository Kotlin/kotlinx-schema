package kotlinx.schema.generator.reflect

import kotlinx.schema.generator.core.InternalSchemaGeneratorApi
import kotlinx.schema.generator.core.ir.BaseIntrospectionContext
import kotlinx.schema.generator.core.ir.ListNode
import kotlinx.schema.generator.core.ir.MapNode
import kotlinx.schema.generator.core.ir.ObjectNode
import kotlinx.schema.generator.core.ir.PrimitiveKind
import kotlinx.schema.generator.core.ir.PrimitiveNode
import kotlinx.schema.generator.core.ir.Property
import kotlinx.schema.generator.core.ir.TypeId
import kotlinx.schema.generator.core.ir.TypeRef
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.KType
import kotlin.reflect.jvm.javaField

/**
 * Base class for reflection-based introspection contexts.
 *
 * Extends core [BaseIntrospectionContext] with reflection-specific type handling.
 * Provides common functionality for both class and function introspection,
 * including type conversion, caching, and cycle detection.
 *
 * Subclasses must implement [createObjectNode] to define how to extract properties
 * from different source types (classes vs function parameters).
 */
@OptIn(InternalSchemaGeneratorApi::class)
internal abstract class ReflectionIntrospectionContext : BaseIntrospectionContext<KClass<*>, KType>() {
    /**
     * Converts a KType (with type arguments) to a TypeRef.
     * Used for property types where we have full type information.
     */
    protected fun convertKTypeToTypeRef(type: KType): TypeRef {
        val classifier =
            requireNotNull(type.classifier as? KClass<*>) {
                "Unsupported classifier: ${type.classifier}. " +
                    "Only KClass classifiers are supported. Type parameters and other classifiers " +
                    "cannot be introspected using reflection."
            }
        val isNullable = type.isMarkedNullable

        return when {
            isListLike(classifier) -> {
                val elementType = type.arguments.firstOrNull()?.type
                val elementRef =
                    elementType?.let { convertKTypeToTypeRef(it) }
                        ?: TypeRef.Inline(PrimitiveNode(PrimitiveKind.STRING), false)
                TypeRef.Inline(ListNode(elementRef), isNullable)
            }

            Map::class.java.isAssignableFrom(classifier.java) -> {
                val keyType = type.arguments.getOrNull(0)?.type
                val valueType = type.arguments.getOrNull(1)?.type
                val keyRef =
                    keyType?.let { convertKTypeToTypeRef(it) }
                        ?: TypeRef.Inline(PrimitiveNode(PrimitiveKind.STRING), false)
                val valueRef =
                    valueType?.let { convertKTypeToTypeRef(it) }
                        ?: TypeRef.Inline(PrimitiveNode(PrimitiveKind.STRING), false)
                TypeRef.Inline(MapNode(keyRef, valueRef), isNullable)
            }

            else -> {
                convertToTypeRef(classifier, isNullable)
            }
        }
    }

    /**
     * Converts a KClass to a TypeRef, handling caching and nullability.
     * Can be overridden by subclasses to add custom type handling (e.g., sealed classes).
     *
     * Default implementation handles primitives, collections, enums, and objects.
     * Subclasses can override to add sealed class handling or other specialized behavior.
     */
    @Suppress("ReturnCount")
    internal open fun convertToTypeRef(
        klass: KClass<*>,
        nullable: Boolean = false,
        useSimpleName: Boolean = false,
    ): TypeRef {
        // Check cache first
        typeRefCache[klass]?.let { cachedRef ->
            return if (nullable && !cachedRef.nullable) {
                cachedRef.withNullable(true)
            } else {
                cachedRef
            }
        }

        // Try to convert to primitive type
        primitiveKindFor(klass)?.let { primitiveKind ->
            val ref = TypeRef.Inline(PrimitiveNode(primitiveKind), nullable)
            if (!nullable) typeRefCache[klass] = ref
            return ref
        }

        // Handle different type categories
        return when {
            isListLike(klass) -> handleListType(klass, nullable)
            Map::class.java.isAssignableFrom(klass.java) -> handleMapType(klass, nullable)
            isEnumClass(klass) -> handleEnumType(klass, nullable)
            else -> handleObjectType(klass, nullable, useSimpleName)
        }
    }

    /**
     * Handles list-like types (List, Collection, Iterable).
     * Creates a fallback ListNode with String elements when type arguments are unavailable.
     */
    protected fun handleListType(
        klass: KClass<*>,
        nullable: Boolean,
    ): TypeRef {
        val elementRef = TypeRef.Inline(PrimitiveNode(PrimitiveKind.STRING), false)
        val ref = TypeRef.Inline(ListNode(elementRef), nullable)
        if (!nullable) typeRefCache[klass] = ref
        return ref
    }

    /**
     * Handles Map types.
     * Creates a fallback MapNode with String keys and values when type arguments are unavailable.
     */
    protected fun handleMapType(
        klass: KClass<*>,
        nullable: Boolean,
    ): TypeRef {
        val keyRef = TypeRef.Inline(PrimitiveNode(PrimitiveKind.STRING), false)
        val valueRef = TypeRef.Inline(PrimitiveNode(PrimitiveKind.STRING), false)
        val ref = TypeRef.Inline(MapNode(keyRef, valueRef), nullable)
        if (!nullable) typeRefCache[klass] = ref
        return ref
    }

    /**
     * Handles enum types by creating an EnumNode and adding it to discovered nodes.
     */
    protected fun handleEnumType(
        klass: KClass<*>,
        nullable: Boolean,
    ): TypeRef {
        val id = createTypeId(klass)

        withCycleDetection(klass, id) {
            createEnumNode(klass)
        }

        val ref = TypeRef.Ref(id, nullable)
        if (!nullable) typeRefCache[klass] = ref
        return ref
    }

    /**
     * Handles object/class types by creating an ObjectNode.
     * Delegates actual node creation to [createObjectNode] which is implemented by subclasses.
     */
    protected fun handleObjectType(
        klass: KClass<*>,
        nullable: Boolean,
        useSimpleName: Boolean,
        parentPrefix: String? = null,
    ): TypeRef {
        val id =
            when {
                parentPrefix != null -> TypeId(generateQualifiedName(klass, parentPrefix))
                useSimpleName -> TypeId(klass.simpleName ?: "Unknown")
                else -> createTypeId(klass)
            }

        withCycleDetection(klass, id) {
            createObjectNode(klass, parentPrefix)
        }

        val ref = TypeRef.Ref(id, nullable)
        if (!nullable) typeRefCache[klass] = ref
        return ref
    }

    /**
     * Creates an ObjectNode from a KClass.
     * This is the main extension point for subclasses to customize how properties are extracted.
     *
     * @param klass The class to create an ObjectNode for
     * @param parentPrefix Optional parent prefix for qualified naming (used for sealed subclasses)
     */
    protected abstract fun createObjectNode(
        klass: KClass<*>,
        parentPrefix: String? = null,
    ): ObjectNode

    /**
     * Extracts properties from the primary constructor of a class.
     *
     * This method processes constructor parameters to create Property objects,
     * handling type conversion, default values, descriptions, and nullability.
     *
     * @param klass The class whose constructor to analyze
     * @param defaultValues Map of property names to their default values (from DefaultValueExtractor)
     * @return Pair of (list of properties, set of required property names)
     */
    protected fun extractConstructorProperties(
        klass: KClass<*>,
        defaultValues: Map<String, Any?>,
    ): Pair<List<Property>, Set<String>> {
        val properties = mutableListOf<Property>()
        val requiredProperties = mutableSetOf<String>()

        klass.constructors.firstOrNull()?.parameters?.forEach { param ->
            val propertyName = param.name ?: return@forEach
            val hasDefault = param.isOptional

            // Find the corresponding property to get annotations
            val property = findPropertyByName(klass, propertyName)

            val propertyType = param.type
            val typeRef = convertKTypeToTypeRef(propertyType)

            // Get the actual default value if available
            val defaultValue = if (hasDefault) defaultValues[propertyName] else null

            properties +=
                Property(
                    name = propertyName,
                    type = typeRef,
                    description = property?.let { extractDescription(it.annotations) },
                    hasDefaultValue = hasDefault,
                    defaultValue = defaultValue,
                    annotations = property?.let { extractValidationAnnotationsFromProperty(it) } ?: emptyMap(),
                )

            if (!hasDefault) {
                requiredProperties += propertyName
            }
        }

        return properties to requiredProperties
    }

    /**
     * Generates a qualified type name for a class, optionally prefixed with parent name.
     * Used for sealed class subclasses to avoid name collisions (e.g., "Parent.Child").
     *
     * @param klass The class to generate a name for
     * @param parentPrefix Optional parent prefix (typically the sealed parent's simple name)
     * @return Qualified name like "Parent.Child" if parentPrefix provided, otherwise simple name
     */
    protected fun generateQualifiedName(
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
    protected fun findPropertyByName(
        klass: KClass<*>,
        propertyName: String,
    ): kotlin.reflect.KProperty<*>? =
        klass.members
            .filterIsInstance<kotlin.reflect.KProperty<*>>()
            .firstOrNull { it.name == propertyName }

    /**
     * Extracts Jakarta Validation annotations (Min, Max, Size, Pattern, NotNull) from a list of annotations.
     * returns a map of constraint names to their values.
     */
    protected fun extractValidationAnnotations(annotations: List<Annotation>): Map<String, String?> {
        val map = mutableMapOf<String, String?>()
        for (ann in annotations) {
            processValidationAnnotation(ann, map)
        }
        return map
    }

    protected fun extractValidationAnnotationsFromProperty(property: kotlin.reflect.KProperty<*>): Map<String, String?> {
        val map = mutableMapOf<String, String?>()
        for (ann in property.annotations) {
            processValidationAnnotation(ann, map)
        }
        (property as? KProperty1<*, *>)?.javaField?.annotations?.forEach { ann ->
            processValidationAnnotation(ann, map)
        }
        return map
    }

    private fun processValidationAnnotation(ann: Annotation, map: MutableMap<String, String?>) {
        val qualifiedName = ann.annotationClass.qualifiedName ?: return
        if (!qualifiedName.startsWith("jakarta.validation.constraints.") &&
            !qualifiedName.startsWith("javax.validation.constraints.")) return

        val simpleName = ann.annotationClass.simpleName ?: return

        try {
            when (simpleName) {
                "Min" -> {
                    val value = ann.annotationClass.members.firstOrNull { it.name == "value" }?.call(ann)
                    value?.let { map["min"] = it.toString() }
                }
                "Max" -> {
                    val value = ann.annotationClass.members.firstOrNull { it.name == "value" }?.call(ann)
                    value?.let { map["max"] = it.toString() }
                }
                "Size" -> {
                    val min = ann.annotationClass.members.firstOrNull { it.name == "min" }?.call(ann)
                    val max = ann.annotationClass.members.firstOrNull { it.name == "max" }?.call(ann)

                    if (min != null && min.toString() != "0") {
                        map["size-min"] = min.toString()
                    }
                    if (max != null && max.toString() != "2147483647") {
                        map["size-max"] = max.toString()
                    }
                }
                "Pattern" -> {
                    val regexp = ann.annotationClass.members.firstOrNull { it.name == "regexp" }?.call(ann)
                    regexp?.let { map["pattern"] = it.toString() }
                }
                "NotNull" -> {
                    map["not-null"] = "true"
                }
            }
        } catch (e: Exception) {
        }
    }
}
