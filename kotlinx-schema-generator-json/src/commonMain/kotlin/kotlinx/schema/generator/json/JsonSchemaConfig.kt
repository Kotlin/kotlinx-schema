package kotlinx.schema.generator.json

import kotlinx.schema.generator.json.JsonSchemaConfig.Companion.Default
import kotlinx.schema.generator.json.JsonSchemaConfig.Companion.Strict

/**
 * Configuration for JSON Schema transformers.
 *
 * Controls schema generation behavior with individual flags for nullable handling
 * and required field handling. Use the [Strict] preset for full JSON Schema Draft 2020-12 compliance.
 *
 * ## Configuration Flags
 *
 * ### Required Field Behavior
 *
 * | respectDefaultPresence | requireNullableFields | Behavior |
 * |------------------------|-----------------------|----------|
 * | true | ignored | Use introspector's DefaultPresence (fields with defaults are optional) |
 * | false | true | All fields required (including nullables) - strict mode |
 * | false | false | Only non-nullable fields required |
 *
 * ### Nullable Type Representation
 *
 * | useUnionTypes | useNullableField | Output |
 * |---------------|------------------|--------|
 * | true | false | `{"type": ["string", "null"]}` (JSON Schema Draft 2020-12) |
 * | false | true | `{"type": "string", "nullable": true}` (legacy OpenAPI) |
 * | false | false | `{"type": "string"}` (no nullable indication) |
 *
 * @see [JSON Schema Draft 2020-12](https://json-schema.org/draft/2020-12/json-schema-core.html)
 * @author Konstantin Pavlov
 */
public open class JsonSchemaConfig(
    /**
     * Whether to use hasDefaultValue from introspector to determine required fields.
     *
     * When `true`: Uses `hasDefaultValue` from introspector. Fields without defaults
     * are marked as required, fields with defaults are optional.
     *
     * When `false`: Uses [requireNullableFields] to determine required field behavior.
     *
     * **Note**: This doesn't work reliably with KSP because KSP cannot detect default values
     * in the same compilation unit. Works best with reflection-based introspection.
     *
     * Default: `false`
     */
    public val respectDefaultPresence: Boolean = false,
    /**
     * Whether nullable fields must be present in JSON.
     *
     * Only applies when [respectDefaultPresence] is `false`.
     *
     * When `true`: Nullable fields ARE in the required array (must be present, can be null).
     * When `false`: Nullable fields are NOT in the required array (can be omitted).
     *
     * Example with `requireNullableFields = true` (strict mode):
     * ```kotlin
     * fun writeLog(level: String, exception: String? = null)
     * ```
     * Generates:
     * ```json
     * {
     *   "required": ["level", "exception"],
     *   "properties": {
     *     "level": { "type": "string" },
     *     "exception": { "type": ["string", "null"] }
     *   }
     * }
     * ```
     *
     * Example with `requireNullableFields = false`:
     * ```json
     * {
     *   "required": ["level"],
     *   "properties": {
     *     "level": { "type": "string" },
     *     "exception": { "type": ["string", "null"] }
     *   }
     * }
     * ```
     *
     * Default: `true` (Draft 2020-12 strict mode)
     */
    public val requireNullableFields: Boolean = true,
    /**
     * Whether to use union types for nullable fields.
     *
     * When `true`: Generates `["string", "null"]` (JSON Schema Draft 2020-12 standard).
     * When `false`: Uses nullable field instead (see [useNullableField]).
     *
     * Default: `true`
     */
    public val useUnionTypes: Boolean = true,
    /**
     * Whether to emit the nullable field for nullable types.
     *
     * When `true`: Adds `"nullable": true` (legacy OpenAPI compatibility).
     * When `false`: Omits nullable field (standard JSON Schema).
     *
     * **Note**: Ignored when [useUnionTypes] is `true`.
     *
     * Default: `false`
     */
    public val useNullableField: Boolean = false,
    /**
     * Whether to include a type discriminator field in polymorphic schemas.
     *
     * When enabled, each polymorphic subtype schema gets an additional `"type"` property
     * containing a constant string equal to the subtype's simple class name.
     **/
    public val includePolymorphicDiscriminator: Boolean = false,
    /**
     * Whether to include discriminator in polymorphic schemas.
     *
     * When `true`: Includes discriminator object in oneOf schemas (OpenAPI 3.x compatibility).
     * When `false`: Omits discriminator (standard JSON Schema Draft 2020-12).
     *
     * Note: Discriminator is an OpenAPI extension, not part of JSON Schema specification.
     * Note: to enable this option, [includePolymorphicDiscriminator] must also be `true`.
     *
     * Default: `false`
     */
    public val includeOpenAPIPolymorphicDiscriminator: Boolean = false,
) {
    init {
        // Validate flag combinations
        require(!useUnionTypes || !useNullableField) {
            "Cannot use both useUnionTypes and useNullableField. " +
                "Choose one: union types [\"string\", \"null\"] OR nullable field."
        }

        require(useUnionTypes || useNullableField) {
            "Either useUnionTypes or useNullableField must be enabled..."
        }

        require(!includeOpenAPIPolymorphicDiscriminator || includePolymorphicDiscriminator) {
            "includeOpenAPIPolymorphicDiscriminator requires includePolymorphicDiscriminator to be enabled"
        }
    }

    public companion object {
        /**
         * Default configuration for standard JSON Schema Draft 2020-12 generation.
         *
         * - Uses default presence detection (fields with defaults are optional)
         * - Uses union types for nullable fields: `["string", "null"]`
         * - Nullable fields are required (must be present in JSON)
         *
         * **Note**: Works best with reflection-based introspection.
         * With KSP, behaves like [Strict] (all fields required).
         */
        public val Default: JsonSchemaConfig =
            JsonSchemaConfig(
                respectDefaultPresence = true,
                requireNullableFields = true,
                useUnionTypes = true,
                useNullableField = false,
                includePolymorphicDiscriminator = false,
                includeOpenAPIPolymorphicDiscriminator = false,
            )

        /**
         * Configuration for full JSON Schema Draft 2020-12 compliance:
         *  - `respectDefaultPresence = false` - ignore default values (KSP limitation)
         *  - `requireNullableFields = true` - all fields in required array (including nullables)
         *  - `useUnionTypes = true` - union types for nullable fields
         *
         * Use this when generating schemas that must strictly comply with JSON Schema Draft 2020-12,
         * or when using with OpenAI function calling APIs with strict mode enabled.
         *
         * See [JSON Schema Draft 2020-12](https://json-schema.org/draft/2020-12/json-schema-core.html)
         */
        public val Strict: JsonSchemaConfig =
            JsonSchemaConfig(
                respectDefaultPresence = false,
                requireNullableFields = true,
                useUnionTypes = true,
                useNullableField = false,
                includePolymorphicDiscriminator = false,
                includeOpenAPIPolymorphicDiscriminator = false,
            )

        /**
         * Configuration for OpenAPI 3.x compatibility:
         *  - `respectDefaultPresence = true` - respect default values when available
         *  - `requireNullableFields = false` - nullable fields are optional
         *  - `useUnionTypes = false` - use nullable field instead of union types
         *  - `useNullableField = true` - emit "nullable": true for OpenAPI
         *  - `includeDiscriminator = true` - include discriminator for polymorphic types
         *
         * Use this when generating schemas for OpenAPI 3.x specifications.
         * OpenAPI 3.x uses a subset of JSON Schema with some extensions.
         *
         * See [OpenAPI 3.1 Specification](https://spec.openapis.org/oas/v3.1.0)
         */
        public val OpenAPI: JsonSchemaConfig =
            JsonSchemaConfig(
                respectDefaultPresence = true,
                requireNullableFields = false,
                useUnionTypes = false,
                useNullableField = true,
                includePolymorphicDiscriminator = true,
                includeOpenAPIPolymorphicDiscriminator = true,
            )
    }
}
