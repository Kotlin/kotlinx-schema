package kotlinx.schema.generator.reflect

import kotlinx.schema.generator.core.InternalSchemaGeneratorApi
import kotlinx.schema.generator.core.ir.BaseIntrospectionContext
import kotlinx.schema.generator.core.ir.Discriminator
import kotlinx.schema.generator.core.ir.EnumNode
import kotlinx.schema.generator.core.ir.ListNode
import kotlinx.schema.generator.core.ir.MapNode
import kotlinx.schema.generator.core.ir.ObjectNode
import kotlinx.schema.generator.core.ir.PolymorphicNode
import kotlinx.schema.generator.core.ir.PrimitiveKind
import kotlinx.schema.generator.core.ir.PrimitiveNode
import kotlinx.schema.generator.core.ir.Property
import kotlinx.schema.generator.core.ir.SubtypeRef
import kotlinx.schema.generator.core.ir.TypeId
import kotlinx.schema.generator.core.ir.TypeRef
import kotlin.reflect.KClass
import kotlin.reflect.KProperty
import kotlin.reflect.KType
import kotlin.reflect.full.createType

/**
 * Reflection-based introspection context based on [KType].
 * Only supports [KClass] classifiers for introspection, generics are not supported.
 */
@Suppress("TooManyFunctions")
@OptIn(InternalSchemaGeneratorApi::class)
internal class ReflectionIntrospectionContext : BaseIntrospectionContext<KType>() {
    /**
     * This is a shared instance, so different schema generation runs would reuse the same class metadata cache.
     */
    private val defaultValueExtractor = DefaultValueExtractor

    /**
     * Converts a [KType] to a [TypeRef].
     * This is the main entry point for type conversion.
     *
     * Handles:
     * - Nullability from descriptor.isNullable
     * - Primitives (inlined)
     * - Collections (List, Map) (inlined)
     * - Enums (referenced via TypeId)
     * - Objects/Classes (referenced via TypeId)
     * - Polymorphic types (referenced via TypeId)
     */
    @Suppress("ReturnCount")
    override fun toRef(type: KType): TypeRef {
        val klass = type.klass
        val nullable = type.isMarkedNullable

        // Check cache first
        typeRefCache[type]?.let { cachedRef ->
            return if (nullable && !cachedRef.nullable) {
                cachedRef.withNullable(true)
            } else {
                cachedRef
            }
        }

        // Try to convert to primitive type
        primitiveKindFor(klass)?.let { primitiveKind ->
            val ref = TypeRef.Inline(PrimitiveNode(primitiveKind), nullable)
            if (!nullable) typeRefCache[type] = ref
            return ref
        }

        // Handle different kinds
        return when {
            isListLike(klass) -> handleListType(type)
            isMapLike(klass) -> handleMapType(type)
            isEnumClass(klass) -> handleEnumType(type)
            klass.isSealed -> handleSealedType(type)
            else -> handleObjectType(type)
        }
    }

    //region KClass type matchers

    /**
     * Checks and maps a Kotlin primitive class to its corresponding [PrimitiveKind].
     * Returns null if the class is not a supported primitive type.
     */
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

    /**
     * Checks if a class is list-like (List, Collection, or Iterable).
     */
    private fun isListLike(klass: KClass<*>): Boolean = Iterable::class.java.isAssignableFrom(klass.java)

    /**
     * Checks if a class is a map-like type (Map).
     */
    private fun isMapLike(klass: KClass<*>): Boolean = Map::class.java.isAssignableFrom(klass.java)

    /**
     * Checks if a class is an enum class.
     */
    private fun isEnumClass(klass: KClass<*>): Boolean = !klass.isData && klass.java.isEnum

    //endregion

    //region KType to TypeRef conversion handlers

    /**
     * Handles list-like types (List, Collection, Iterable).
     */
    private fun handleListType(type: KType): TypeRef {
        val elementType = type.arguments.firstOrNull()?.type

        val elementRef =
            elementType
                ?.let { toRef(it) }
                ?: TypeRef.Inline(PrimitiveNode(PrimitiveKind.STRING), false)

        val ref = TypeRef.Inline(ListNode(elementRef), type.isMarkedNullable)
        if (!type.isMarkedNullable) typeRefCache[type] = ref
        return ref
    }

    /**
     * Handles Map types.
     * Creates a fallback MapNode with String keys and values when type arguments are unavailable.
     */
    private fun handleMapType(type: KType): TypeRef {
        val keyType = type.arguments.getOrNull(0)?.type
        val valueType = type.arguments.getOrNull(1)?.type

        val keyRef =
            keyType
                ?.let { toRef(it) }
                ?: TypeRef.Inline(PrimitiveNode(PrimitiveKind.STRING), false)

        val valueRef =
            valueType
                ?.let { toRef(it) }
                ?: TypeRef.Inline(PrimitiveNode(PrimitiveKind.STRING), false)

        val ref = TypeRef.Inline(MapNode(keyRef, valueRef), type.isMarkedNullable)
        if (!type.isMarkedNullable) typeRefCache[type] = ref
        return ref
    }

    /**
     * Handles enum types by creating an EnumNode and adding it to discovered nodes.
     */
    private fun handleEnumType(type: KType): TypeRef {
        val id = createTypeId(type.klass)

        withCycleDetection(type, id) {
            createEnumNode(type.klass)
        }

        val ref = TypeRef.Ref(id, type.isMarkedNullable)
        if (!type.isMarkedNullable) typeRefCache[type] = ref
        return ref
    }

    /**
     * Handles object/class types by creating an ObjectNode.
     */
    private fun handleObjectType(
        type: KType,
        parentPrefix: String? = null,
    ): TypeRef {
        val klass = type.klass

        val id =
            when {
                parentPrefix != null -> TypeId(generateQualifiedName(klass, parentPrefix))
                else -> createTypeId(klass)
            }

        withCycleDetection(type, id) {
            createObjectNode(klass)
        }

        val ref = TypeRef.Ref(id, type.isMarkedNullable)
        if (!type.isMarkedNullable) typeRefCache[type] = ref
        return ref
    }

    /**
     * Handles sealed types by creating a PolymorphicNode and processing each sealed subclass.
     */
    private fun handleSealedType(type: KType): TypeRef {
        val klass = type.klass
        val id = createTypeId(klass)

        withCycleDetection(type, id) {
            val polymorphicNode = createPolymorphicNode(klass)

            // Process each sealed subclass with parent-qualified names
            val parentName = klass.simpleName ?: "UnknownSealed"
            klass.sealedSubclasses.forEach { subclass ->
                val subclassType = subclass.createType()

                handleObjectType(
                    type = subclassType,
                    parentPrefix = parentName,
                )
            }

            polymorphicNode
        }

        val ref = TypeRef.Ref(id, type.isMarkedNullable)
        if (!type.isMarkedNullable) typeRefCache[type] = ref
        return ref
    }

    //endregion

    //region Create methods

    /**
     * Creates a [TypeId] from a [KClass], using qualified name or simple name as fallback.
     */
    private fun createTypeId(klass: KClass<*>): TypeId = TypeId(klass.qualifiedName ?: klass.simpleName ?: "Anonymous")

    /**
     * Creates an [EnumNode] from an enum [KClass].
     */
    private fun createEnumNode(klass: KClass<*>): EnumNode {
        @Suppress("UNCHECKED_CAST")
        val enumConstants = (klass.java as Class<out Enum<*>>).enumConstants
        return EnumNode(
            name = klass.simpleName ?: "UnknownEnum",
            entries = enumConstants.map { it.name },
            description = extractDescription(klass.annotations),
        )
    }

    /**
     * Creates an [ObjectNode] from a [KClass].
     */
    @Suppress("LongMethod", "CyclomaticComplexMethod")
    private fun createObjectNode(klass: KClass<*>): ObjectNode {
        val properties = mutableListOf<Property>()
        val requiredProperties = mutableSetOf<String>()

        // Find sealed parent classes to inherit property descriptions
        val sealedParents =
            klass.supertypes
                .mapNotNull { it.classifier as? KClass<*> }
                .filter { it.isSealed }

        // Build a map of parent property descriptions and properties
        val parentPropertyDescriptions = mutableMapOf<String, String>()
        val parentProperties = mutableSetOf<String>()
        sealedParents.forEach { parent ->
            parent.members
                .filterIsInstance<KProperty<*>>()
                .forEach { prop ->
                    parentProperties.add(prop.name)
                    val desc = extractDescription(prop.annotations)
                    if (desc != null) {
                        parentPropertyDescriptions[prop.name] = desc
                    }
                }
        }

        // Try to extract default values by creating an instance
        val defaultValues = defaultValueExtractor.extractDefaultValues(klass)

        // Extract properties from primary constructor using shared method
        val (constructorProperties, constructorRequired) = extractConstructorProperties(klass, defaultValues)

        // Track which properties were processed from constructor
        val processedProperties = constructorProperties.map { it.name }.toSet()

        // If there are sealed parents, update descriptions to inherit from parent if needed
        if (sealedParents.isNotEmpty()) {
            constructorProperties.forEach { prop ->
                val updatedProp =
                    if (prop.description == null && parentPropertyDescriptions.containsKey(prop.name)) {
                        prop.copy(description = parentPropertyDescriptions[prop.name])
                    } else {
                        prop
                    }
                properties += updatedProp
            }
        } else {
            properties += constructorProperties
        }

        requiredProperties += constructorRequired

        // Add inherited properties from sealed parents that weren't in the constructor
        val inheritedPropertyNames = parentProperties - processedProperties
        inheritedPropertyNames.forEach { propertyName ->
            // Find the property in the current class (inherited)
            val property = findPropertyByName(klass, propertyName)

            if (property != null) {
                val typeRef = toRef(property.returnType)
                val description = parentPropertyDescriptions[propertyName]

                // For inherited properties, try to get the fixed value from the instance
                val fixedValue = defaultValues[propertyName]

                properties +=
                    Property(
                        name = propertyName,
                        type = typeRef,
                        description = description,
                        hasDefaultValue = fixedValue != null,
                        defaultValue = fixedValue,
                    )

                // Inherited properties with fixed values are required
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

    /**
     * Creates a [PolymorphicNode] from a [KClass]
     */
    private fun createPolymorphicNode(klass: KClass<*>): PolymorphicNode {
        val parentName = klass.simpleName ?: "UnknownSealed"

        val subtypes =
            klass.sealedSubclasses.map { subclass ->
                SubtypeRef(TypeId(generateQualifiedName(subclass, parentName)))
            }

        // Build discriminator mapping: discriminator value -> TypeId
        val discriminatorMapping =
            klass.sealedSubclasses.associate { subclass ->
                val simpleName = subclass.simpleName ?: "Unknown"
                simpleName to TypeId(generateQualifiedName(subclass, parentName))
            }

        return PolymorphicNode(
            baseName = parentName,
            subtypes = subtypes,
            discriminator =
                Discriminator(
                    // TODO allow to configure discriminator property name
                    name = "type",
                    mapping = discriminatorMapping,
                ),
            description = extractDescription(klass.annotations),
        )
    }

    //endregion

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
    private fun extractConstructorProperties(
        klass: KClass<*>,
        defaultValues: Map<String, Any?>,
    ): Pair<List<Property>, Set<String>> {
        val properties = mutableListOf<Property>()
        val requiredProperties = mutableSetOf<String>()

        klass.constructors.firstOrNull()?.parameters?.forEach { param ->
            val propertyName = param.name ?: return@forEach
            val hasDefault = param.isOptional

            // Get annotations both on the constructor parameter and property associated with it
            val annotations = param.annotations + findPropertyByName(klass, propertyName)?.annotations.orEmpty()

            val propertyType = param.type
            val typeRef = toRef(propertyType)

            // Get the actual default value if available
            val defaultValue = if (hasDefault) defaultValues[propertyName] else null

            properties +=
                Property(
                    name = propertyName,
                    type = typeRef,
                    description = extractDescription(annotations),
                    hasDefaultValue = hasDefault,
                    defaultValue = defaultValue,
                )

            if (!hasDefault) {
                requiredProperties += propertyName
            }
        }

        return properties to requiredProperties
    }
}
