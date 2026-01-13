package kotlinx.schema.generator.json

/**
 * Strategy for determining which parameters should be marked as required in the function schema.
 *
 * This strategy ONLY affects which fields are included in the `required` array.
 * Other OpenAI Strict Mode features (additionalProperties: false, strict: true flag)
 * are always enabled.
 *
 * @see [OpenAI Strict Mode](https://platform.openai.com/docs/guides/function-calling?strict-mode=enabled#strict-mode)
 */
public enum class RequiredFieldStrategy {
    /**
     * OpenAI Strict Mode (Default): All parameters are marked as required.
     *
     * In strict mode, all parameters must be present in the `required` array.
     * Optional/nullable parameters are represented using union types with null: `["string", "null"]`.
     *
     * Example:
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
     * This ensures the model always provides all fields, even if they're null.
     */
    ALL_REQUIRED,

    /**
     * Non-strict mode: Only non-nullable parameters are marked as required.
     *
     * Nullable parameters are excluded from the required list, allowing them to be omitted entirely.
     * Use this when you want nullable parameters to be truly optional (not required with null value).
     *
     * Example:
     * ```kotlin
     * fun writeLog(level: String, exception: String? = null)
     * ```
     * Generates:
     * ```json
     * {
     *   "required": ["level"],
     *   "properties": {
     *     "level": { "type": "string" },
     *     "exception": { "type": ["string", "null"] }
     *   }
     * }
     * ```
     */
    NON_NULLABLE_REQUIRED,

    /**
     * Use the DefaultPresence from the introspector to determine required fields.
     *
     * Parameters with DefaultPresence.Required are marked as required.
     * Parameters with DefaultPresence.Absent are excluded from required.
     *
     * **Note**: This strategy doesn't work reliably with KSP because KSP cannot detect
     * default values in the same compilation unit. For KSP, this behaves the same as ALL_REQUIRED.
     *
     * Use this strategy with reflection-based introspection where default values are detectable.
     */
    USE_DEFAULT_PRESENCE,
}
