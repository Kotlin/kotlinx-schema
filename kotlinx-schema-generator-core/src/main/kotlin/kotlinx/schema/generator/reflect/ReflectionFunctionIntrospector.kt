package kotlinx.schema.generator.reflect

import kotlinx.schema.generator.core.ir.DefaultPresence
import kotlinx.schema.generator.core.ir.EnumNode
import kotlinx.schema.generator.core.ir.Introspections
import kotlinx.schema.generator.core.ir.ListNode
import kotlinx.schema.generator.core.ir.MapNode
import kotlinx.schema.generator.core.ir.ObjectNode
import kotlinx.schema.generator.core.ir.PrimitiveKind
import kotlinx.schema.generator.core.ir.PrimitiveNode
import kotlinx.schema.generator.core.ir.Property
import kotlinx.schema.generator.core.ir.SchemaIntrospector
import kotlinx.schema.generator.core.ir.TypeGraph
import kotlinx.schema.generator.core.ir.TypeId
import kotlinx.schema.generator.core.ir.TypeNode
import kotlinx.schema.generator.core.ir.TypeRef
import kotlin.reflect.KCallable
import kotlin.reflect.KClass
import kotlin.reflect.KParameter
import kotlin.reflect.KProperty1
import kotlin.reflect.KType

/**
 * Introspects Kotlin functions/methods using reflection to build a [TypeGraph].
 *
 * This introspector analyzes function parameters and their types to generate
 * schema IR nodes suitable for tool/function schema generation.
 *
 * ## Example
 * ```kotlin
 * fun myFunction(name: String, age: Int = 0): String = "Hello"
 * val typeGraph = ReflectionFunctionIntrospector.introspect(::myFunction)
 * ```
 */
public object ReflectionFunctionIntrospector : SchemaIntrospector<KCallable<*>> {
    override fun introspect(root: KCallable<*>): TypeGraph {
        require(!root.isSuspend) { "Suspend functions are not supported" }
        require(root.parameters.none { it.kind == KParameter.Kind.EXTENSION_RECEIVER }) {
            "Extension functions are not supported"
        }

        val context = IntrospectionContext()
        val rootRef = context.convertFunctionToTypeRef(root)
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
         * Converts a KCallable (function) to a TypeRef representing its parameters as an object.
         */
        fun convertFunctionToTypeRef(callable: KCallable<*>): TypeRef {
            val functionName = callable.name
            val id = TypeId(functionName)

            // Create an ObjectNode representing the function parameters
            val properties = mutableListOf<Property>()
            val requiredProperties = mutableSetOf<String>()

            callable.parameters.forEach { param ->
                // Skip instance parameter for member functions
                if (param.kind == KParameter.Kind.INSTANCE) return@forEach

                val paramName = param.name ?: return@forEach
                val paramType = param.type
                val hasDefault = param.isOptional
                val isNullable = paramType.isMarkedNullable

                val typeRef = convertKTypeToTypeRef(paramType)

                // Extract description from annotations
                val description = extractDescription(param.annotations)

                properties +=
                    Property(
                        name = paramName,
                        type = typeRef,
                        description = description,
                        defaultPresence = if (hasDefault) DefaultPresence.Absent else DefaultPresence.Required,
                    )

                if (!hasDefault) {
                    requiredProperties += paramName
                }
            }

            val objectNode =
                ObjectNode(
                    name = functionName,
                    properties = properties,
                    required = requiredProperties,
                    description = extractDescription(callable.annotations),
                )

            discoveredNodes[id] = objectNode
            return TypeRef.Ref(id, nullable = false)
        }

        /**
         * Converts a KType (with type arguments) to a TypeRef.
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
                else -> handleObjectType(klass, nullable, useSimpleName)
            }
        }

        private fun handleListType(
            klass: KClass<*>,
            nullable: Boolean,
        ): TypeRef {
            val elementRef = TypeRef.Inline(PrimitiveNode(PrimitiveKind.STRING), false)
            val ref = TypeRef.Inline(ListNode(elementRef), nullable)
            if (!nullable) typeRefCache[klass] = ref
            return ref
        }

        private fun handleMapType(
            klass: KClass<*>,
            nullable: Boolean,
        ): TypeRef {
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

        private fun createObjectNode(klass: KClass<*>): ObjectNode {
            // Check for @Serializable annotation - not supported
            val hasSerializable =
                klass.annotations.any {
                    it.annotationClass.qualifiedName == "kotlinx.serialization.Serializable"
                }
            require(!hasSerializable) {
                "ReflectionFunctionIntrospector does not support classes annotated with @Serializable " +
                    "(${klass.qualifiedName}). The kotlinx.serialization compiler plugin modifies the " +
                    "constructor structure, making reflection-based introspection unreliable. " +
                    "Please remove the @Serializable annotation or use a different introspector."
            }

            val properties = mutableListOf<Property>()
            val requiredProperties = mutableSetOf<String>()

            // Extract properties from primary constructor
            klass.constructors.firstOrNull()?.parameters?.forEach { param ->
                val propertyName = param.name ?: return@forEach
                val hasDefault = param.isOptional

                // Find the corresponding property to get annotations
                val property =
                    klass.members
                        .filterIsInstance<KProperty1<*, *>>()
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
