package kotlinx.schema.generator.json

import kotlinx.schema.generator.core.ir.AbstractTypeGraphTransformer
import kotlinx.schema.generator.core.ir.EnumNode
import kotlinx.schema.generator.core.ir.ListNode
import kotlinx.schema.generator.core.ir.MapNode
import kotlinx.schema.generator.core.ir.ObjectNode
import kotlinx.schema.generator.core.ir.PrimitiveKind
import kotlinx.schema.generator.core.ir.PrimitiveNode
import kotlinx.schema.generator.core.ir.TypeGraph
import kotlinx.schema.generator.core.ir.TypeNode
import kotlinx.schema.generator.core.ir.TypeRef
import kotlinx.schema.json.ArrayPropertyDefinition
import kotlinx.schema.json.BooleanPropertyDefinition
import kotlinx.schema.json.FunctionCallingSchema
import kotlinx.schema.json.JsonSchemaConstants.Types.ARRAY_OR_NULL_TYPE
import kotlinx.schema.json.JsonSchemaConstants.Types.ARRAY_TYPE
import kotlinx.schema.json.JsonSchemaConstants.Types.BOOLEAN_OR_NULL_TYPE
import kotlinx.schema.json.JsonSchemaConstants.Types.BOOLEAN_TYPE
import kotlinx.schema.json.JsonSchemaConstants.Types.INTEGER_OR_NULL_TYPE
import kotlinx.schema.json.JsonSchemaConstants.Types.INTEGER_TYPE
import kotlinx.schema.json.JsonSchemaConstants.Types.NUMBER_OR_NULL_TYPE
import kotlinx.schema.json.JsonSchemaConstants.Types.NUMBER_TYPE
import kotlinx.schema.json.JsonSchemaConstants.Types.OBJECT_OR_NULL_TYPE
import kotlinx.schema.json.JsonSchemaConstants.Types.OBJECT_TYPE
import kotlinx.schema.json.JsonSchemaConstants.Types.STRING_OR_NULL_TYPE
import kotlinx.schema.json.JsonSchemaConstants.Types.STRING_TYPE
import kotlinx.schema.json.NumericPropertyDefinition
import kotlinx.schema.json.ObjectPropertyDefinition
import kotlinx.schema.json.PropertyDefinition
import kotlinx.schema.json.StringPropertyDefinition
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.schema.generator.json.FunctionCallingSchemaConfig.Companion.Default as DefaultConfig

/**
 * Transforms a [TypeGraph] into a [FunctionCallingSchema] for tool/function schema representation.
 *
 * This transformer converts the IR representation of a function's parameters
 * into a tool schema suitable for LLM function calling APIs.
 *
 * ## Flat Schema Structure (Default)
 *
 * Function schemas use a **FLAT/INLINED structure** by default (`useDefsAndRefs = false`):
 * - Complex types are inlined at their point of use
 * - No `$defs` section or `$ref` references
 * - All type information embedded directly in parameters
 *
 * This design optimizes for:
 * - LLM parsing simplicity (no reference resolution)
 * - Self-contained schemas (single parameter object)
 * - API compatibility (OpenAI, Anthropic, etc.)
 *
 * Contrast with [TypeGraphToJsonSchemaTransformer] which can use `$defs`/`$ref` for type reuse.
 *
 * ## Nullable Types
 *
 * Nullable/optional fields are represented using union types that include "null"
 * (e.g., ["string", "null"]) instead of using the "nullable" flag.
 */
@Suppress("TooManyFunctions")
public class TypeGraphToFunctionCallingSchemaTransformer
    @JvmOverloads
    public constructor(
        private val json: Json = Json { encodeDefaults = false },
        public override val config: FunctionCallingSchemaConfig = DefaultConfig,
    ) : AbstractTypeGraphTransformer<
            FunctionCallingSchema,
            FunctionCallingSchemaConfig,
        >(config = config) {
        public override fun transform(
            graph: TypeGraph,
            rootName: String,
        ): FunctionCallingSchema =
            when (val rootRef = graph.root) {
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

            val requiredFields =
                if (config.respectDefaultPresence) {
                    // Use the required set from the ObjectNode (respects DefaultPresence)
                    node.required.toList()
                } else if (config.requireNullableFields) {
                    // All properties are required (strict mode)
                    node.properties.map { it.name }
                } else {
                    // Only non-nullable properties are required
                    node.properties.filter { !it.type.nullable }.map { it.name }
                }

            return FunctionCallingSchema(
                name = node.name,
                description = node.description ?: "",
                strict = if (config.strictMode) true else null,
                parameters =
                    ObjectPropertyDefinition(
                        properties = properties,
                        required = requiredFields,
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
                        type = if (nullable) STRING_OR_NULL_TYPE else STRING_TYPE,
                        description = node.description,
                        nullable = null,
                    )
                }

                PrimitiveKind.BOOLEAN -> {
                    BooleanPropertyDefinition(
                        type = if (nullable) BOOLEAN_OR_NULL_TYPE else BOOLEAN_TYPE,
                        description = node.description,
                        nullable = null,
                    )
                }

                PrimitiveKind.INT, PrimitiveKind.LONG -> {
                    NumericPropertyDefinition(
                        type = if (nullable) INTEGER_OR_NULL_TYPE else INTEGER_TYPE,
                        description = node.description,
                        nullable = null,
                    )
                }

                PrimitiveKind.FLOAT, PrimitiveKind.DOUBLE -> {
                    NumericPropertyDefinition(
                        type = if (nullable) NUMBER_OR_NULL_TYPE else NUMBER_TYPE,
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

            val requiredFields =
                if (config.respectDefaultPresence) {
                    // Use the required set from the ObjectNode (respects DefaultPresence)
                    node.required.toList()
                } else if (config.requireNullableFields) {
                    // All properties are required (strict mode)
                    node.properties.map { it.name }
                } else {
                    // Only non-nullable properties are required
                    node.properties.filter { !it.type.nullable }.map { it.name }
                }

            return ObjectPropertyDefinition(
                type = if (nullable) OBJECT_OR_NULL_TYPE else OBJECT_TYPE,
                description = node.description,
                nullable = null,
                properties = properties,
                required = requiredFields,
                additionalProperties = JsonPrimitive(false),
            )
        }

        private fun convertEnum(
            node: EnumNode,
            nullable: Boolean,
        ): PropertyDefinition =
            StringPropertyDefinition(
                type = if (nullable) STRING_OR_NULL_TYPE else STRING_TYPE,
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
                type = if (nullable) ARRAY_OR_NULL_TYPE else ARRAY_TYPE,
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
                type = if (nullable) OBJECT_OR_NULL_TYPE else OBJECT_TYPE,
                description = node.description,
                nullable = null,
                additionalProperties = additionalPropertiesSchema,
            )
        }
    }
