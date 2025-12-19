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
import kotlinx.schema.json.ArrayPropertyDefinition
import kotlinx.schema.json.BooleanPropertyDefinition
import kotlinx.schema.json.Discriminator
import kotlinx.schema.json.JsonSchema
import kotlinx.schema.json.JsonSchemaDefinition
import kotlinx.schema.json.NumericPropertyDefinition
import kotlinx.schema.json.ObjectPropertyDefinition
import kotlinx.schema.json.OneOfPropertyDefinition
import kotlinx.schema.json.PropertyDefinition
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

@Suppress("TooManyFunctions")
public class TypeGraphToJsonSchemaTransformer
    @JvmOverloads
    public constructor(
        public val config: JsonSchemaConfig = JsonSchemaConfig.Default,
        private val json: Json = Json { encodeDefaults = false },
    ) : TypeGraphTransformer<JsonSchema> {
        override fun transform(
            graph: TypeGraph,
            rootName: String,
        ): JsonSchema {
            val rootDefinition = convertTypeRef(graph.root, graph)

            // Extract the main schema definition
            val schemaDefinition =
                when (rootDefinition) {
                    is ObjectPropertyDefinition -> {
                        JsonSchemaDefinition(
                            properties = rootDefinition.properties ?: emptyMap(),
                            required = rootDefinition.required ?: emptyList(),
                            additionalProperties = rootDefinition.additionalProperties,
                            description = rootDefinition.description,
                        )
                    }

                    is OneOfPropertyDefinition -> {
                        // For polymorphic types, use oneOf at the schema definition level
                        JsonSchemaDefinition(
                            type = "object", // Keep type as object for compatibility
                            properties = emptyMap(),
                            required = emptyList(),
                            additionalProperties = JsonPrimitive(false),
                            description = rootDefinition.description,
                            oneOf = rootDefinition.oneOf,
                            discriminator = rootDefinition.discriminator,
                        )
                    }

                    else -> {
                        // For non-object types, wrap in a schema definition
                        JsonSchemaDefinition(
                            properties = emptyMap(),
                            required = emptyList(),
                            additionalProperties = JsonPrimitive(false),
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

        private fun convertTypeRef(
            typeRef: TypeRef,
            graph: TypeGraph,
        ): PropertyDefinition =
            when (typeRef) {
                is TypeRef.Inline -> convertInlineNode(typeRef.node, typeRef.nullable, graph)
                is TypeRef.Ref -> {
                    val node =
                        graph.nodes[typeRef.id]
                            ?: throw IllegalStateException(
                                "Type reference '${typeRef.id.value}' not found in type graph. " +
                                    "This indicates a bug in the introspector - all referenced types " +
                                    "should be present in the graph's nodes map.",
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
                is PrimitiveNode -> convertPrimitive(node, nullable)
                is ListNode -> convertList(node, nullable, graph)
                is MapNode -> convertMap(node, nullable, graph)
                else ->
                    throw IllegalArgumentException(
                        "Unsupported inline node type: ${node::class.simpleName}. " +
                            "Only PrimitiveNode, ListNode, and MapNode can be inlined. " +
                            "Complex types like ObjectNode and EnumNode must use TypeRef.Ref.",
                    )
            }

        private fun convertNode(
            node: TypeNode,
            nullable: Boolean,
            graph: TypeGraph,
        ): PropertyDefinition =
            when (node) {
                is PrimitiveNode -> convertPrimitive(node, nullable)
                is ObjectNode -> convertObject(node, nullable, graph)
                is EnumNode -> convertEnum(node, nullable)
                is ListNode -> convertList(node, nullable, graph)
                is MapNode -> convertMap(node, nullable, graph)
                is PolymorphicNode -> convertPolymorphic(node, graph)
                else ->
                    throw IllegalArgumentException(
                        "Unsupported node type: ${node::class.simpleName}. " +
                            "Expected one of: PrimitiveNode, ObjectNode, EnumNode, ListNode, MapNode, PolymorphicNode.",
                    )
            }

        private fun convertPrimitive(
            node: PrimitiveNode,
            nullable: Boolean,
        ): PropertyDefinition =
            when (node.kind) {
                PrimitiveKind.STRING ->
                    StringPropertyDefinition(
                        type = listOf("string"),
                        description = null,
                        nullable = if (nullable) true else null,
                    )

                PrimitiveKind.BOOLEAN ->
                    BooleanPropertyDefinition(
                        type = listOf("boolean"),
                        description = null,
                        nullable = if (nullable) true else null,
                    )

                PrimitiveKind.INT ->
                    NumericPropertyDefinition(
                        type = listOf("integer"),
                        description = null,
                        nullable = if (nullable) true else null,
                    )

                PrimitiveKind.LONG ->
                    NumericPropertyDefinition(
                        type = listOf("integer"),
                        description = null,
                        nullable = if (nullable) true else null,
                    )

                PrimitiveKind.FLOAT, PrimitiveKind.DOUBLE ->
                    NumericPropertyDefinition(
                        type = listOf("number"),
                        description = null,
                        nullable = if (nullable) true else null,
                    )
            }

        private fun convertObject(
            node: ObjectNode,
            nullable: Boolean,
            graph: TypeGraph,
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

                    val propertyDef = convertTypeRef(property.type, graph)

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
                            // Properties with defaults when config is false: keep as is
                            else -> propertyDef
                        }

                    // Add the property description if available
                    val description = property.description
                    val finalDef =
                        if (description != null) {
                            setDescription(adjustedDef, description)
                        } else {
                            adjustedDef
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
                is StringPropertyDefinition ->
                    propertyDef.copy(
                        type = propertyDef.type + "null",
                        nullable = null,
                        default = null,
                    )
                is NumericPropertyDefinition ->
                    propertyDef.copy(
                        type = propertyDef.type + "null",
                        nullable = null,
                        default = null,
                    )
                is BooleanPropertyDefinition ->
                    propertyDef.copy(
                        type = propertyDef.type + "null",
                        nullable = null,
                        default = null,
                    )
                is ArrayPropertyDefinition ->
                    propertyDef.copy(
                        type = propertyDef.type + "null",
                        nullable = null,
                        default = null,
                    )
                is ObjectPropertyDefinition ->
                    propertyDef.copy(
                        type = propertyDef.type + "null",
                        nullable = null,
                        default = null,
                    )
                else -> propertyDef
            }

        private fun removeNullableFlag(propertyDef: PropertyDefinition): PropertyDefinition =
            when (propertyDef) {
                is StringPropertyDefinition -> propertyDef.copy(nullable = null)
                is NumericPropertyDefinition -> propertyDef.copy(nullable = null)
                is BooleanPropertyDefinition -> propertyDef.copy(nullable = null)
                is ArrayPropertyDefinition -> propertyDef.copy(nullable = null)
                is ObjectPropertyDefinition -> propertyDef.copy(nullable = null)
                else -> propertyDef
            }

        private fun setDescription(
            propertyDef: PropertyDefinition,
            description: String,
        ): PropertyDefinition =
            when (propertyDef) {
                is StringPropertyDefinition -> propertyDef.copy(description = description)
                is NumericPropertyDefinition -> propertyDef.copy(description = description)
                is BooleanPropertyDefinition -> propertyDef.copy(description = description)
                is ArrayPropertyDefinition -> propertyDef.copy(description = description)
                is ObjectPropertyDefinition -> propertyDef.copy(description = description)
                else -> propertyDef
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
        ): PropertyDefinition {
            val items = convertTypeRef(node.element, graph)
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
        ): PropertyDefinition {
            // Maps are represented as objects with additionalProperties
            // The value type determines what additionalProperties accepts
            val valuePropertyDef = convertTypeRef(node.value, graph)
            val additionalPropertiesSchema = json.encodeToJsonElement(valuePropertyDef)

            return ObjectPropertyDefinition(
                type = listOf("object"),
                description = null,
                nullable = if (nullable) true else null,
                additionalProperties = additionalPropertiesSchema,
            )
        }

        private fun convertPolymorphic(
            node: PolymorphicNode,
            graph: TypeGraph,
        ): PropertyDefinition {
            // Convert each subtype to a PropertyDefinition
            val subtypeDefinitions =
                node.subtypes.map { subtypeRef ->
                    convertTypeRef(subtypeRef.ref, graph)
                }

            // Convert discriminator if present
            val discriminator =
                node.discriminator?.let { disc ->
                    val mapping =
                        disc.mapping?.mapValues { (_, typeId) ->
                            // For the mapping, we need to provide the reference string
                            // Typically this would be something like "#/definitions/ClassName"
                            // For now, we'll use the typeId value
                            typeId.value
                        }
                    Discriminator(
                        propertyName = disc.name,
                        mapping = mapping,
                    )
                }

            return OneOfPropertyDefinition(
                oneOf = subtypeDefinitions,
                discriminator = discriminator,
                description = node.description,
            )
        }
    }
