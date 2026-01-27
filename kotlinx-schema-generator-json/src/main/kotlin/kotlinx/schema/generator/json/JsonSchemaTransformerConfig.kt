package kotlinx.schema.generator.json

import kotlinx.schema.generator.json.JsonSchemaTransformerConfig.Companion.Default
import kotlinx.schema.generator.json.JsonSchemaTransformerConfig.Companion.Strict

/**
 * Configuration for JSON Schema transformers.
 *
 * Controls schema generation behavior with individual flags for strict mode compliance
 * and required field handling. Use the [Strict] preset for full OpenAI Strict Mode compliance.
 *
 * ## Configuration Flags
 *
 * The three flags work together to control required field behavior:
 *
 * | respectDefaultPresence | requireNullableFields | Behavior |
 * |------------------------|----------------------|----------|
 * | true | ignored | Use introspector's DefaultPresence (fields with defaults are optional) |
 * | false | true | All fields required (strict mode) |
 * | false | false | Only non-nullable fields required |
 *
 * @property strictSchemaFlag Whether to set `strict: true` flag in output schemas
 * @property respectDefaultPresence Whether to use introspector's DefaultPresence for determining required fields
 * @property requireNullableFields Whether to include nullable fields in required array
 *          (when not using default presence)
 *
 * @see [OpenAI Structured Outputs](https://platform.openai.com/docs/guides/function-calling)
 * @author Konstantin Pavlov
 */
public open class JsonSchemaTransformerConfig(
    /**
     * Whether to set the `strict: true` flag in output schemas.
     *
     * When `true`, the generated schema includes `"strict": true` in the JSON output.
     * Required for OpenAI Structured Outputs and function calling strict mode.
     *
     * Default: `false`
     */
    public val strictSchemaFlag: Boolean = false,
    /**
     * Whether to respect DefaultPresence from the introspector for determining required fields.
     *
     * When `true`: Uses `DefaultPresence.Required` from introspector. Fields without defaults
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
     * Whether to include nullable fields in the required array.
     *
     * Only applies when [respectDefaultPresence] is `false`.
     *
     * When `true`: All fields (including nullable ones) are in the required array.
     * Nullable fields use union types: `["string", "null"]`. Required for OpenAI Strict Mode.
     *
     * When `false`: Only non-nullable fields are in the required array.
     * Nullable fields are truly optional and can be omitted.
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
     * Default: `true`
     */
    public val requireNullableFields: Boolean = true,
) {
    public companion object {
        /**
         * Default configuration for standard JSON Schema generation.
         *
         * - Strict flag: disabled
         * - Uses default presence detection (fields with defaults are optional)
         * - Standard nullable handling
         *
         * **Note**: Works best with reflection-based introspection.
         * With KSP, behaves like [Strict] (all fields required).
         */
        public val Default: JsonSchemaTransformerConfig =
            JsonSchemaTransformerConfig(
                strictSchemaFlag = false,
                respectDefaultPresence = true,
                requireNullableFields = true, // ignored when respectDefaultPresence=true
            )

        /**
         * Simplified configuration using default presence detection.
         *
         * - Strict flag: disabled
         * - Required fields based on default values (uses introspector's DefaultPresence)
         *
         * **Note**: Identical to [Default]. Kept for backward compatibility.
         */
        public val Simple: JsonSchemaTransformerConfig =
            JsonSchemaTransformerConfig(
                strictSchemaFlag = false,
                respectDefaultPresence = true,
                requireNullableFields = true, // ignored when respectDefaultPresence=true
            )

        /**
         * Configuration for full OpenAI Strict Mode compliance:
         *  - `strictSchemaFlag = true` - sets `strict: true` in output
         *  - `respectDefaultPresence = false` - ignore default values
         *  - `requireNullableFields = true` - all fields in required array with union types
         *
         * Use this when generating schemas for OpenAI function calling APIs with strict mode enabled.
         *
         * See [OpenAI Structured Outputs](https://platform.openai.com/docs/guides/function-calling)
         */
        public val Strict: JsonSchemaTransformerConfig =
            JsonSchemaTransformerConfig(
                strictSchemaFlag = true,
                respectDefaultPresence = false,
                requireNullableFields = true,
            )
    }
}

/**
 * Configuration for function calling schema transformers.
 *
 * Extends [JsonSchemaTransformerConfig] with defaults optimized for LLM function calling.
 * By default, uses strict mode settings to comply with OpenAI function calling requirements.
 *
 * @see [OpenAI Function Calling](https://platform.openai.com/docs/guides/function-calling)
 */
public class FunctionCallingSchemaTransformerConfig(
    strictSchemaFlag: Boolean = true,
    respectDefaultPresence: Boolean = false,
    requireNullableFields: Boolean = true,
) : JsonSchemaTransformerConfig(
        strictSchemaFlag = strictSchemaFlag,
        respectDefaultPresence = respectDefaultPresence,
        requireNullableFields = requireNullableFields,
    ) {
    public companion object {
        /**
         * Default configuration for function calling schemas (strict mode enabled).
         *
         * - Strict flag: enabled
         * - All fields required including nullables
         * - Union type nullable handling
         */
        public val Default: FunctionCallingSchemaTransformerConfig =
            FunctionCallingSchemaTransformerConfig(
                strictSchemaFlag = true,
                respectDefaultPresence = false,
                requireNullableFields = true,
            )

        /**
         * Non-strict configuration for legacy function calling schemas.
         *
         * - Strict flag: disabled
         * - Only non-nullable fields required
         */
        public val NonStrict: FunctionCallingSchemaTransformerConfig =
            FunctionCallingSchemaTransformerConfig(
                strictSchemaFlag = false,
                respectDefaultPresence = false,
                requireNullableFields = false,
            )
    }
}
