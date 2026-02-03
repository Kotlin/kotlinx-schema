package kotlinx.schema.generator.json.serialization

import kotlinx.schema.Description
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
import kotlinx.schema.generator.core.ir.TypeNode
import kotlinx.schema.generator.core.ir.TypeRef
import kotlinx.serialization.descriptors.PolymorphicKind
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.SerialKind
import kotlinx.serialization.descriptors.StructureKind
import kotlinx.serialization.json.ClassDiscriminatorMode
import kotlinx.serialization.json.Json
import kotlinx.serialization.descriptors.PrimitiveKind as SerialPrimitiveKind

/**
 * Context for introspecting kotlinx.serialization descriptors into Schema IR.
 *
 * Extends [BaseIntrospectionContext] to leverage shared state management
 * (discovered nodes, visiting set for cycle detection, type reference cache).
 *
 * @property json The [Json] configuration used to extract discriminator settings for polymorphic types
 */
@Suppress("TooManyFunctions")
@OptIn(InternalSchemaGeneratorApi::class)
internal class SerializationIntrospectionContext(
    private val json: Json,
) : BaseIntrospectionContext<SerialDescriptor, SerialDescriptor>() {
    /**
     * Returns the discovered type nodes.
     * This is the main output of the introspection process.
     */
    fun nodes(): Map<TypeId, TypeNode> = discoveredNodes

    /**
     * Converts a [SerialDescriptor] to a [TypeRef].
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
    fun toRef(descriptor: SerialDescriptor): TypeRef {
        // Check cache first
        typeRefCache[descriptor]?.let { cachedRef ->
            return if (descriptor.isNullable && !cachedRef.nullable) {
                cachedRef.withNullable(true)
            } else {
                cachedRef
            }
        }

        val nullable = descriptor.isNullable

        // Try primitives first (always inlined)
        primitiveFor(descriptor)?.let { primitiveNode ->
            val ref = TypeRef.Inline(primitiveNode, nullable)
            if (!nullable) typeRefCache[descriptor] = ref
            return ref
        }

        // Handle different kinds
        return when (descriptor.kind) {
            is SerialKind.ENUM -> {
                handleEnumType(descriptor, nullable)
            }

            is StructureKind.CLASS, StructureKind.OBJECT -> {
                handleObjectType(descriptor, nullable)
            }

            is StructureKind.MAP -> {
                handleMapType(descriptor, nullable)
            }

            is StructureKind.LIST -> {
                handleListType(descriptor, nullable)
            }

            is PolymorphicKind -> {
                handlePolymorphicType(descriptor, nullable)
            }

            else -> {
                // Fallback: treat unknown kinds as empty objects
                handleUnknownType(descriptor, nullable)
            }
        }
    }

    /**
     * Maps a kotlinx.serialization [SerialDescriptor] with primitive kind to a [PrimitiveNode].
     * Returns null if the descriptor is not a primitive.
     */
    private fun primitiveFor(descriptor: SerialDescriptor): PrimitiveNode? =
        when (descriptor.kind) {
            SerialPrimitiveKind.STRING -> {
                PrimitiveNode(PrimitiveKind.STRING)
            }

            SerialPrimitiveKind.BOOLEAN -> {
                PrimitiveNode(PrimitiveKind.BOOLEAN)
            }

            SerialPrimitiveKind.BYTE, SerialPrimitiveKind.SHORT, SerialPrimitiveKind.INT -> {
                PrimitiveNode(PrimitiveKind.INT)
            }

            SerialPrimitiveKind.LONG -> {
                PrimitiveNode(PrimitiveKind.LONG)
            }

            SerialPrimitiveKind.FLOAT -> {
                PrimitiveNode(PrimitiveKind.FLOAT)
            }

            SerialPrimitiveKind.DOUBLE -> {
                PrimitiveNode(PrimitiveKind.DOUBLE)
            }

            SerialPrimitiveKind.CHAR -> {
                PrimitiveNode(PrimitiveKind.STRING)
            }

            else -> {
                null
            }
        }

    /**
     * Handles enum types by creating an [EnumNode] with entries extracted from descriptor elements.
     */
    private fun handleEnumType(
        descriptor: SerialDescriptor,
        nullable: Boolean,
    ): TypeRef {
        val id = descriptorId(descriptor)

        withCycleDetection(descriptor, id) {
            val entries = (0 until descriptor.elementsCount).map { descriptor.getElementName(it) }
            EnumNode(
                name = descriptor.serialName,
                entries = entries,
                description = extractDescription(descriptor),
            )
        }

        val ref = TypeRef.Ref(id, nullable)
        if (!nullable) typeRefCache[descriptor] = ref
        return ref
    }

    /**
     * Handles object/class types by creating an [ObjectNode] with properties.
     */
    private fun handleObjectType(
        descriptor: SerialDescriptor,
        nullable: Boolean,
    ): TypeRef {
        val id = descriptorId(descriptor)

        withCycleDetection(descriptor, id) {
            val properties = mutableListOf<Property>()
            val required = mutableSetOf<String>()

            for (i in 0 until descriptor.elementsCount) {
                val name = descriptor.getElementName(i)
                val elementDescriptor = descriptor.getElementDescriptor(i)
                val elementDescription = extractElementDescription(descriptor, i)
                val typeRef = toRef(elementDescriptor)
                val hasDefault = descriptor.isElementOptional(i)

                if (!hasDefault) {
                    required.add(name)
                }

                properties.add(
                    Property(
                        name = name,
                        type = typeRef,
                        description = elementDescription,
                        hasDefaultValue = hasDefault,
                    ),
                )
            }

            ObjectNode(
                name = descriptor.serialName,
                properties = properties,
                required = required,
                description = extractDescription(descriptor),
            )
        }

        val ref = TypeRef.Ref(id, nullable)
        if (!nullable) typeRefCache[descriptor] = ref
        return ref
    }

    /**
     * Handles list types by creating an inline [ListNode].
     */
    private fun handleListType(
        descriptor: SerialDescriptor,
        nullable: Boolean,
    ): TypeRef {
        val elementDescriptor = descriptor.getElementDescriptor(0)
        val elementRef = toRef(elementDescriptor)
        val node = ListNode(element = elementRef)
        val ref = TypeRef.Inline(node, nullable)
        if (!nullable) typeRefCache[descriptor] = ref
        return ref
    }

    /**
     * Handles map types by creating an inline [MapNode].
     */
    private fun handleMapType(
        descriptor: SerialDescriptor,
        nullable: Boolean,
    ): TypeRef {
        val keyDescriptor = descriptor.getElementDescriptor(0)
        val valueDescriptor = descriptor.getElementDescriptor(1)
        val keyRef = toRef(keyDescriptor)
        val valueRef = toRef(valueDescriptor)
        val node = MapNode(key = keyRef, value = valueRef)
        val ref = TypeRef.Inline(node, nullable)
        if (!nullable) typeRefCache[descriptor] = ref
        return ref
    }

    /**
     * Handles polymorphic types (sealed classes) by creating a [PolymorphicNode].
     *
     * For sealed classes, the descriptor structure is:
     * - descriptor.kind is PolymorphicKind.SEALED
     * - descriptor.elementDescriptors[0] is the discriminator descriptor (name "klass" or similar)
     * - descriptor.elementDescriptors[1] is the "value" descriptor containing subtypes
     * - The "value" descriptor's elementDescriptors are the actual subtype descriptors
     */
    private fun handlePolymorphicType(
        descriptor: SerialDescriptor,
        nullable: Boolean,
    ): TypeRef {
        val id = descriptorId(descriptor)

        withCycleDetection(descriptor, id) {
            // Extract subtypes from the nested structure
            val subtypeDescriptors = extractPolymorphicSubtypes(descriptor)
            val subtypes = subtypeDescriptors.map { SubtypeRef(TypeId(it.serialName)) }

            // Get discriminator configuration from Json
            val mode = json.configuration.classDiscriminatorMode
            val discriminatorName = json.configuration.classDiscriminator
            val discriminatorRequired =
                mode in
                    listOf(
                        ClassDiscriminatorMode.ALL_JSON_OBJECTS,
                        ClassDiscriminatorMode.POLYMORPHIC,
                    )

            val discriminator =
                Discriminator(
                    name = discriminatorName,
                    required = discriminatorRequired,
                    mapping = null, // Mapping is typically derived from serialName
                )

            // Create the polymorphic node
            val node =
                PolymorphicNode(
                    baseName = descriptor.serialName,
                    subtypes = subtypes,
                    discriminator = discriminator,
                    description = extractDescription(descriptor),
                )

            // Recursively process each subtype to discover their ObjectNodes
            subtypeDescriptors.forEach { toRef(it) }

            node
        }

        val ref = TypeRef.Ref(id, nullable)
        if (!nullable) typeRefCache[descriptor] = ref
        return ref
    }

    /**
     * Extracts subtype descriptors from a sealed polymorphic descriptor.
     *
     * The structure of a sealed class descriptor is:
     * ```
     * SerialDescriptor (PolymorphicKind.SEALED)
     *   ├─ element[0] → "klass" discriminator descriptor
     *   └─ element[1] → "value" descriptor containing subtypes
     *        └─ elements → [subtype1, subtype2, ...]
     * ```
     */
    private fun extractPolymorphicSubtypes(descriptor: SerialDescriptor): List<SerialDescriptor> {
        require(descriptor.kind is PolymorphicKind.SEALED) {
            "Expected sealed polymorphic descriptor, got ${descriptor.kind}"
        }

        require(descriptor.elementsCount >= 2 && descriptor.getElementName(1) == "value") {
            "Unexpected sealed descriptor structure: expected 'value' element at index 1, " +
                "but found '${descriptor.getElementName(1)}'"
        }

        val valueDescriptor = descriptor.getElementDescriptor(1)
        return (0 until valueDescriptor.elementsCount).map { valueDescriptor.getElementDescriptor(it) }
    }

    /**
     * Handles unknown types by creating an empty [ObjectNode].
     */
    private fun handleUnknownType(
        descriptor: SerialDescriptor,
        nullable: Boolean,
    ): TypeRef {
        val id = descriptorId(descriptor)

        withCycleDetection(descriptor, id) {
            ObjectNode(
                name = descriptor.serialName,
                properties = emptyList(),
                required = emptySet(),
                description = extractDescription(descriptor),
            )
        }

        val ref = TypeRef.Ref(id, nullable)
        if (!nullable) typeRefCache[descriptor] = ref
        return ref
    }

    /**
     * Creates a [TypeId] from a [SerialDescriptor] using its serialName.
     */
    private fun descriptorId(descriptor: SerialDescriptor): TypeId = TypeId(descriptor.serialName)

    /**
     * Extracts the @Description annotation value from a descriptor's annotations.
     */
    private fun extractDescription(descriptor: SerialDescriptor): String? =
        descriptor.annotations
            .filterIsInstance<Description>()
            .firstOrNull()
            ?.value

    /**
     * Extracts the @Description annotation value from a descriptor element's annotations.
     */
    private fun extractElementDescription(
        descriptor: SerialDescriptor,
        index: Int,
    ): String? =
        descriptor
            .getElementAnnotations(index)
            .filterIsInstance<Description>()
            .firstOrNull()
            ?.value

    /**
     * Returns a new [TypeRef] with the specified nullable flag.
     */
    private fun TypeRef.withNullable(nullable: Boolean): TypeRef =
        when (this) {
            is TypeRef.Inline -> copy(nullable = nullable)
            is TypeRef.Ref -> copy(nullable = nullable)
        }
}
