package kotlinx.schema.generator.json

import kotlinx.schema.generator.core.ir.AbstractTypeGraphTransformer
import kotlinx.schema.generator.core.ir.TypeGraph
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonObject

/**
 * Transforms [TypeGraph] IR into JSON Schema as a [JsonObject].
 *
 * This transformer delegates to [TypeGraphToJsonSchemaTransformer] to produce
 * a [kotlinx.schema.json.JsonSchema] object first, then serializes it to [JsonObject].
 *
 * This two-step approach ensures:
 * - Both reflection and KSP generators use the same [TypeGraphToJsonSchemaTransformer]
 * - The JsonSchema type provides a structured, type-safe intermediate representation
 * - JsonObject can be produced when needed by serializing the JsonSchema
 *
 * @property config JSON Schema generation configuration
 * @param json JSON encoder for serialization
 */
public class TypeGraphToJsonObjectSchemaTransformer
    @JvmOverloads
    public constructor(
        public override val config: JsonSchemaConfig = JsonSchemaConfig.Default,
        private val json: Json = Json { encodeDefaults = true },
        private val jsonSchemaTransformer: TypeGraphToJsonSchemaTransformer =
            TypeGraphToJsonSchemaTransformer(config, json),
    ) : AbstractTypeGraphTransformer<JsonObject, JsonSchemaConfig>(
            config = config,
        ) {
        /**
         * Transforms a type graph into a JSON Schema [JsonObject].
         *
         * This method uses [TypeGraphToJsonSchemaTransformer] to ensure consistent type conversion,
         * but adds all nodes to $defs (matching the old TypeGraphToJsonObjectSchemaTransformer behavior).
         *
         * @param graph Type graph with all type definitions
         * @param rootName Schema name
         * @return JSON Schema as a [JsonObject]
         */
        override fun transform(
            graph: TypeGraph,
            rootName: String,
        ): JsonObject {
            // Step 1: Generate JsonSchema for structure (we'll replace $defs)
            val jsonSchema = jsonSchemaTransformer.transform(graph, rootName)

            // Step 2: Serialize JsonSchema to JsonObject
            return json.encodeToJsonElement(jsonSchema).jsonObject
        }
    }
