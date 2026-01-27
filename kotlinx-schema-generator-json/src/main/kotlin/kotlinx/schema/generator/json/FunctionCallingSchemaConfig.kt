package kotlinx.schema.generator.json

import kotlinx.schema.generator.json.FunctionCallingSchemaConfig.Companion.Strict

/**
 * Configuration for function calling schema transformers.
 *
 * Extends [JsonSchemaConfig] with defaults optimized for LLM function calling.
 * By default, uses strict mode settings to comply with OpenAI function calling requirements.
 *
 * @see [OpenAI Function Calling](https://platform.openai.com/docs/guides/function-calling)
 */
public class FunctionCallingSchemaConfig(
    strictSchemaFlag: Boolean = true,
    respectDefaultPresence: Boolean = false,
    requireNullableFields: Boolean = true,
) : JsonSchemaConfig(
        strictSchemaFlag = strictSchemaFlag,
        respectDefaultPresence = respectDefaultPresence,
        requireNullableFields = requireNullableFields,
    ) {
    public companion object {
        /**
         * Strict configuration for function calling schemas (strict mode enabled).
         *
         * - Strict flag: enabled
         * - All fields required including nullables
         * - Union type nullable handling
         */
        public val Strict: FunctionCallingSchemaConfig =
            FunctionCallingSchemaConfig(
                strictSchemaFlag = true,
                respectDefaultPresence = false,
                requireNullableFields = true,
            )

        /**
         * Non-strict configuration for function calling schemas.
         *
         * - Strict flag: disabled
         * - Only non-nullable fields required
         */
        public val Simple: FunctionCallingSchemaConfig =
            FunctionCallingSchemaConfig(
                strictSchemaFlag = false,
                respectDefaultPresence = false,
                requireNullableFields = false,
            )

        /**
         * Default configuration for function calling schemas is [Strict] configuration.
         */
        public val Default: FunctionCallingSchemaConfig = Strict
    }
}
