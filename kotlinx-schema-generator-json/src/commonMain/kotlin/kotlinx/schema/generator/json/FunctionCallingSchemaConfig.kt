package kotlinx.schema.generator.json

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
    respectDefaultPresence: Boolean = JsonSchemaConfig.Default.respectDefaultPresence,
    requireNullableFields: Boolean = JsonSchemaConfig.Default.requireNullableFields,
    useUnionTypes: Boolean = JsonSchemaConfig.Default.useUnionTypes,
    useNullableField: Boolean = JsonSchemaConfig.Default.useNullableField,
    includePolymorphicDiscriminator: Boolean = JsonSchemaConfig.Default.includePolymorphicDiscriminator,
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
        includePolymorphicDiscriminator = includePolymorphicDiscriminator,
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
                respectDefaultPresence = JsonSchemaConfig.Strict.respectDefaultPresence,
                requireNullableFields = JsonSchemaConfig.Strict.requireNullableFields,
                useUnionTypes = JsonSchemaConfig.Strict.useUnionTypes,
                useNullableField = JsonSchemaConfig.Strict.useNullableField,
                includePolymorphicDiscriminator = JsonSchemaConfig.Strict.includePolymorphicDiscriminator,
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
                respectDefaultPresence = true,
                requireNullableFields = false,
                useUnionTypes = true,
                useNullableField = false,
                includePolymorphicDiscriminator = false,
                strictMode = false,
            )

        /**
         * Default configuration for function calling schemas.
         */
        public val Default: FunctionCallingSchemaConfig = FunctionCallingSchemaConfig()
    }
}
