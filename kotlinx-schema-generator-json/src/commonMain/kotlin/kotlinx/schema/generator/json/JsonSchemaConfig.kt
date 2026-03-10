package kotlinx.schema.generator.json

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
 * | true  | false | Fields without defaults required; nullable fields with defaults optional |
 * | true  | true  | Fields without defaults required; nullable fields always required |
 * | false | true  | All fields required (including nullables) |
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
     * When `true`: Fields without defaults are required; fields with defaults are optional.
     * If [requireNullableFields] is also `true`, nullable fields are additionally required
     * even when they carry a default value.
     *
     * When `false`: Uses [requireNullableFields] to determine required field behavior.
     *
     * **Note**: Does not work reliably with KSP because KSP cannot detect default values
     * in the same compilation unit. Works best with reflection-based introspection.
     *
     * Default: `true`
     */
    public val respectDefaultPresence: Boolean = true,
    /**
     * Whether nullable fields must be present in JSON.
     *
     * When [respectDefaultPresence] is `true`: additionally requires nullable fields
     * even when they have a default value (e.g. `val x: String? = null`).
     *
     * When [respectDefaultPresence] is `false`:
     * - `true`: All fields are required (must be present, can be null).
     * - `false`: Only non-nullable fields are required.
     *
     * Example with `requireNullableFields = true`:
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
     * Default: `false`
     */
    public val requireNullableFields: Boolean = false,
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
     *
     * It's a good practice to enable it by default
     **/
    public val includePolymorphicDiscriminator: Boolean = true,
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
            JsonSchemaConfig()

        /**
         * Configuration where all fields are required regardless of Kotlin default values.
         *
         *  - `requireNullableFields = true` — all fields in required array (including nullables)
         *  - `useUnionTypes = true` — union types for nullable fields: `["string", "null"]`
         *  - Type discriminators are enabled for polymorphic types
         *
         * Use this when generating schemas for OpenAI function calling APIs with strict mode enabled,
         * or any other schema consumer that requires all properties to be present in the JSON object.
         *
         * See [JSON Schema Draft 2020-12](https://json-schema.org/draft/2020-12/json-schema-core.html)
         */
        public val Strict: JsonSchemaConfig =
            JsonSchemaConfig(
                respectDefaultPresence = false,
                requireNullableFields = true,
                useUnionTypes = true,
                useNullableField = false,
                includePolymorphicDiscriminator = true,
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

    override fun toString(): String =
        "JsonSchemaConfig(" +
            "respectDefaultPresence=$respectDefaultPresence, " +
            "requireNullableFields=$requireNullableFields, " +
            "useUnionTypes=$useUnionTypes, " +
            "useNullableField=$useNullableField, " +
            "includePolymorphicDiscriminator=$includePolymorphicDiscriminator, " +
            "includeOpenAPIPolymorphicDiscriminator=$includeOpenAPIPolymorphicDiscriminator" +
            ")"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is JsonSchemaConfig) return false

        if (respectDefaultPresence != other.respectDefaultPresence) return false
        if (requireNullableFields != other.requireNullableFields) return false
        if (useUnionTypes != other.useUnionTypes) return false
        if (useNullableField != other.useNullableField) return false
        if (includePolymorphicDiscriminator != other.includePolymorphicDiscriminator) return false
        if (includeOpenAPIPolymorphicDiscriminator != other.includeOpenAPIPolymorphicDiscriminator) return false

        return true
    }

    override fun hashCode(): Int {
        var result = respectDefaultPresence.hashCode()
        result = 31 * result + requireNullableFields.hashCode()
        result = 31 * result + useUnionTypes.hashCode()
        result = 31 * result + useNullableField.hashCode()
        result = 31 * result + includePolymorphicDiscriminator.hashCode()
        result = 31 * result + includeOpenAPIPolymorphicDiscriminator.hashCode()
        return result
    }
}
