package kotlinx.schema.generator.reflect

import kotlinx.schema.generator.core.ir.ListNode
import kotlinx.schema.generator.core.ir.MapNode
import kotlinx.schema.generator.core.ir.ObjectNode
import kotlinx.schema.generator.core.ir.PrimitiveKind
import kotlinx.schema.generator.core.ir.PrimitiveNode
import kotlinx.schema.generator.core.ir.TypeId
import kotlinx.schema.generator.core.ir.TypeNode
import kotlinx.schema.generator.core.ir.TypeRef
import kotlin.reflect.KClass
import kotlin.reflect.KType

/**
 * Base class for introspection contexts that maintain state during type graph construction.
 *
 * This class provides common functionality for both class and function introspection,
 * including type conversion, caching, and cycle detection.
 *
 * Subclasses must implement [createObjectNode] to define how to extract properties
 * from different source types (classes vs function parameters).
 */
@Suppress("TooManyFunctions")
internal abstract class BaseIntrospectionContext {
    /**
     * Map of discovered type nodes indexed by their type ID.
     */
    val discoveredNodes = linkedMapOf<TypeId, TypeNode>()

    /**
     * Set of classes currently being visited (for cycle detection).
     */
    private val visitingClasses = mutableSetOf<KClass<*>>()

    /**
     * Cache of type references to avoid redundant processing.
     */
    protected val typeRefCache = mutableMapOf<KClass<*>, TypeRef>()

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
     */
    @Suppress("ReturnCount")
    internal abstract fun convertToTypeRef(
        klass: KClass<*>,
        nullable: Boolean = false,
        useSimpleName: Boolean = false,
    ): TypeRef

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

    /**
     * Handles object/class types by creating an ObjectNode.
     * Delegates actual node creation to [createObjectNode] which is implemented by subclasses.
     */
    protected fun handleObjectType(
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

    /**
     * Creates an ObjectNode from a KClass.
     * This is the main extension point for subclasses to customize how properties are extracted.
     */
    protected abstract fun createObjectNode(klass: KClass<*>): ObjectNode

    /**
     * Checks if a class should be processed (not already discovered and not currently being visited).
     */
    protected fun shouldProcessClass(
        klass: KClass<*>,
        id: TypeId,
    ): Boolean = id !in discoveredNodes.keys && klass !in visitingClasses

    /**
     * Marks a class as currently being visited (for cycle detection).
     */
    protected fun markAsVisiting(klass: KClass<*>) {
        visitingClasses += klass
    }

    /**
     * Removes a class from the visiting set.
     */
    protected fun unmarkAsVisiting(klass: KClass<*>) {
        visitingClasses -= klass
    }
}
