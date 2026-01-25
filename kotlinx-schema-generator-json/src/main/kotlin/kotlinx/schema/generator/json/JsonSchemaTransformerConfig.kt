package kotlinx.schema.generator.json

public class FunctionCallingSchemaTransformerConfig(
    public val requiredFieldStrategy: RequiredFieldStrategy,
) {
    public companion object {
        public val Default: FunctionCallingSchemaTransformerConfig =
            FunctionCallingSchemaTransformerConfig(
                requiredFieldStrategy = RequiredFieldStrategy.ALL_REQUIRED,
            )
    }
}

public class JsonSchemaTransformerConfig(
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
    public val treatNullableOptionalAsRequired: Boolean = false,
    public val requiredFieldStrategy: RequiredFieldStrategy = RequiredFieldStrategy.ALL_REQUIRED,
) {
    public companion object {
        public val Default: JsonSchemaTransformerConfig =
            JsonSchemaTransformerConfig(
                treatNullableOptionalAsRequired = false,
                requiredFieldStrategy = RequiredFieldStrategy.ALL_REQUIRED,
            )

        public val Simple: JsonSchemaTransformerConfig =
            JsonSchemaTransformerConfig(
                treatNullableOptionalAsRequired = false,
                requiredFieldStrategy = RequiredFieldStrategy.USE_DEFAULT_PRESENCE,
            )
    }
}
