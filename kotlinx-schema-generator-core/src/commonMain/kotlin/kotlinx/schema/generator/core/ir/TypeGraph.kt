package kotlinx.schema.generator.core.ir

/** A graph of discovered types plus the root type reference used to emit schemas. */
public data class TypeGraph(
    val root: TypeRef,
    val nodes: Map<TypeId, TypeNode>,
)

/** A stable identifier for a type definition used for deduplication and $ref linking. */
public data class TypeId(
    val value: String,
) {
    override fun toString(): String = value
}

/** Reference to a type: either inline node or reference by [TypeId]. */
public sealed interface TypeRef {
    public val nullable: Boolean

    /** Inline node reference (anonymous, not addressable by $ref). */
    public data class Inline(
        val node: TypeNode,
        override val nullable: Boolean = false,
    ) : TypeRef

    /** Reference a named/type-def node by its [TypeId]. */
    public data class Ref(
        val id: TypeId,
        override val nullable: Boolean = false,
    ) : TypeRef
}

/** Base node for all kinds supported by the schema IR. */
public sealed interface TypeNode {
    public val description: String?
}

/** Primitive kinds supported by the IR. */
public enum class PrimitiveKind { STRING, BOOLEAN, INT, LONG, FLOAT, DOUBLE }

/** Primitive node. */
public data class PrimitiveNode(
    val kind: PrimitiveKind,
    override val description: String? = null,
) : TypeNode

/** Enum node with symbolic entries. */
public data class EnumNode(
    val name: String,
    val entries: List<String>,
    override val description: String? = null,
) : TypeNode

/** Object node with named properties and required set. */
public data class ObjectNode(
    val name: String,
    val properties: List<Property>,
    val required: Set<String>,
    override val description: String? = null,
) : TypeNode

/** List/array node. */
public data class ListNode(
    val element: TypeRef,
    override val description: String? = null,
) : TypeNode

/** Map/dictionary node. */
public data class MapNode(
    val key: TypeRef,
    val value: TypeRef,
    override val description: String? = null,
) : TypeNode

/** Polymorphic node for sealed/open hierarchies. */
public data class PolymorphicNode(
    val baseName: String,
    val subtypes: List<SubtypeRef>,
    val discriminator: Discriminator?,
    override val description: String? = null,
) : TypeNode

/** Property of an object. */
public data class Property(
    val name: String,
    val type: TypeRef,
    val description: String? = null,
    val deprecated: Boolean = false,
    val hasDefaultValue: Boolean = false,
    val defaultValue: Any? = null,
    val annotations: Map<String, String?> = emptyMap(),
)

/** Reference to a subtype in a polymorphic hierarchy. */
public data class SubtypeRef(
    val id: TypeId,
    val ref: TypeRef.Ref = TypeRef.Ref(id),
)

/**
 * Class discriminator information. If [mapping] is null, default implicit mapping is assumed
 * (typically discriminator value equals subtype serial name).
 */
public data class Discriminator(
    val name: String = "type",
    val required: Boolean,
    val mapping: Map<String, TypeId>? = null,
)

public fun TypeGraph.rootObject(): ObjectNode? {
    val inline = root as? TypeRef.Inline ?: return null
    return inline.node as? ObjectNode
}

public fun TypeGraph.rootProperty(name: String): Property? {
    return rootObject()
        ?.properties
        ?.firstOrNull { it.name == name }
}
