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
import kotlinx.schema.generator.core.ir.TypeNode
import kotlinx.schema.generator.core.ir.TypeRef
import kotlinx.schema.json.AdditionalPropertiesSchema
import kotlinx.schema.json.AnyOfPropertyDefinition
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
 * Nullable sealed types use `anyOf` with null option.
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
            val schemaDefinition =
                when (val rootDefinition = convertTypeRef(graph.root, graph, definitions)) {
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
                discriminator = if (config.includeDiscriminator) rootDefinition.discriminator else null,
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

        // FIXME correctly handle recursive polymorphism, it throws StackOverflowError now

        /**
         * Converts a type reference to a property definition.
         * Handles both inline types and named type references.
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
                    val node =
                        checkNotNull(graph.nodes[typeRef.id]) {
                            "Type reference '${typeRef.id.value}' not found in type graph. " +
                                "This indicates a bug in the introspector - all referenced types " +
                                "should be present in the graph's nodes map."
                        }
                    convertNode(node, typeRef.nullable, graph, definitions)
                }
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
        private fun convertObject(
            node: ObjectNode,
            nullable: Boolean,
            graph: TypeGraph,
            definitions: MutableMap<String, PropertyDefinition>,
        ): PropertyDefinition {
            // Build required list based on config flags
            val required =
                when {
                    config.respectDefaultPresence -> {
                        // Use introspector's hasDefaultValue: only fields without defaults are required
                        node.properties
                            .filter { property ->
                                !property.hasDefaultValue
                            }.map { it.name }
                    }

                    config.requireNullableFields -> {
                        // All fields required (including nullables) - strict mode
                        node.properties.map { it.name }
                    }

                    else -> {
                        // Only non-nullable fields required
                        node.properties
                            .filter { property ->
                                !property.type.nullable
                            }.map { it.name }
                    }
                }.toSet()

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

                    // Set const or default value if property has one
                    // Use const for required properties with fixed values in all modes
                    val withDefaultOrConst =
                        if (property.defaultValue != null) {
                            if (isRequired) {
                                setConstValue(withoutNullableIfRequired, property.defaultValue)
                            } else {
                                setDefaultValue(withoutNullableIfRequired, property.defaultValue)
                            }
                        } else {
                            withoutNullableIfRequired
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
         * Generates $defs entries for each subtype and uses $ref in oneOf array.
         * Discriminator mapping references proper $ref paths. Nullable types are
         * wrapped in anyOf with `null` option.
         *
         * @param node Polymorphic node with subtypes and discriminator
         * @param nullable Whether type reference is nullable
         * @param graph Type graph with all definitions
         * @param definitions Map to collect type definitions for $defs
         * @return OneOfPropertyDefinition, or AnyOfPropertyDefinition if nullable
         */
        @Suppress("LongMethod")
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

                    val subtypeDefinition =
                        convertTypeRef(subtypeRef.ref, graph, definitions)
                            .let { definition ->
                                definition as? ObjectPropertyDefinition
                                    ?: throw IllegalStateException(
                                        "All subtypes of a polymorphic type must be objects. " +
                                            "Found subtype '$typeName' with type '${definition::class.simpleName}'.",
                                    )
                            }.let { definition ->
                                // Append discriminator property to the definition if required
                                if (node.discriminator.required) {
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

                    // Add to definitions map for $defs section
                    definitions[typeName] = subtypeDefinition

                    // Return a reference to this definition
                    ReferencePropertyDefinition(ref = $$"#/$defs/$$typeName")
                }

            // Convert discriminator with proper $ref paths if includeDiscriminator is enabled
            val discriminator =
                if (config.includeDiscriminator) {
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

            val oneOfDef =
                OneOfPropertyDefinition(
                    oneOf = subtypeRefs,
                    discriminator = discriminator,
                    description = if (nullable) null else node.description,
                )

            // If nullable, wrap in anyOf with the 'null' option
            return if (nullable) {
                AnyOfPropertyDefinition(
                    anyOf =
                        listOf(
                            oneOfDef,
                            StringPropertyDefinition(
                                type = NULL_TYPE,
                                description = null,
                                nullable = null,
                            ),
                        ),
                    description = null, // Description set by setDescription in convertObject
                )
            } else {
                oneOfDef
            }
        }
    }
