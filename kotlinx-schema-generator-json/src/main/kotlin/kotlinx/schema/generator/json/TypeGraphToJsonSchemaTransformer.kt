package kotlinx.schema.generator.json

import kotlinx.schema.generator.core.ir.DefaultPresence
import kotlinx.schema.generator.core.ir.EnumNode
import kotlinx.schema.generator.core.ir.ListNode
import kotlinx.schema.generator.core.ir.MapNode
import kotlinx.schema.generator.core.ir.ObjectNode
import kotlinx.schema.generator.core.ir.PolymorphicNode
import kotlinx.schema.generator.core.ir.PrimitiveKind
import kotlinx.schema.generator.core.ir.PrimitiveNode
import kotlinx.schema.generator.core.ir.TypeGraph
import kotlinx.schema.generator.core.ir.TypeGraphTransformer
import kotlinx.schema.generator.core.ir.TypeNode
import kotlinx.schema.generator.core.ir.TypeRef
import kotlinx.schema.json.AnyOfPropertyDefinition
import kotlinx.schema.json.ArrayPropertyDefinition
import kotlinx.schema.json.BooleanPropertyDefinition
import kotlinx.schema.json.Discriminator
import kotlinx.schema.json.JsonSchema
import kotlinx.schema.json.JsonSchemaDefinition
import kotlinx.schema.json.NumericPropertyDefinition
import kotlinx.schema.json.ObjectPropertyDefinition
import kotlinx.schema.json.OneOfPropertyDefinition
import kotlinx.schema.json.PropertyDefinition
import kotlinx.schema.json.ReferencePropertyDefinition
import kotlinx.schema.json.StringPropertyDefinition
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.encodeToJsonElement

public data class JsonSchemaConfig(
    /**
     * Controls how optional nullable properties are represented in JSON Schema.
     *
     * When `true`: `val age: Int? = null` becomes:
     *   - Included in "required" array
     *   - Type is `["integer", "null"]`
     *   - `"default": null` is set
     *
     * When false (default): Such properties are omitted from "required"
     */
    val treatNullableOptionalAsRequired: Boolean = false,
    val json: Json = Json,
) {
    public companion object {
        /**
         * Default configuration instance for JSON Schema generation.
         *
         * Provides a preconfigured instance of [JsonSchemaConfig] with the default settings.
         *
         * Can be used as a baseline configuration or as a convenient default for most use cases.
         */
        public val Default: JsonSchemaConfig = JsonSchemaConfig()
    }
}

/**
 * Transforms [TypeGraph] IR into JSON Schema Draft 2020-12 format.
 *
 * Converts type graphs from introspectors (reflection, KSP) into JSON Schema definitions.
 * Supports primitives, collections, objects, enums, and sealed hierarchies with discriminators.
 * Nullable sealed types use `anyOf` with null option.
 *
 * @property config JSON Schema generation configuration
 * @param json JSON encoder for schema elements
 */
@Suppress("TooManyFunctions")
public class TypeGraphToJsonSchemaTransformer
    @JvmOverloads
    public constructor(
        public val config: JsonSchemaConfig = JsonSchemaConfig.Default,
        private val json: Json = Json { encodeDefaults = false },
    ) : TypeGraphTransformer<JsonSchema> {
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

            // Extract the main schema definition
            val schemaDefinition =
                when (val rootDefinition = convertTypeRef(graph.root, graph, definitions)) {
                    is ObjectPropertyDefinition -> {
                        JsonSchemaDefinition(
                            properties = rootDefinition.properties ?: emptyMap(),
                            required = rootDefinition.required ?: emptyList(),
                            additionalProperties = rootDefinition.additionalProperties,
                            description = rootDefinition.description,
                            defs = definitions.takeIf { it.isNotEmpty() },
                        )
                    }

                    is OneOfPropertyDefinition -> {
                        // For polymorphic types, use oneOf at the schema definition level
                        JsonSchemaDefinition(
                            properties = emptyMap(),
                            required = emptyList(),
                            additionalProperties = JsonPrimitive(false),
                            description = rootDefinition.description,
                            oneOf = rootDefinition.oneOf,
                            discriminator = rootDefinition.discriminator,
                            defs = definitions.takeIf { it.isNotEmpty() },
                        )
                    }

                    else -> {
                        // For non-object types, wrap in a schema definition
                        JsonSchemaDefinition(
                            properties = emptyMap(),
                            required = emptyList(),
                            additionalProperties = JsonPrimitive(false),
                            defs = definitions.takeIf { it.isNotEmpty() },
                        )
                    }
                }

            return JsonSchema(
                name = rootName,
                strict = false,
                description = null,
                schema = schemaDefinition,
            )
        }

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
                        graph.nodes[typeRef.id]
                            ?: throw IllegalStateException(
                                "Type reference '${typeRef.id.value}' not found in type graph. " +
                                    "This indicates a bug in the introspector - all referenced types " +
                                    "should be present in the graph's nodes map.",
                            )
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

        private fun convertPrimitive(
            node: PrimitiveNode,
            nullable: Boolean,
        ): PropertyDefinition =
            when (node.kind) {
                PrimitiveKind.STRING -> {
                    StringPropertyDefinition(
                        type = listOf("string"),
                        description = null,
                        nullable = if (nullable) true else null,
                    )
                }

                PrimitiveKind.BOOLEAN -> {
                    BooleanPropertyDefinition(
                        type = listOf("boolean"),
                        description = null,
                        nullable = if (nullable) true else null,
                    )
                }

                PrimitiveKind.INT -> {
                    NumericPropertyDefinition(
                        type = listOf("integer"),
                        description = null,
                        nullable = if (nullable) true else null,
                    )
                }

                PrimitiveKind.LONG -> {
                    NumericPropertyDefinition(
                        type = listOf("integer"),
                        description = null,
                        nullable = if (nullable) true else null,
                    )
                }

                PrimitiveKind.FLOAT, PrimitiveKind.DOUBLE -> {
                    NumericPropertyDefinition(
                        type = listOf("number"),
                        description = null,
                        nullable = if (nullable) true else null,
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
            // Build required list based on config and DefaultPresence
            val required =
                if (config.treatNullableOptionalAsRequired) {
                    // Include all properties in required
                    node.properties.map { it.name }
                } else {
                    // Only include properties without defaults (Required) in required list
                    node.properties
                        .filter { property ->
                            property.defaultPresence == DefaultPresence.Required
                        }.map { it.name }
                }.toSet()

            // Convert all properties
            val properties =
                node.properties.associate { property ->
                    val isNullable =
                        when (val typeRef = property.type) {
                            is TypeRef.Inline -> typeRef.nullable
                            is TypeRef.Ref -> typeRef.nullable
                        }
                    val hasDefault = property.defaultPresence != DefaultPresence.Required

                    val propertyDef = convertTypeRef(property.type, graph, definitions)

                    // Adjust based on config and property characteristics
                    val adjustedDef =
                        when {
                            // When treatNullableOptionalAsRequired=true and property has default (is optional):
                            // add "null" to type array and set default value
                            config.treatNullableOptionalAsRequired && hasDefault && isNullable -> {
                                addNullToTypeAndSetDefault(propertyDef)
                            }

                            // Property without default (required): remove nullable flag
                            !hasDefault -> {
                                removeNullableFlag(propertyDef)
                            }

                            // Properties with defaults when config is false: set the default value
                            hasDefault -> {
                                setDefaultValue(propertyDef, property.defaultValue)
                            }

                            else -> {
                                propertyDef
                            }
                        }

                    // Add the property description if available
                    val finalDef =
                        adjustedDef.let { def ->
                            property.description?.let { setDescription(def, it) } ?: def
                        }
                    property.name to finalDef
                }

            return ObjectPropertyDefinition(
                type = listOf("object"),
                description = node.description,
                nullable = if (nullable) true else null,
                properties = properties,
                required = required.toList(),
                additionalProperties = JsonPrimitive(false),
            )
        }

        private fun addNullToTypeAndSetDefault(propertyDef: PropertyDefinition): PropertyDefinition =
            when (propertyDef) {
                is StringPropertyDefinition -> {
                    propertyDef.copy(
                        type = propertyDef.type + "null",
                        nullable = null,
                        default = null,
                    )
                }

                is NumericPropertyDefinition -> {
                    propertyDef.copy(
                        type = propertyDef.type + "null",
                        nullable = null,
                        default = null,
                    )
                }

                is BooleanPropertyDefinition -> {
                    propertyDef.copy(
                        type = propertyDef.type + "null",
                        nullable = null,
                        default = null,
                    )
                }

                is ArrayPropertyDefinition -> {
                    propertyDef.copy(
                        type = propertyDef.type + "null",
                        nullable = null,
                        default = null,
                    )
                }

                is ObjectPropertyDefinition -> {
                    propertyDef.copy(
                        type = propertyDef.type + "null",
                        nullable = null,
                        default = null,
                    )
                }

                else -> {
                    propertyDef
                }
            }

        private fun convertEnum(
            node: EnumNode,
            nullable: Boolean,
        ): PropertyDefinition =
            StringPropertyDefinition(
                type = listOf("string"),
                description = node.description,
                nullable = if (nullable) true else null,
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
                type = listOf("array"),
                description = null,
                nullable = if (nullable) true else null,
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
            val additionalPropertiesSchema = json.encodeToJsonElement(valuePropertyDef)

            return ObjectPropertyDefinition(
                type = listOf("object"),
                description = null,
                nullable = if (nullable) true else null,
                additionalProperties = additionalPropertiesSchema,
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
                    val subtypeDefinition = convertTypeRef(subtypeRef.ref, graph, definitions)

                    // Add to definitions map for $defs section
                    definitions[typeName] = subtypeDefinition

                    // Return a reference to this definition
                    ReferencePropertyDefinition(ref = "#/\$defs/$typeName")
                }

            // Convert discriminator with proper $ref paths
            val discriminator =
                node.discriminator?.let { disc ->
                    val mapping =
                        disc.mapping?.mapValues { (_, typeId) ->
                            "#/\$defs/${typeId.value}"
                        }
                    Discriminator(
                        propertyName = disc.name,
                        mapping = mapping,
                    )
                }

            val oneOfDef =
                OneOfPropertyDefinition(
                    oneOf = subtypeRefs,
                    discriminator = discriminator,
                    description = if (nullable) null else node.description,
                )

            // If nullable, wrap in anyOf with null option
            return if (nullable) {
                AnyOfPropertyDefinition(
                    anyOf =
                        listOf(
                            oneOfDef,
                            StringPropertyDefinition(
                                type = listOf("null"),
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
