package kotlinx.schema.generator.reflect

import kotlinx.schema.generator.core.ir.DefaultPresence
import kotlinx.schema.generator.core.ir.Discriminator
import kotlinx.schema.generator.core.ir.EnumNode
import kotlinx.schema.generator.core.ir.Introspections
import kotlinx.schema.generator.core.ir.ListNode
import kotlinx.schema.generator.core.ir.MapNode
import kotlinx.schema.generator.core.ir.ObjectNode
import kotlinx.schema.generator.core.ir.PolymorphicNode
import kotlinx.schema.generator.core.ir.PrimitiveKind
import kotlinx.schema.generator.core.ir.PrimitiveNode
import kotlinx.schema.generator.core.ir.Property
import kotlinx.schema.generator.core.ir.SchemaIntrospector
import kotlinx.schema.generator.core.ir.SubtypeRef
import kotlinx.schema.generator.core.ir.TypeGraph
import kotlinx.schema.generator.core.ir.TypeId
import kotlinx.schema.generator.core.ir.TypeNode
import kotlinx.schema.generator.core.ir.TypeRef
import kotlin.reflect.KClass
import kotlin.reflect.KProperty
import kotlin.reflect.KProperty1
import kotlin.reflect.KType

/**
 * Introspects Kotlin classes using reflection to build a [TypeGraph].
 *
 * This introspector analyzes class structures including properties, constructors,
 * and type hierarchies to generate schema IR nodes.
 *
 *  ## Example
 *  ```kotlin
 *  val typeGraph = ReflectionIntrospector.introspect(MyClass::class)
 *  ```
 *
 * ## Limitations
 * - Requires classes to have a primary constructor
 * - Type parameters are not fully supported
 */
public object ReflectionIntrospector : SchemaIntrospector<KClass<*>> {
    override fun introspect(root: KClass<*>): TypeGraph {
        val context = IntrospectionContext()
        val rootRef = context.convertToTypeRef(root)
        return TypeGraph(root = rootRef, nodes = context.discoveredNodes)
    }

    /**
     * Maintains state during introspection including discovered nodes,
     * visited classes, and type reference cache.
     */
    @Suppress("TooManyFunctions")
    private class IntrospectionContext {
        val discoveredNodes = linkedMapOf<TypeId, TypeNode>()
        private val visitingClasses = mutableSetOf<KClass<*>>()
        private val typeRefCache = mutableMapOf<KClass<*>, TypeRef>()

        /**
         * Converts a KClass to a TypeRef, handling caching and nullability.
         */
        @Suppress("ReturnCount")
        fun convertToTypeRef(
            klass: KClass<*>,
            nullable: Boolean = false,
            useSimpleName: Boolean = false,
        ): TypeRef {
            // Check cache for non-nullable version, adjust if needed
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
                klass == Map::class -> handleMapType(klass, nullable)
                isEnumClass(klass) -> handleEnumType(klass, nullable)
                klass.isSealed -> handleSealedType(klass, nullable)
                else -> handleObjectType(klass, nullable, useSimpleName)
            }
        }

        /**
         * Converts a KType (with type arguments) to a TypeRef.
         * Used for property types where we have full type information.
         */
        private fun convertKTypeToTypeRef(type: KType): TypeRef {
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

                classifier == Map::class -> {
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

                else -> convertToTypeRef(classifier, isNullable)
            }
        }

        private fun handleListType(
            klass: KClass<*>,
            nullable: Boolean,
        ): TypeRef {
            // Fallback for when we don't have type arguments (shouldn't happen in normal usage)
            val elementRef = TypeRef.Inline(PrimitiveNode(PrimitiveKind.STRING), false)
            val ref = TypeRef.Inline(ListNode(elementRef), nullable)
            if (!nullable) typeRefCache[klass] = ref
            return ref
        }

        private fun handleMapType(
            klass: KClass<*>,
            nullable: Boolean,
        ): TypeRef {
            // Fallback for when we don't have type arguments
            val keyRef = TypeRef.Inline(PrimitiveNode(PrimitiveKind.STRING), false)
            val valueRef = TypeRef.Inline(PrimitiveNode(PrimitiveKind.STRING), false)
            val ref = TypeRef.Inline(MapNode(keyRef, valueRef), nullable)
            if (!nullable) typeRefCache[klass] = ref
            return ref
        }

        private fun handleEnumType(
            klass: KClass<*>,
            nullable: Boolean,
        ): TypeRef {
            val id = createTypeId(klass)

            if (shouldProcessClass(klass, id)) {
                markAsVisiting(klass)
                val enumNode = createEnumNode(klass)
                discoveredNodes[id] = enumNode
                unmarkAsVisiting(klass)
            }

            val ref = TypeRef.Ref(id, nullable)
            if (!nullable) typeRefCache[klass] = ref
            return ref
        }

        private fun handleSealedType(
            klass: KClass<*>,
            nullable: Boolean,
        ): TypeRef {
            val id = createTypeId(klass)

            if (shouldProcessClass(klass, id)) {
                markAsVisiting(klass)
                val polymorphicNode = createPolymorphicNode(klass)
                discoveredNodes[id] = polymorphicNode

                // Process each sealed subclass
                klass.sealedSubclasses.forEach { subclass ->
                    convertToTypeRef(subclass, nullable = false, useSimpleName = true)
                }

                unmarkAsVisiting(klass)
            }

            val ref = TypeRef.Ref(id, nullable)
            if (!nullable) typeRefCache[klass] = ref
            return ref
        }

        private fun handleObjectType(
            klass: KClass<*>,
            nullable: Boolean,
            useSimpleName: Boolean,
        ): TypeRef {
            val id =
                if (useSimpleName) {
                    TypeId(klass.simpleName ?: "Unknown")
                } else {
                    createTypeId(klass)
                }

            if (shouldProcessClass(klass, id)) {
                markAsVisiting(klass)
                val objectNode = createObjectNode(klass)
                discoveredNodes[id] = objectNode
                unmarkAsVisiting(klass)
            }

            val ref = TypeRef.Ref(id, nullable)
            if (!nullable) typeRefCache[klass] = ref
            return ref
        }

        private fun createEnumNode(klass: KClass<*>): EnumNode {
            @Suppress("UNCHECKED_CAST")
            val enumConstants = (klass.java as Class<out Enum<*>>).enumConstants
            return EnumNode(
                name = klass.simpleName ?: "UnknownEnum",
                entries = enumConstants.map { it.name },
                description = extractDescription(klass.annotations),
            )
        }

        private fun createPolymorphicNode(klass: KClass<*>): PolymorphicNode {
            val subtypes =
                klass.sealedSubclasses.map { subclass ->
                    SubtypeRef(TypeId(subclass.simpleName ?: "Unknown"))
                }
            return PolymorphicNode(
                baseName = klass.simpleName ?: "UnknownSealed",
                subtypes = subtypes,
                discriminator = Discriminator(name = "type", required = true, mapping = null),
                description = extractDescription(klass.annotations),
            )
        }

        private fun createObjectNode(klass: KClass<*>): ObjectNode {
            val properties = mutableListOf<Property>()
            val requiredProperties = mutableSetOf<String>()

            // Extract properties from primary constructor
            klass.constructors.firstOrNull()?.parameters?.forEach { param ->
                val propertyName = param.name ?: return@forEach
                val hasDefault = param.isOptional

                // Find the corresponding property to get annotations
                val property =
                    klass.members
                        .filterIsInstance<KProperty<*>>()
                        .firstOrNull { it.name == propertyName }

                val propertyType = param.type
                val typeRef = convertKTypeToTypeRef(propertyType)

                properties +=
                    Property(
                        name = propertyName,
                        type = typeRef,
                        description = property?.let { extractDescription(it.annotations) },
                        defaultPresence = if (hasDefault) DefaultPresence.Absent else DefaultPresence.Required,
                    )

                if (!hasDefault) {
                    requiredProperties += propertyName
                }
            }

            return ObjectNode(
                name = klass.simpleName ?: "UnknownClass",
                properties = properties,
                required = requiredProperties,
                description = extractDescription(klass.annotations),
            )
        }

        private fun shouldProcessClass(
            klass: KClass<*>,
            id: TypeId,
        ): Boolean = id !in discoveredNodes.keys && klass !in visitingClasses

        private fun markAsVisiting(klass: KClass<*>) {
            visitingClasses += klass
        }

        private fun unmarkAsVisiting(klass: KClass<*>) {
            visitingClasses -= klass
        }

        private fun createTypeId(klass: KClass<*>): TypeId =
            TypeId(klass.qualifiedName ?: klass.simpleName ?: "Anonymous")

        private fun isListLike(klass: KClass<*>): Boolean =
            klass == List::class || klass == Collection::class || klass == Iterable::class

        private fun isEnumClass(klass: KClass<*>): Boolean = klass.isData == false && klass.java.isEnum

        private fun primitiveKindFor(klass: KClass<*>): PrimitiveKind? =
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

        private fun extractDescription(annotations: List<Annotation>): String? =
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

        private fun TypeRef.withNullable(nullable: Boolean): TypeRef =
            when (this) {
                is TypeRef.Inline -> copy(nullable = nullable)
                is TypeRef.Ref -> copy(nullable = nullable)
            }
    }
}
