package kotlinx.schema.generator.json

import kotlinx.schema.Description
import kotlinx.schema.generator.core.ir.DefaultPresence
import kotlinx.schema.generator.core.ir.Discriminator
import kotlinx.schema.generator.core.ir.EnumNode
import kotlinx.schema.generator.core.ir.ListNode
import kotlinx.schema.generator.core.ir.MapNode
import kotlinx.schema.generator.core.ir.ObjectNode
import kotlinx.schema.generator.core.ir.PolymorphicNode
import kotlinx.schema.generator.core.ir.PrimitiveNode
import kotlinx.schema.generator.core.ir.Property
import kotlinx.schema.generator.core.ir.SchemaIntrospector
import kotlinx.schema.generator.core.ir.SubtypeRef
import kotlinx.schema.generator.core.ir.TypeGraph
import kotlinx.schema.generator.core.ir.TypeId
import kotlinx.schema.generator.core.ir.TypeNode
import kotlinx.schema.generator.core.ir.TypeRef
import kotlinx.schema.generator.json.internal.getPolymorphicDescriptors
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PolymorphicKind
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.SerialKind
import kotlinx.serialization.descriptors.StructureKind
import kotlinx.serialization.json.ClassDiscriminatorMode
import kotlinx.serialization.json.Json

/**
 * Introspects kotlinx.serialization descriptors into Schema IR.
 */
public class SerializationIntrospector(
    private val json: Json,
) : SchemaIntrospector<KSerializer<*>> {
    @Suppress("CyclomaticComplexMethod", "NestedBlockDepth")
    override fun introspect(root: KSerializer<*>): TypeGraph {
        val nodes = LinkedHashMap<TypeId, TypeNode>()
        val visiting = HashSet<SerialDescriptor>()
        val cache = HashMap<SerialDescriptor, TypeRef>()

        fun descriptorId(d: SerialDescriptor): TypeId = TypeId(d.serialName)

        fun primitiveFor(d: SerialDescriptor): PrimitiveNode? =
            when (d.kind) {
                PrimitiveKind.STRING -> PrimitiveNode(kotlinx.schema.generator.core.ir.PrimitiveKind.STRING)
                PrimitiveKind.BOOLEAN -> PrimitiveNode(kotlinx.schema.generator.core.ir.PrimitiveKind.BOOLEAN)
                PrimitiveKind.BYTE, PrimitiveKind.SHORT, PrimitiveKind.INT ->
                    PrimitiveNode(
                        kotlinx.schema.generator.core.ir.PrimitiveKind.INT,
                    )

                PrimitiveKind.LONG -> PrimitiveNode(kotlinx.schema.generator.core.ir.PrimitiveKind.LONG)
                PrimitiveKind.FLOAT -> PrimitiveNode(kotlinx.schema.generator.core.ir.PrimitiveKind.FLOAT)
                PrimitiveKind.DOUBLE -> PrimitiveNode(kotlinx.schema.generator.core.ir.PrimitiveKind.DOUBLE)
                PrimitiveKind.CHAR -> PrimitiveNode(kotlinx.schema.generator.core.ir.PrimitiveKind.STRING)
                else -> null
            }

        fun SerialDescriptor.readDescription(): String? =
            annotations.filterIsInstance<Description>().firstOrNull()?.value

        fun elementDescription(
            parent: SerialDescriptor,
            index: Int,
        ): String? =
            parent
                .getElementAnnotations(index)
                .filterIsInstance<Description>()
                .firstOrNull()
                ?.value

        @Suppress("LongMethod", "ReturnCount")
        fun toRef(d: SerialDescriptor): TypeRef {
            cache[d]?.let { return it }

            val nullable = d.isNullable

            // Primitives inline
            primitiveFor(d)?.let { prim ->
                val ref = TypeRef.Inline(prim, nullable)
                cache[d] = ref
                return ref
            }

            return when (d.kind) {
                is SerialKind.ENUM -> {
                    val id = descriptorId(d)
                    if (id !in nodes.keys && d !in visiting) {
                        visiting += d
                        val entries = (0 until d.elementsCount).map { d.getElementName(it) }
                        val node =
                            EnumNode(
                                name = d.serialName,
                                entries = entries,
                                description = d.readDescription(),
                            )
                        nodes[id] = node
                        visiting -= d
                    }
                    val ref = TypeRef.Ref(id, nullable)
                    cache[d] = ref
                    ref
                }

                is StructureKind.CLASS, StructureKind.OBJECT, StructureKind.MAP -> {
                    // Map is treated as object with additionalProperties semantics in emitters; keep as MapNode inline
                    if (d.kind == StructureKind.MAP) {
                        val keyDesc = d.getElementDescriptor(0)
                        val valDesc = d.getElementDescriptor(1)
                        val node =
                            MapNode(key = toRef(keyDesc), value = toRef(valDesc))
                        val ref = TypeRef.Inline(node, nullable)
                        cache[d] = ref
                        ref
                    } else {
                        val id = descriptorId(d)
                        if (id !in nodes.keys && d !in visiting) {
                            visiting += d
                            val props = ArrayList<Property>()
                            val required = LinkedHashSet<String>()
                            for (i in 0 until d.elementsCount) {
                                val name = d.getElementName(i)
                                val ed = d.getElementDescriptor(i)
                                val pDesc = elementDescription(d, i)
                                val typeRef = toRef(ed)
                                val hasDefault = d.isElementOptional(i)
                                val presence = if (hasDefault) DefaultPresence.Absent else DefaultPresence.Required
                                if (!hasDefault) required += name
                                props +=
                                    Property(
                                        name = name,
                                        type = typeRef,
                                        description = pDesc,
                                        defaultPresence = presence,
                                    )
                            }
                            val node =
                                ObjectNode(
                                    name = d.serialName,
                                    properties = props,
                                    required = required,
                                    description = d.readDescription(),
                                )
                            nodes[id] = node
                            visiting -= d
                        }
                        val ref = TypeRef.Ref(id, nullable)
                        cache[d] = ref
                        ref
                    }
                }

                is StructureKind.LIST -> {
                    val elem = d.getElementDescriptor(0)
                    val node = ListNode(element = toRef(elem))
                    val ref = TypeRef.Inline(node, nullable)
                    cache[d] = ref
                    ref
                }

                is PolymorphicKind -> {
                    val id = descriptorId(d)
                    if (id !in nodes.keys && d !in visiting) {
                        visiting += d
                        val subDescs = d.getPolymorphicDescriptors(json)
                        val subtypes = subDescs.map { sd -> SubtypeRef(TypeId(sd.serialName)) }
                        val mode = json.configuration.classDiscriminatorMode
                        val discName = json.configuration.classDiscriminator
                        val required =
                            mode in listOf(ClassDiscriminatorMode.ALL_JSON_OBJECTS, ClassDiscriminatorMode.POLYMORPHIC)
                        val disc = Discriminator(name = discName, required = required, mapping = null)
                        val node =
                            PolymorphicNode(
                                baseName = d.serialName,
                                subtypes = subtypes,
                                discriminator = disc,
                                description = d.readDescription(),
                            )
                        nodes[id] = node
                        // also ensure each subtype object node is discovered
                        subDescs.forEach { sd -> toRef(sd) }
                        visiting -= d
                    }
                    val ref = TypeRef.Ref(id, nullable)
                    cache[d] = ref
                    ref
                }

                else -> {
                    // Fallback: treat unknown structured kinds as object without properties
                    val id = descriptorId(d)
                    if (id !in nodes.keys) {
                        nodes[id] =
                            ObjectNode(
                                name = d.serialName,
                                properties = emptyList(),
                                required = emptySet(),
                                description = d.readDescription(),
                            )
                    }
                    val ref = TypeRef.Ref(id, nullable)
                    cache[d] = ref
                    ref
                }
            }
        }

        val rootRef = toRef(root.descriptor)
        return TypeGraph(root = rootRef, nodes = nodes)
    }
}
