package kotlinx.schema.generator.json

import kotlinx.schema.generator.json.FunctionCallingSchemaConfig.Companion.Strict

/**
 * Configuration for function calling schema transformers.
 *
 * Extends [JsonSchemaConfig] with defaults optimized for LLM function calling.
 * By default, uses strict mode settings to comply with OpenAI function calling requirements.
 *
 * @property strictMode Whether to set `strict: true` flag in function calling schema output.
 *                      Required for OpenAI Structured Outputs and function calling strict mode.
 *
 * @see [OpenAI Function Calling](https://platform.openai.com/docs/guides/function-calling)
 */
public class FunctionCallingSchemaConfig(
    respectDefaultPresence: Boolean = false,
    requireNullableFields: Boolean = true,
    useUnionTypes: Boolean = true,
    useNullableField: Boolean = false,
    /**
     * Whether to set the `strict: true` flag in function calling schema output.
     *
     * When `true`, the generated schema includes `"strict": true` in the JSON output.
     * Required for OpenAI Structured Outputs and function calling strict mode.
     *
     * Default: `true`
     */
    public val strictMode: Boolean = true,
) : JsonSchemaConfig(
        respectDefaultPresence = respectDefaultPresence,
        requireNullableFields = requireNullableFields,
        useUnionTypes = useUnionTypes,
        useNullableField = useNullableField,
    ) {
    public companion object {
        /**
         * Strict configuration for function calling schemas (strict mode enabled).
         *
         * - Strict flag: enabled (`strict: true` in output)
         * - All fields required including nullables
         * - Union type nullable handling: `["string", "null"]`
         */
        public val Strict: FunctionCallingSchemaConfig =
            FunctionCallingSchemaConfig(
                respectDefaultPresence = false,
                requireNullableFields = true,
                useUnionTypes = true,
                useNullableField = false,
                strictMode = true,
            )

        /**
         * Non-strict configuration for function calling schemas.
         *
         * - Strict flag: disabled
         * - Only non-nullable fields required
         * - Union type nullable handling
         */
        public val Simple: FunctionCallingSchemaConfig =
            FunctionCallingSchemaConfig(
                respectDefaultPresence = false,
                requireNullableFields = false,
                useUnionTypes = true,
                useNullableField = false,
                strictMode = false,
            )

        /**
         * Default configuration for function calling schemas is [Strict] configuration.
         */
        public val Default: FunctionCallingSchemaConfig = Strict
    }
}
