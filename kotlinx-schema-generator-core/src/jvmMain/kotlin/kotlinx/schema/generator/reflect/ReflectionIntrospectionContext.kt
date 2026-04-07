package kotlinx.schema.generator.reflect

import kotlinx.schema.generator.core.ir.AnyNode
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
import kotlinx.serialization.descriptors.SerialKind
import kotlinx.serialization.serializerOrNull
import kotlin.reflect.KClass
import kotlin.reflect.KProperty
import kotlin.reflect.KType
import kotlin.reflect.KVisibility
import kotlin.reflect.full.createType
import kotlinx.serialization.descriptors.PrimitiveKind as SerialPrimitiveKind

/**
 * Reflection-based introspection context based on [KType].
 * Only supports [KClass] classifiers for introspection, generics are not supported.
 */
@Suppress("TooManyFunctions")
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
    @Suppress("ReturnCount", "CyclomaticComplexMethod")
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

        // kotlin.Any / java.lang.Object: any value — emit empty schema {}
        if (klass == Any::class) {
            return TypeRef.Inline(AnyNode(), nullable)
        }

        // Try to convert to primitive type
        primitiveKindFor(klass)?.let { primitiveKind ->
            val ref = TypeRef.Inline(PrimitiveNode(primitiveKind), nullable)
            if (!nullable) typeRefCache[type] = ref
            return ref
        }

        // If kotlinx-serialization can resolve a serializer for this type whose descriptor
        // describes a primitive (e.g. the built-in `kotlin.uuid.Uuid` serializer, or any
        // user-defined `@Serializable(with = X)` typealias attached to a custom string/long/
        // etc. serializer), trust that and emit the matching primitive node. This keeps the
        // reflection-side schema in agreement with the actual wire format used by
        // kotlinx-serialization at runtime, which previously diverged for types like
        // `Uuid` (string at the wire, structural object in the reflection schema).
        serializerPrimitiveFor(type)?.let { primitiveKind ->
            val ref = TypeRef.Inline(PrimitiveNode(primitiveKind), nullable)
            if (!nullable) typeRefCache[type] = ref
            return ref
        }

        // Inline value classes (`@JvmInline value class Wrapper(val inner: T)`) serialize
        // as their inner value, not as a `{inner: …}` object. Mirror the inline-class
        // flattening already implemented in the serialization-based introspector by
        // recursing into the wrapped element's KType — which crucially still carries any
        // typealias-expanded annotations on it.
        if (klass.isValue) return handleInlineValueClass(type)

        // Handle different kinds
        return when {
            isListLike(klass) -> handleListType(type)
            isMapLike(klass) -> handleMapType(type)
            isEnumClass(klass) -> handleEnumType(type)
            klass.isSealed -> handleSealedType(type)
            else -> handleObjectType(type)
        }
    }

    /**
     * If [type] has an associated kotlinx-serialization serializer whose descriptor is a
     * primitive kind, returns the matching kotlinx-schema [PrimitiveKind]. Otherwise null.
     *
     * Uses [serializerOrNull] (not `serializer`) so unknown types fall through cleanly to
     * the existing structural handling instead of throwing.
     *
     * The lookup respects:
     * - The `@Serializable(with = X::class)` annotation expanded from a typealias used at
     *   the call site (the annotation is preserved on `KType.annotations` after expansion).
     * - Built-in serializers shipped by `kotlinx-serialization` for stdlib types like
     *   `kotlin.uuid.Uuid`, which are correctly described as `PrimitiveKind.STRING`.
     * - Contextual serializers registered in the current module — kotlinx-serialization's
     *   resolution rules apply uniformly.
     */
    private fun serializerPrimitiveFor(type: KType): PrimitiveKind? {
        val serializer = serializerOrNull(type) ?: return null
        return serialKindToPrimitive(serializer.descriptor.kind)
    }

    /**
     * Maps a kotlinx-serialization [SerialKind] to a kotlinx-schema [PrimitiveKind] when
     * the kind describes a primitive. Returns null for any structural / enum / polymorphic
     * kind so the caller falls through to existing handling.
     */
    private fun serialKindToPrimitive(kind: SerialKind): PrimitiveKind? = when (kind) {
        SerialPrimitiveKind.STRING, SerialPrimitiveKind.CHAR -> PrimitiveKind.STRING
        SerialPrimitiveKind.BOOLEAN -> PrimitiveKind.BOOLEAN
        SerialPrimitiveKind.BYTE, SerialPrimitiveKind.SHORT, SerialPrimitiveKind.INT -> PrimitiveKind.INT
        SerialPrimitiveKind.LONG -> PrimitiveKind.LONG
        SerialPrimitiveKind.FLOAT -> PrimitiveKind.FLOAT
        SerialPrimitiveKind.DOUBLE -> PrimitiveKind.DOUBLE
        else -> null
    }

    /**
     * Flattens an `@JvmInline value class` to its wrapped element, preserving any
     * annotations carried on the inner [KType] (e.g. typealias-expanded `@Serializable`).
     * Mirrors the equivalent fix in `SerializationIntrospectionContext` so the two
     * introspector paths agree about what an inline value class looks like.
     */
    private fun handleInlineValueClass(type: KType): TypeRef {
        val klass = type.klass
        val innerParam = findPrimaryConstructor(klass)?.parameters?.firstOrNull()
            ?: return handleObjectType(type)
        val innerRef = toRef(innerParam.type)
        val nullable = type.isMarkedNullable
        val ref = if (nullable && !innerRef.nullable) innerRef.withNullable(true) else innerRef
        if (!nullable) typeRefCache[type] = ref
        return ref
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
    private fun handleObjectType(type: KType): TypeRef {
        val klass = type.klass
        val id = createTypeId(klass)

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
            val filteredSubclasses = klass.filteredSealedSubclasses()
            val polymorphicNode = createPolymorphicNode(klass, filteredSubclasses)

            filteredSubclasses.forEach { subclass ->
                toRef(subclass.createType())
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
     * Creates a [TypeId] from a [KClass], using `@SerialName` override if present,
     * or qualified name / simple name as fallback.
     */
    private fun createTypeId(klass: KClass<*>): TypeId {
        val nameOverride = extractNameOverride(klass.java.annotations.toList())
        return TypeId(nameOverride ?: klass.qualifiedName ?: klass.simpleName ?: "Anonymous")
    }

    /**
     * Creates an [EnumNode] from an enum [KClass].
     *
     * Respects `@SerialName` on the enum class (overrides the class name)
     * and on individual enum entries (overrides the entry name in the schema).
     */
    private fun createEnumNode(klass: KClass<*>): EnumNode {
        @Suppress("UNCHECKED_CAST")
        val enumConstants = (klass.java as Class<out Enum<*>>).enumConstants
        val entries = enumConstants.map { constant ->
            val field = klass.java.getField(constant.name)
            extractNameOverride(field.annotations.toList()) ?: constant.name
        }
        val nameOverride = extractNameOverride(klass.java.annotations.toList())
        return EnumNode(
            name = nameOverride ?: klass.simpleName ?: "UnknownEnum",
            entries = entries,
            description = extractDescription(klass.java.annotations.toList()),
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
        val processedProperties = constructorProperties.map { it.name }.toMutableSet()

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
                        isConstant = fixedValue != null,
                    )

                // Inherited properties with fixed values are required
                requiredProperties += propertyName
                processedProperties += propertyName
            }
        }

        // Add public properties for objects (singletons) that weren't in the constructor or from parents
        if (klass.objectInstance != null) {
            klass.members
                .filterIsInstance<KProperty<*>>()
                .filter { it.visibility == KVisibility.PUBLIC }
                .forEach { prop ->
                    if (prop.name !in processedProperties) {
                        val fixedValue = defaultValues[prop.name]
                        properties +=
                            Property(
                                name = prop.name,
                                type = toRef(prop.returnType),
                                description = extractDescription(prop.annotations),
                                hasDefaultValue = fixedValue != null,
                                defaultValue = fixedValue,
                                isConstant = fixedValue != null,
                            )
                        requiredProperties += prop.name
                        processedProperties += prop.name
                    }
                }
        }

        val nameOverride = extractNameOverride(klass.java.annotations.toList())
        return ObjectNode(
            name = nameOverride ?: klass.simpleName ?: "UnknownClass",
            properties = properties,
            required = requiredProperties,
            description = extractDescription(klass.java.annotations.toList()),
        )
    }

    /**
     * Creates a [PolymorphicNode] from a [KClass] using the given [sealedSubclasses]
     * (already filtered to exclude `@SchemaIgnore`-annotated subtypes).
     */
    private fun createPolymorphicNode(
        klass: KClass<*>,
        sealedSubclasses: List<KClass<*>>,
    ): PolymorphicNode {
        val nameOverride = extractNameOverride(klass.java.annotations.toList())
        val baseName = nameOverride ?: klass.simpleName ?: "UnknownSealed"

        val subtypes =
            sealedSubclasses.map { subclass ->
                SubtypeRef(createTypeId(subclass))
            }

        // Build discriminator mapping: discriminator value -> TypeId
        // Key must equal the TypeId value so it matches the `const` value the transformer emits
        val discriminatorMapping =
            sealedSubclasses.associate { subclass ->
                val id = createTypeId(subclass)
                id.value to id
            }

        return PolymorphicNode(
            baseName = baseName,
            subtypes = subtypes,
            discriminator =
                Discriminator(
                    // TODO allow to configure discriminator property name
                    name = "type",
                    mapping = discriminatorMapping,
                ),
            description = extractDescription(klass.java.annotations.toList()),
        )
    }

    /**
     * Returns sealed subclasses excluding those annotated with a recognized ignore annotation.
     */
    private fun KClass<*>.filteredSealedSubclasses(): List<KClass<*>> =
        sealedSubclasses.filter { !isSchemaIgnored(it.annotations) }

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

        val constructor = findPrimaryConstructor(klass)

        constructor?.parameters?.forEach { param ->
            val kotlinName = param.name ?: return@forEach
            val hasDefault = param.isOptional

            // Get annotations both on the constructor parameter and property associated with it
            val annotations = param.annotations + findPropertyByName(klass, kotlinName)?.annotations.orEmpty()

            // Use @SerialName override if present, otherwise use Kotlin property name
            val propertyName = extractNameOverride(annotations) ?: kotlinName

            val propertyType = param.type
            val typeRef = toRef(propertyType)

            // Get the actual default value if available
            val defaultValue = if (hasDefault) defaultValues[kotlinName] else null

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
