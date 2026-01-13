package kotlinx.schema.generator.json

import kotlinx.schema.generator.core.ir.EnumNode
import kotlinx.schema.generator.core.ir.ListNode
import kotlinx.schema.generator.core.ir.MapNode
import kotlinx.schema.generator.core.ir.ObjectNode
import kotlinx.schema.generator.core.ir.PrimitiveKind
import kotlinx.schema.generator.core.ir.PrimitiveNode
import kotlinx.schema.generator.core.ir.TypeGraph
import kotlinx.schema.generator.core.ir.TypeGraphTransformer
import kotlinx.schema.generator.core.ir.TypeNode
import kotlinx.schema.generator.core.ir.TypeRef
import kotlinx.schema.json.ArrayPropertyDefinition
import kotlinx.schema.json.BooleanPropertyDefinition
import kotlinx.schema.json.FunctionCallingSchema
import kotlinx.schema.json.NumericPropertyDefinition
import kotlinx.schema.json.ObjectPropertyDefinition
import kotlinx.schema.json.PropertyDefinition
import kotlinx.schema.json.StringPropertyDefinition
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.encodeToJsonElement

/**
 * Transforms a [TypeGraph] into a [FunctionCallingSchema] for tool/function schema representation.
 *
 * This transformer converts the IR representation of a function's parameters
 * into a tool schema suitable for LLM function calling APIs.
 *
 * All fields are marked as required in the schema. Nullable/optional fields are represented
 * using union types that include "null" (e.g., ["string", "null"]) instead of using the "nullable" flag.
 *
 * See
 */
@Suppress("TooManyFunctions")
public class TypeGraphToFunctionCallingSchemaTransformer
    @JvmOverloads
    public constructor(
        private val json: Json = Json { encodeDefaults = false },
    ) : TypeGraphTransformer<FunctionCallingSchema> {
        override fun transform(
            graph: TypeGraph,
            rootName: String,
        ): FunctionCallingSchema {
            val rootRef = graph.root

            return when (rootRef) {
                is TypeRef.Ref -> {
                    val node =
                        graph.nodes[rootRef.id]
                            ?: error(
                                "Type reference '${rootRef.id.value}' not found in type graph. " +
                                    "This indicates a bug in the introspector.",
                            )

                    when (node) {
                        is ObjectNode -> convertObjectNodeToToolSchema(node, graph)

                        else -> throw IllegalArgumentException(
                            "Root node must be ObjectNode for tool schema, got: ${node::class.simpleName}",
                        )
                    }
                }

                is TypeRef.Inline -> {
                    throw IllegalArgumentException(
                        "Root cannot be inline for tool schema. Expected ObjectNode reference.",
                    )
                }
            }
        }

        private fun convertObjectNodeToToolSchema(
            node: ObjectNode,
            graph: TypeGraph,
        ): FunctionCallingSchema {
            val properties =
                node.properties.associate { property ->
                    val finalDef =
                        convertTypeRef(property.type, graph)
                            .let { def ->
                                property.description?.let { setDescription(def, it) } ?: def
                            }.let { def ->
                                property.defaultValue?.let { setDefaultValue(def, it) } ?: def
                            }

                    property.name to finalDef
                }

            // All properties must be required for OpenAI structured outputs compatibility
            return FunctionCallingSchema(
                name = node.name,
                description = node.description ?: "",
                parameters =
                    ObjectPropertyDefinition(
                        properties = properties,
                        required = node.properties.map { it.name },
                        additionalProperties = JsonPrimitive(false),
                    ),
            )
        }

        private fun convertTypeRef(
            typeRef: TypeRef,
            graph: TypeGraph,
        ): PropertyDefinition =
            when (typeRef) {
                is TypeRef.Inline -> {
                    convertInlineNode(typeRef.node, typeRef.nullable, graph)
                }

                is TypeRef.Ref -> {
                    val node =
                        graph.nodes[typeRef.id]
                            ?: error(
                                "Type reference '${typeRef.id.value}' not found in type graph.",
                            )
                    convertNode(node, typeRef.nullable, graph)
                }
            }

        private fun convertInlineNode(
            node: TypeNode,
            nullable: Boolean,
            graph: TypeGraph,
        ): PropertyDefinition =
            when (node) {
                is PrimitiveNode -> {
                    convertPrimitive(node, nullable)
                }

                is ListNode -> {
                    convertList(node, nullable, graph)
                }

                is MapNode -> {
                    convertMap(node, nullable, graph)
                }

                else -> {
                    throw IllegalArgumentException(
                        "Unsupported inline node type: ${node::class.simpleName}. " +
                            "Only PrimitiveNode, ListNode, and MapNode can be inlined.",
                    )
                }
            }

        private fun convertNode(
            node: TypeNode,
            nullable: Boolean,
            graph: TypeGraph,
        ): PropertyDefinition =
            when (node) {
                is PrimitiveNode -> {
                    convertPrimitive(node, nullable)
                }

                is ObjectNode -> {
                    convertObject(node, nullable, graph)
                }

                is EnumNode -> {
                    convertEnum(node, nullable)
                }

                is ListNode -> {
                    convertList(node, nullable, graph)
                }

                is MapNode -> {
                    convertMap(node, nullable, graph)
                }

                else -> {
                    throw IllegalArgumentException(
                        "Unsupported node type: ${node::class.simpleName}.",
                    )
                }
            }

        private fun convertPrimitive(
            node: PrimitiveNode,
            nullable: Boolean,
        ): PropertyDefinition =
            when (node.kind) {
                PrimitiveKind.STRING -> {
                    StringPropertyDefinition(
                        type = if (nullable) listOf("string", "null") else listOf("string"),
                        description = node.description,
                        nullable = null,
                    )
                }

                PrimitiveKind.BOOLEAN -> {
                    BooleanPropertyDefinition(
                        type = if (nullable) listOf("boolean", "null") else listOf("boolean"),
                        description = node.description,
                        nullable = null,
                    )
                }

                PrimitiveKind.INT -> {
                    NumericPropertyDefinition(
                        type = if (nullable) listOf("integer", "null") else listOf("integer"),
                        description = node.description,
                        nullable = null,
                    )
                }

                PrimitiveKind.LONG -> {
                    NumericPropertyDefinition(
                        type = if (nullable) listOf("integer", "null") else listOf("integer"),
                        description = node.description,
                        nullable = null,
                    )
                }

                PrimitiveKind.FLOAT, PrimitiveKind.DOUBLE -> {
                    NumericPropertyDefinition(
                        type = if (nullable) listOf("number", "null") else listOf("number"),
                        description = node.description,
                        nullable = null,
                    )
                }
            }

        private fun convertObject(
            node: ObjectNode,
            nullable: Boolean,
            graph: TypeGraph,
        ): PropertyDefinition {
            val properties =
                node.properties.associate { property ->
                    val finalDef =
                        convertTypeRef(property.type, graph)
                            .let { def ->
                                property.description?.let { setDescription(def, it) } ?: def
                            }.let { def ->
                                property.defaultValue?.let { setDefaultValue(def, it) } ?: def
                            }

                    property.name to finalDef
                }

            // All properties must be required for OpenAI structured outputs compatibility
            return ObjectPropertyDefinition(
                type = if (nullable) listOf("object", "null") else listOf("object"),
                description = node.description,
                nullable = null,
                properties = properties,
                required = node.properties.map { it.name },
                additionalProperties = JsonPrimitive(false),
            )
        }

        private fun convertEnum(
            node: EnumNode,
            nullable: Boolean,
        ): PropertyDefinition =
            StringPropertyDefinition(
                type = if (nullable) listOf("string", "null") else listOf("string"),
                description = node.description,
                nullable = null,
                enum = node.entries,
            )

        private fun convertList(
            node: ListNode,
            nullable: Boolean,
            graph: TypeGraph,
        ): PropertyDefinition {
            val items = convertTypeRef(node.element, graph)
            return ArrayPropertyDefinition(
                type = if (nullable) listOf("array", "null") else listOf("array"),
                description = node.description,
                nullable = null,
                items = items,
            )
        }

        private fun convertMap(
            node: MapNode,
            nullable: Boolean,
            graph: TypeGraph,
        ): PropertyDefinition {
            val valuePropertyDef = convertTypeRef(node.value, graph)
            val additionalPropertiesSchema = json.encodeToJsonElement(valuePropertyDef)

            return ObjectPropertyDefinition(
                type = if (nullable) listOf("object", "null") else listOf("object"),
                description = node.description,
                nullable = null,
                additionalProperties = additionalPropertiesSchema,
            )
        }
    }
