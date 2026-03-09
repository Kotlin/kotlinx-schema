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
import kotlinx.schema.json.AdditionalPropertiesSchema
import kotlinx.schema.json.ArrayPropertyDefinition
import kotlinx.schema.json.BooleanPropertyDefinition
import kotlinx.schema.json.DenyAdditionalProperties
import kotlinx.schema.json.Discriminator
import kotlinx.schema.json.JsonSchema
import kotlinx.schema.json.JsonSchemaConstants.JSON_SCHEMA_ID_DRAFT202012
import kotlinx.schema.json.JsonSchemaConstants.Types.ARRAY_OR_NULL_TYPE
import kotlinx.schema.json.JsonSchemaConstants.Types.ARRAY_TYPE
import kotlinx.schema.json.JsonSchemaConstants.Types.BOOLEAN_OR_NULL_TYPE
import kotlinx.schema.json.JsonSchemaConstants.Types.BOOLEAN_TYPE
import kotlinx.schema.json.JsonSchemaConstants.Types.INTEGER_OR_NULL_TYPE
import kotlinx.schema.json.JsonSchemaConstants.Types.INTEGER_TYPE
import kotlinx.schema.json.JsonSchemaConstants.Types.NULL_TYPE
import kotlinx.schema.json.JsonSchemaConstants.Types.NUMBER_OR_NULL_TYPE
import kotlinx.schema.json.JsonSchemaConstants.Types.NUMBER_TYPE
import kotlinx.schema.json.JsonSchemaConstants.Types.OBJECT_OR_NULL_TYPE
import kotlinx.schema.json.JsonSchemaConstants.Types.OBJECT_TYPE
import kotlinx.schema.json.JsonSchemaConstants.Types.STRING_OR_NULL_TYPE
import kotlinx.schema.json.JsonSchemaConstants.Types.STRING_TYPE
import kotlinx.schema.json.NumericPropertyDefinition
import kotlinx.schema.json.ObjectPropertyDefinition
import kotlinx.schema.json.OneOfPropertyDefinition
import kotlinx.schema.json.PropertyDefinition
import kotlinx.schema.json.ReferencePropertyDefinition
import kotlinx.schema.json.StringPropertyDefinition
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlin.jvm.JvmOverloads

/**
 * Transforms [TypeGraph] IR into JSON Schema Draft 2020-12 format.
 *
 * Converts type graphs from introspectors (reflection, KSP) into JSON Schema definitions.
 * Supports primitives, collections, objects, enums, and sealed hierarchies with discriminators.
 * All named types (objects, enums, sealed hierarchies) are emitted as `$ref` with definitions
 * registered in `$defs` exactly once, regardless of nullability. Nullable named types use
 * `oneOf: [{type: null}, {$ref}]`.
 *
 * @param json JSON encoder for schema elements
 */
@Suppress("TooManyFunctions")
public class TypeGraphToJsonSchemaTransformer
    @JvmOverloads
    public constructor(
        public override val config: JsonSchemaConfig,
        public val json: Json = Json { encodeDefaults = false },
    ) : AbstractTypeGraphTransformer<JsonSchema, JsonSchemaConfig>(
            config = config,
        ) {
        /**
         * Transforms a type graph into a JSON Schema.
         *
         * @param graph Type graph with all type definitions
         * @param rootName Schema name
         * @return Complete JSON Schema definition
         */
        override fun transform(
            graph: TypeGraph,
            rootName: String,
        ): JsonSchema {
            val definitions = mutableMapOf<String, PropertyDefinition>()
            // Resolve the root node directly to avoid the root becoming a bare $ref
            val rootNode =
                when (val root = graph.root) {
                    is TypeRef.Inline -> root.node
                    is TypeRef.Ref ->
                        checkNotNull(graph.nodes[root.id]) {
                            "Root type '${root.id.value}' not found in type graph"
                        }
                }
            val schemaDefinition =
                when (val rootDefinition = convertNode(rootNode, nullable = false, graph, definitions)) {
                    is ObjectPropertyDefinition -> {
                        createObjectSchemaDefinition(rootName, rootDefinition, definitions)
                    }

                    is OneOfPropertyDefinition -> {
                        createPolymorphicSchemaDefinition(
                            rootName,
                            rootDefinition,
                            definitions,
                        )
                    }

                    is StringPropertyDefinition -> {
                        createStringSchemaDefinition(rootName, rootDefinition, definitions)
                    }

                    is NumericPropertyDefinition -> {
                        createNumericSchemaDefinition(rootName, rootDefinition, definitions)
                    }

                    is BooleanPropertyDefinition -> {
                        createBooleanSchemaDefinition(rootName, rootDefinition, definitions)
                    }

                    is ArrayPropertyDefinition -> {
                        createArraySchemaDefinition(rootName, rootDefinition, definitions)
                    }

                    else -> {
                        createDefaultSchemaDefinition(rootName, definitions)
                    }
                }

            return schemaDefinition
        }

        /**
         * Creates schema definition for object types.
         */
        private fun createObjectSchemaDefinition(
            rootName: String,
            rootDefinition: ObjectPropertyDefinition,
            definitions: Map<String, PropertyDefinition>,
        ): JsonSchema =
            JsonSchema(
                schema = JSON_SCHEMA_ID_DRAFT202012,
                id = formatSchemaId(rootName),
                properties = rootDefinition.properties.orEmpty(),
                required = rootDefinition.required.orEmpty(),
                additionalProperties = rootDefinition.additionalProperties,
                description = rootDefinition.description,
                defs = definitions.takeIf { it.isNotEmpty() },
            )

        /**
         * Creates schema definition for polymorphic types (oneOf).
         */
        private fun createPolymorphicSchemaDefinition(
            rootName: String,
            rootDefinition: OneOfPropertyDefinition,
            definitions: Map<String, PropertyDefinition>,
        ): JsonSchema =
            JsonSchema(
                schema = JSON_SCHEMA_ID_DRAFT202012,
                id = formatSchemaId(rootName),
                properties = emptyMap(),
                required = emptyList(),
                additionalProperties = DenyAdditionalProperties,
                description = rootDefinition.description,
                oneOf = rootDefinition.oneOf,
                discriminator =
                    if (config.includeOpenAPIPolymorphicDiscriminator) {
                        rootDefinition.discriminator
                    } else {
                        null
                    },
                defs = definitions.takeIf { it.isNotEmpty() },
            )

        /**
         * Creates schema definition for string/enum root types.
         */
        private fun createStringSchemaDefinition(
            rootName: String,
            rootDefinition: StringPropertyDefinition,
            definitions: Map<String, PropertyDefinition>,
        ): JsonSchema =
            JsonSchema(
                schema = JSON_SCHEMA_ID_DRAFT202012,
                id = formatSchemaId(rootName),
                type = rootDefinition.type,
                `enum` = rootDefinition.enum?.map { JsonPrimitive(it) },
                description = rootDefinition.description,
                properties = emptyMap(),
                required = emptyList(),
                additionalProperties = null,
                defs = definitions.takeIf { it.isNotEmpty() },
            )

        /**
         * Creates schema definition for numeric root types.
         */
        private fun createNumericSchemaDefinition(
            rootName: String,
            rootDefinition: NumericPropertyDefinition,
            definitions: Map<String, PropertyDefinition>,
        ): JsonSchema =
            JsonSchema(
                schema = JSON_SCHEMA_ID_DRAFT202012,
                id = formatSchemaId(rootName),
                type = rootDefinition.type,
                description = rootDefinition.description,
                properties = emptyMap(),
                required = emptyList(),
                additionalProperties = null,
                defs = definitions.takeIf { it.isNotEmpty() },
            )

        /**
         * Creates schema definition for boolean root types.
         */
        private fun createBooleanSchemaDefinition(
            rootName: String,
            rootDefinition: BooleanPropertyDefinition,
            definitions: Map<String, PropertyDefinition>,
        ): JsonSchema =
            JsonSchema(
                schema = JSON_SCHEMA_ID_DRAFT202012,
                id = formatSchemaId(rootName),
                type = rootDefinition.type,
                description = rootDefinition.description,
                properties = emptyMap(),
                required = emptyList(),
                additionalProperties = null,
                defs = definitions.takeIf { it.isNotEmpty() },
            )

        /**
         * Creates schema definition for array root types.
         */
        private fun createArraySchemaDefinition(
            rootName: String,
            rootDefinition: ArrayPropertyDefinition,
            definitions: Map<String, PropertyDefinition>,
        ): JsonSchema =
            JsonSchema(
                schema = JSON_SCHEMA_ID_DRAFT202012,
                id = formatSchemaId(rootName),
                type = rootDefinition.type,
                description = rootDefinition.description,
                items = rootDefinition.items,
                properties = emptyMap(),
                required = emptyList(),
                additionalProperties = null,
                defs = definitions.takeIf { it.isNotEmpty() },
            )

        /**
         * Creates default schema definition for other types.
         */
        private fun createDefaultSchemaDefinition(
            rootName: String,
            definitions: Map<String, PropertyDefinition>,
        ): JsonSchema =
            JsonSchema(
                schema = JSON_SCHEMA_ID_DRAFT202012,
                id = formatSchemaId(rootName),
                properties = emptyMap(),
                required = emptyList(),
                additionalProperties = DenyAdditionalProperties,
                defs = definitions.takeIf { it.isNotEmpty() },
            )

        /**
         * Formats a qualified name as a schema ID.
         * Currently returns the qualified name as-is since '#' cannot be used in definition names
         * (it would break $ref references like "#/$defs/Name").
         */
        private fun formatSchemaId(qualifiedName: String): String = qualifiedName

        /**
         * Converts a type reference to a property definition.
         *
         * Named types ([TypeRef.Ref]) are always emitted as `$ref` with the definition registered
         * in `$defs` exactly once. Nullable named types use `oneOf: [{type: null}, {$ref}]`.
         * Inline types (primitives, lists, maps) are expanded directly.
         */
        private fun convertTypeRef(
            typeRef: TypeRef,
            graph: TypeGraph,
            definitions: MutableMap<String, PropertyDefinition>,
        ): PropertyDefinition =
            when (typeRef) {
                is TypeRef.Inline -> {
                    convertInlineNode(typeRef.node, typeRef.nullable, graph, definitions)
                }

                is TypeRef.Ref -> {
                    val id = typeRef.id
                    val node =
                        checkNotNull(graph.nodes[id]) {
                            "Type reference '${id.value}' not found in type graph. " +
                                "This indicates a bug in the introspector - all referenced types " +
                                "should be present in the graph's nodes map."
                        }
                    ensureNodeInDefinitions(id, node, graph, definitions)
                    val refDef = ReferencePropertyDefinition(ref = $$"#/$defs/$${id.value}")
                    if (typeRef.nullable) {
                        OneOfPropertyDefinition(
                            oneOf =
                                listOf(
                                    StringPropertyDefinition(type = NULL_TYPE, description = null, nullable = null),
                                    refDef,
                                ),
                            description = null,
                        )
                    } else {
                        refDef
                    }
                }
            }

        /**
         * Ensures [node] is registered in [definitions] under [id].
         * A placeholder is inserted first to safely handle circular type references.
         */
        private fun ensureNodeInDefinitions(
            id: TypeId,
            node: TypeNode,
            graph: TypeGraph,
            definitions: MutableMap<String, PropertyDefinition>,
        ) {
            if (id.value in definitions) return
            definitions[id.value] = ReferencePropertyDefinition() // placeholder to break cycles
            definitions[id.value] = convertNode(node, nullable = false, graph, definitions)
        }

        /**
         * Converts inline type nodes (primitives, lists, maps) to property definitions.
         * Complex types must use named references.
         */
        private fun convertInlineNode(
            node: TypeNode,
            nullable: Boolean,
            graph: TypeGraph,
            definitions: MutableMap<String, PropertyDefinition>,
        ): PropertyDefinition =
            when (node) {
                is PrimitiveNode -> {
                    convertPrimitive(node, nullable)
                }

                is ListNode -> {
                    convertList(node, nullable, graph, definitions)
                }

                is MapNode -> {
                    convertMap(node, nullable, graph, definitions)
                }

                else -> {
                    throw IllegalArgumentException(
                        "Unsupported inline node type: ${node::class.simpleName}. " +
                            "Only PrimitiveNode, ListNode, and MapNode can be inlined. " +
                            "Complex types like ObjectNode and EnumNode must use TypeRef.Ref.",
                    )
                }
            }

        /**
         * Converts any type node to a property definition.
         * Dispatches to specialized converters based on node type.
         */
        private fun convertNode(
            node: TypeNode,
            nullable: Boolean,
            graph: TypeGraph,
            definitions: MutableMap<String, PropertyDefinition>,
        ): PropertyDefinition =
            when (node) {
                is PrimitiveNode -> convertPrimitive(node, nullable)
                is ObjectNode -> convertObject(node, nullable, graph, definitions)
                is EnumNode -> convertEnum(node, nullable)
                is ListNode -> convertList(node, nullable, graph, definitions)
                is MapNode -> convertMap(node, nullable, graph, definitions)
                is PolymorphicNode -> convertPolymorphic(node, nullable, graph, definitions)
            }

        /**
         * Determines the nullable flag value based on config and nullable parameter.
         */
        private fun getNullableFlag(nullable: Boolean): Boolean? =
            if (!config.useUnionTypes && nullable && config.useNullableField) true else null

        private fun convertPrimitive(
            node: PrimitiveNode,
            nullable: Boolean,
        ): PropertyDefinition =
            when (node.kind) {
                PrimitiveKind.STRING -> {
                    StringPropertyDefinition(
                        type = if (nullable && config.useUnionTypes) STRING_OR_NULL_TYPE else STRING_TYPE,
                        description = null,
                        nullable = getNullableFlag(nullable),
                    )
                }

                PrimitiveKind.BOOLEAN -> {
                    BooleanPropertyDefinition(
                        type = if (nullable && config.useUnionTypes) BOOLEAN_OR_NULL_TYPE else BOOLEAN_TYPE,
                        description = null,
                        nullable = getNullableFlag(nullable),
                    )
                }

                PrimitiveKind.INT, PrimitiveKind.LONG -> {
                    NumericPropertyDefinition(
                        type = if (nullable && config.useUnionTypes) INTEGER_OR_NULL_TYPE else INTEGER_TYPE,
                        description = null,
                        nullable = getNullableFlag(nullable),
                    )
                }

                PrimitiveKind.FLOAT, PrimitiveKind.DOUBLE -> {
                    NumericPropertyDefinition(
                        type = if (nullable && config.useUnionTypes) NUMBER_OR_NULL_TYPE else NUMBER_TYPE,
                        description = null,
                        nullable = getNullableFlag(nullable),
                    )
                }
            }

        /**
         * Converts object nodes (classes, data classes) to object property definitions.
         * Handles property mapping, required fields, and nullable optional properties based on config.
         */
        @Suppress("CyclomaticComplexMethod")
        private fun convertObject(
            node: ObjectNode,
            nullable: Boolean,
            graph: TypeGraph,
            definitions: MutableMap<String, PropertyDefinition>,
        ): PropertyDefinition {
            // Build required list based on config flags
            val required =
                node.properties
                    .filter { property ->
                        property.isConstant ||
                            when {
                                config.respectDefaultPresence -> {
                                    !property.hasDefaultValue ||
                                        (config.requireNullableFields && property.type.nullable)
                                }

                                config.requireNullableFields -> {
                                    true
                                }

                                else -> {
                                    !property.type.nullable
                                }
                            }
                    }.map { it.name }
                    .toSet()

            // Convert all properties
            val properties =
                node.properties.associate { property ->
                    val isRequired = property.name in required

                    val propertyDef = convertTypeRef(property.type, graph, definitions)

                    // Remove nullable flag if property is required (in required array)
                    // Convention: nullable flag is only used for optional properties
                    val withoutNullableIfRequired =
                        if (isRequired) {
                            removeNullableFlag(propertyDef)
                        } else {
                            propertyDef
                        }

                    val withDefaultOrConst =
                        when {
                            property.isConstant -> {
                                setConstValue(withoutNullableIfRequired, property.defaultValue)
                            }

                            !isRequired && property.defaultValue != null -> {
                                setDefaultValue(withoutNullableIfRequired, property.defaultValue)
                            }

                            else -> {
                                withoutNullableIfRequired
                            }
                        }

                    // Add description if available
                    val finalDef =
                        property.description?.let { desc ->
                            setDescription(withDefaultOrConst, desc)
                        } ?: withDefaultOrConst
                    property.name to finalDef
                }

            return ObjectPropertyDefinition(
                type = if (nullable && config.useUnionTypes) OBJECT_OR_NULL_TYPE else OBJECT_TYPE,
                description = node.description,
                nullable = getNullableFlag(nullable),
                properties = properties,
                required = required.toList(),
                additionalProperties = DenyAdditionalProperties,
            )
        }

        private fun convertEnum(
            node: EnumNode,
            nullable: Boolean,
        ): PropertyDefinition =
            StringPropertyDefinition(
                type = if (nullable && config.useUnionTypes) STRING_OR_NULL_TYPE else STRING_TYPE,
                description = node.description,
                nullable = getNullableFlag(nullable),
                enum = node.entries,
            )

        private fun convertList(
            node: ListNode,
            nullable: Boolean,
            graph: TypeGraph,
            definitions: MutableMap<String, PropertyDefinition>,
        ): PropertyDefinition {
            val items = convertTypeRef(node.element, graph, definitions)
            return ArrayPropertyDefinition(
                type = if (nullable && config.useUnionTypes) ARRAY_OR_NULL_TYPE else ARRAY_TYPE,
                description = null,
                nullable = getNullableFlag(nullable),
                items = items,
            )
        }

        private fun convertMap(
            node: MapNode,
            nullable: Boolean,
            graph: TypeGraph,
            definitions: MutableMap<String, PropertyDefinition>,
        ): PropertyDefinition {
            // Maps are represented as objects with additionalProperties
            // The value type determines what additionalProperties accepts
            val valuePropertyDef = convertTypeRef(node.value, graph, definitions)

            return ObjectPropertyDefinition(
                type = if (nullable && config.useUnionTypes) OBJECT_OR_NULL_TYPE else OBJECT_TYPE,
                description = null,
                nullable = getNullableFlag(nullable),
                additionalProperties = AdditionalPropertiesSchema(valuePropertyDef),
            )
        }

        /**
         * Converts sealed class hierarchies to JSON Schema oneOf with $ref and $defs.
         *
         * Resolves each subtype node directly (bypassing `convertTypeRef`) so the full definition
         * is available for discriminator injection before registration in `$defs`.
         *
         * @param node Polymorphic node with subtypes and discriminator
         * @param nullable Unused — nullable wrapping is handled by [convertTypeRef]
         * @param graph Type graph with all definitions
         * @param definitions Map to collect type definitions for $defs
         * @return [OneOfPropertyDefinition] with `$ref` entries for each subtype
         */
        @Suppress("LongMethod", "UnusedParameter")
        private fun convertPolymorphic(
            node: PolymorphicNode,
            nullable: Boolean,
            graph: TypeGraph,
            definitions: MutableMap<String, PropertyDefinition>,
        ): PropertyDefinition {
            // Convert each subtype and add to $defs, collect $ref for oneOf
            val subtypeRefs =
                node.subtypes.map { subtypeRef ->
                    val typeName = subtypeRef.id.value
                    val subtypeNode =
                        checkNotNull(graph.nodes[subtypeRef.id]) {
                            "Subtype '$typeName' not found in type graph"
                        }

                    val subtypeDefinition =
                        when (val definition = convertNode(subtypeNode, nullable = false, graph, definitions)) {
                            is ObjectPropertyDefinition -> {
                                // Append discriminator property to the definition if enabled
                                if (config.includePolymorphicDiscriminator) {
                                    val discriminatorProperty =
                                        StringPropertyDefinition(
                                            constValue = JsonPrimitive(typeName),
                                        )

                                    definition.copy(
                                        properties =
                                            mapOf(node.discriminator.name to discriminatorProperty) +
                                                definition.properties.orEmpty(),
                                        required = listOf(node.discriminator.name) + definition.required.orEmpty(),
                                    )
                                } else {
                                    definition
                                }
                            }

                            // Nested sealed hierarchy: intermediate sealed subtype is itself polymorphic.
                            // No discriminator injection — the leaf subtypes carry the const.
                            is OneOfPropertyDefinition -> {
                                definition
                            }

                            else -> {
                                error(
                                    "All subtypes of a polymorphic type must be objects or sealed hierarchies. " +
                                        "Found subtype '$typeName' with type '${definition::class.simpleName}'.",
                                )
                            }
                        }

                    // Add to definitions map for $defs section
                    definitions[typeName] = subtypeDefinition

                    // Return a reference to this definition
                    ReferencePropertyDefinition(ref = $$"#/$defs/$$typeName")
                }

            // Convert discriminator with proper $ref paths if OpenAPI polymorphic discriminator is enabled
            val discriminator =
                if (
                    config.includePolymorphicDiscriminator &&
                    config.includeOpenAPIPolymorphicDiscriminator
                ) {
                    node.discriminator.let { disc ->
                        val mapping =
                            disc.mapping?.mapValues { (_, typeId) ->
                                $$"#/$defs/$${typeId.value}"
                            }
                        Discriminator(
                            propertyName = disc.name,
                            mapping = mapping,
                        )
                    }
                } else {
                    null
                }

            return OneOfPropertyDefinition(
                oneOf = subtypeRefs,
                discriminator = discriminator,
                description = node.description,
            )
        }
    }
