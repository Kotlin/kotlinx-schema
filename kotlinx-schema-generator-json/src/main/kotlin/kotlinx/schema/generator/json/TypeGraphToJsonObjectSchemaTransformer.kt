package kotlinx.schema.generator.json

import kotlinx.schema.generator.core.ir.AbstractTypeGraphTransformer
import kotlinx.schema.generator.core.ir.EnumNode
import kotlinx.schema.generator.core.ir.ListNode
import kotlinx.schema.generator.core.ir.MapNode
import kotlinx.schema.generator.core.ir.ObjectNode
import kotlinx.schema.generator.core.ir.PolymorphicNode
import kotlinx.schema.generator.core.ir.PrimitiveKind
import kotlinx.schema.generator.core.ir.PrimitiveNode
import kotlinx.schema.generator.core.ir.TypeGraph
import kotlinx.schema.generator.core.ir.TypeId
import kotlinx.schema.generator.core.ir.TypeNode
import kotlinx.schema.generator.core.ir.TypeRef
import kotlinx.schema.generator.json.internal.JsonSchemaConsts
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/** Emits JSON Schema from Schema IR following the Standard rules used previously. */
public class TypeGraphToJsonObjectSchemaTransformer :
    AbstractTypeGraphTransformer<JsonObject, JsonSchemaConfig>(
        config = JsonSchemaConfig.Default,
    ) {
    @Suppress("CyclomaticComplexMethod", "LongMethod", "NestedBlockDepth")
    override fun transform(
        graph: TypeGraph,
        rootName: String,
    ): JsonObject {
        // Build $defs for named nodes
        val defs: MutableMap<String, JsonObject> = LinkedHashMap()

        // Helpers for nullability
        fun JsonObject.withNullableTypeUnion(): JsonObject {
            val m = this.toMutableMap()
            val t = m[JsonSchemaConsts.Keys.TYPE]
            if (t is JsonPrimitive) {
                m[JsonSchemaConsts.Keys.TYPE] = JsonArray(listOf(t, JsonPrimitive(JsonSchemaConsts.Types.NULL)))
            } else if (m.containsKey(JsonSchemaConsts.Keys.ONE_OF)) {
                val arr = (m[JsonSchemaConsts.Keys.ONE_OF] as? JsonArray)?.toMutableList() ?: mutableListOf()
                arr.add(buildJsonObject { put(JsonSchemaConsts.Keys.TYPE, JsonPrimitive(JsonSchemaConsts.Types.NULL)) })
                m[JsonSchemaConsts.Keys.ONE_OF] = JsonArray(arr)
            }
            return JsonObject(m)
        }

        fun wrapNullableType(base: JsonObject): JsonObject = base.withNullableTypeUnion()

        // Declare mutually-recursive lambdas
        lateinit var emitRef: (TypeRef) -> JsonObject
        lateinit var emitNode: (TypeId, TypeNode) -> JsonObject

        fun primitiveWithNullability(
            n: PrimitiveNode,
            nullable: Boolean,
        ): JsonObject {
            val base = emitNode(TypeId("primitive:${n.kind}"), n)
            return if (!nullable) base else base.withNullableTypeUnion()
        }

        fun enumWithNullability(
            n: EnumNode,
            nullable: Boolean,
        ): JsonObject {
            val base = emitNode(TypeId(n.name), n)
            return if (!nullable) base else base.withNullableTypeUnion()
        }

        fun listWithNullability(
            n: ListNode,
            nullable: Boolean,
        ): JsonObject {
            val base = emitNode(TypeId("list"), n)
            return if (!nullable) base else base.withNullableTypeUnion()
        }

        fun mapWithNullability(
            n: MapNode,
            nullable: Boolean,
        ): JsonObject {
            val base = emitNode(TypeId("map"), n)
            return if (!nullable) base else base.withNullableTypeUnion()
        }

        emitNode = { _, node ->
            when (node) {
                is PrimitiveNode -> {
                    buildJsonObject {
                        put(
                            JsonSchemaConsts.Keys.TYPE,
                            when (node.kind) {
                                PrimitiveKind.STRING -> JsonSchemaConsts.Types.STRING
                                PrimitiveKind.BOOLEAN -> JsonSchemaConsts.Types.BOOLEAN
                                PrimitiveKind.INT -> JsonSchemaConsts.Types.INTEGER
                                PrimitiveKind.LONG -> JsonSchemaConsts.Types.INTEGER
                                PrimitiveKind.FLOAT, PrimitiveKind.DOUBLE -> JsonSchemaConsts.Types.NUMBER
                            },
                        )
                        node.description?.let { put(JsonSchemaConsts.Keys.DESCRIPTION, it) }
                    }
                }

                is EnumNode -> {
                    buildJsonObject {
                        put(JsonSchemaConsts.Keys.TYPE, JsonSchemaConsts.Types.STRING)
                        put(JsonSchemaConsts.Keys.ENUM, JsonArray(node.entries.map { JsonPrimitive(it) }))
                        node.description?.let { put(JsonSchemaConsts.Keys.DESCRIPTION, it) }
                    }
                }

                is ObjectNode -> {
                    buildJsonObject {
                        put(JsonSchemaConsts.Keys.TYPE, JsonSchemaConsts.Types.OBJECT)
                        val props =
                            buildJsonObject {
                                for (p in node.properties) {
                                    val base = emitRef(p.type)
                                    // Merge property-level description into the property's schema,
                                    // overriding any inner description
                                    val merged =
                                        if (p.description != null) {
                                            val m = base.toMutableMap()
                                            m[JsonSchemaConsts.Keys.DESCRIPTION] = JsonPrimitive(p.description)
                                            JsonObject(m)
                                        } else {
                                            base
                                        }
                                    put(p.name, merged)
                                }
                            }
                        put(JsonSchemaConsts.Keys.PROPERTIES, props)
                        put(JsonSchemaConsts.Keys.REQUIRED, JsonArray(node.required.map { JsonPrimitive(it) }))
                        put(JsonSchemaConsts.Keys.ADDITIONAL_PROPERTIES, JsonPrimitive(false))
                        node.description?.let { put(JsonSchemaConsts.Keys.DESCRIPTION, it) }
                    }
                }

                is ListNode -> {
                    buildJsonObject {
                        put(JsonSchemaConsts.Keys.TYPE, JsonSchemaConsts.Types.ARRAY)
                        put(JsonSchemaConsts.Keys.ITEMS, emitRef(node.element))
                        node.description?.let { put(JsonSchemaConsts.Keys.DESCRIPTION, it) }
                    }
                }

                is MapNode -> {
                    buildJsonObject {
                        put(JsonSchemaConsts.Keys.TYPE, JsonSchemaConsts.Types.OBJECT)
                        put(JsonSchemaConsts.Keys.ADDITIONAL_PROPERTIES, emitRef(node.value))
                        node.description?.let { put(JsonSchemaConsts.Keys.DESCRIPTION, it) }
                    }
                }

                is PolymorphicNode -> {
                    buildJsonObject {
                        put(
                            JsonSchemaConsts.Keys.ONE_OF,
                            buildJsonArray {
                                node.subtypes.forEach { st ->
                                    add(
                                        buildJsonObject {
                                            put(
                                                JsonSchemaConsts.Keys.REF,
                                                "${JsonSchemaConsts.Keys.REF_PREFIX}${st.id}",
                                            )
                                        },
                                    )
                                }
                            },
                        )
                        node.description?.let { put(JsonSchemaConsts.Keys.DESCRIPTION, it) }
                    }
                }
            }
        }

        emitRef = { ref ->
            when (ref) {
                is TypeRef.Ref -> {
                    if (ref.nullable) {
                        buildJsonObject {
                            put(
                                JsonSchemaConsts.Keys.ONE_OF,
                                buildJsonArray {
                                    add(
                                        buildJsonObject {
                                            put(
                                                JsonSchemaConsts.Keys.REF,
                                                "${JsonSchemaConsts.Keys.REF_PREFIX}${ref.id}",
                                            )
                                        },
                                    )
                                    add(
                                        buildJsonObject {
                                            put(
                                                JsonSchemaConsts.Keys.TYPE,
                                                JsonPrimitive(JsonSchemaConsts.Types.NULL),
                                            )
                                        },
                                    )
                                },
                            )
                        }
                    } else {
                        buildJsonObject {
                            put(
                                JsonSchemaConsts.Keys.REF,
                                "${JsonSchemaConsts.Keys.REF_PREFIX}${ref.id}",
                            )
                        }
                    }
                }

                is TypeRef.Inline -> {
                    when (val n = ref.node) {
                        is PrimitiveNode -> {
                            primitiveWithNullability(n, ref.nullable)
                        }

                        is EnumNode -> {
                            enumWithNullability(n, ref.nullable)
                        }

                        is ListNode -> {
                            listWithNullability(n, ref.nullable)
                        }

                        is MapNode -> {
                            mapWithNullability(n, ref.nullable)
                        }

                        is ObjectNode -> {
                            val base = emitNode(TypeId(n.name), n)
                            if (ref.nullable) wrapNullableType(base) else base
                        }

                        is PolymorphicNode -> {
                            val base = emitNode(TypeId(n.baseName), n)
                            if (ref.nullable) {
                                buildJsonObject {
                                    put(
                                        JsonSchemaConsts.Keys.ONE_OF,
                                        buildJsonArray {
                                            add(base)
                                            add(
                                                buildJsonObject {
                                                    put(
                                                        JsonSchemaConsts.Keys.TYPE,
                                                        JsonPrimitive(JsonSchemaConsts.Types.NULL),
                                                    )
                                                },
                                            )
                                        },
                                    )
                                }
                            } else {
                                base
                            }
                        }
                    }
                }
            }
        }

        // Populate defs from graph.nodes
        graph.nodes.forEach { (id, node) ->
            defs[id.value] = emitNode(id, node)
        }

        // Polymorphic discriminator enrichment
        graph.nodes.values.filterIsInstance<PolymorphicNode>().forEach { poly ->
            val disc = poly.discriminator
            if (disc != null && disc.required) {
                poly.subtypes.forEach { st ->
                    val key = st.id.value
                    val existing = defs[key] ?: return@forEach
                    val schema = existing.toMutableMap()
                    val props =
                        (schema[JsonSchemaConsts.Keys.PROPERTIES] as? JsonObject)?.toMutableMap() ?: mutableMapOf()
                    props[disc.name] = buildJsonObject { put(JsonSchemaConsts.Keys.CONST, JsonPrimitive(st.id.value)) }
                    schema[JsonSchemaConsts.Keys.PROPERTIES] = JsonObject(props)
                    val required =
                        (schema[JsonSchemaConsts.Keys.REQUIRED] as? JsonArray)?.toMutableList() ?: mutableListOf()
                    if (required.none { it is JsonPrimitive && it.content == disc.name }) {
                        required.add(JsonPrimitive(disc.name))
                    }
                    schema[JsonSchemaConsts.Keys.REQUIRED] = JsonArray(required)
                    defs[key] = JsonObject(schema)
                }
            }
        }

        val rootSchema = emitRef(graph.root)
        return buildJsonObject {
            put(JsonSchemaConsts.Keys.ID, rootName)
            if (defs.isNotEmpty()) put(JsonSchemaConsts.Keys.DEFS, JsonObject(defs))
            rootSchema.entries.forEach { entry -> put(entry.key, entry.value) }
        }
    }
}
