package kotlinx.schema.json

/**
 * Collection of special constants, such as keys and data types, from JSON schema definition.
 */
public object JsonSchemaConstants {
    public const val JSON_SCHEMA_ID_DRAFT202012: String = "https://json-schema.org/draft/2020-12/schema"

    /**
     * JSON Schema keys.
     */
    public object Keys {
        // Top level schema info keys
        public const val SCHEMA: String = $$"$schema"
        public const val ID: String = $$"$id"
        public const val DEFS: String = $$"$defs"
        public const val COMMENT: String = $$"$comment"

        // Type definition keys
        public const val TYPE: String = "type"

        public const val TITLE: String = "title"
        public const val PROPERTIES: String = "properties"
        public const val REQUIRED: String = "required"
        public const val NULLABLE: String = "nullable"
        public const val DESCRIPTION: String = "description"
        public const val ITEMS: String = "items"
        public const val ENUM: String = "enum"
        public const val CONST: String = "const"
        public const val ADDITIONAL_PROPERTIES: String = "additionalProperties"

        // Special JSON Schema function keys
        public const val ONE_OF: String = "oneOf"
        public const val ANY_OF: String = "anyOf"
        public const val ALL_OF: String = "allOf"
        public const val PATTERN: String = "pattern"

        // Definition references related keys
        public const val REF: String = $$"$ref"
        public const val REF_PREFIX: String = "#/$DEFS/"
    }

    /**
     * JSON Schema types.
     */
    public object Types {
        public const val STRING: String = "string"
        public const val INTEGER: String = "integer"
        public const val NUMBER: String = "number"
        public const val BOOLEAN: String = "boolean"
        public const val ARRAY: String = "array"
        public const val OBJECT: String = "object"
        public const val NULL: String = "null"
        public const val ANY: String = "any"

        public val ARRAY_TYPE: List<String> = listOf(ARRAY)
        public val ARRAY_OR_NULL_TYPE: List<String> = listOf(ARRAY, NULL)
        public val BOOLEAN_TYPE: List<String> = listOf(BOOLEAN)
        public val BOOLEAN_OR_NULL_TYPE: List<String> = listOf(BOOLEAN, NULL)
        public val INTEGER_TYPE: List<String> = listOf(INTEGER)
        public val INTEGER_OR_NULL_TYPE: List<String> = listOf(INTEGER, NULL)
        public val NUMBER_TYPE: List<String> = listOf(NUMBER)
        public val NUMBER_OR_NULL_TYPE: List<String> = listOf(NUMBER, NULL)
        public val OBJECT_TYPE: List<String> = listOf(OBJECT)
        public val OBJECT_OR_NULL_TYPE: List<String> = listOf(OBJECT, NULL)
        public val STRING_TYPE: List<String> = listOf(STRING)
        public val STRING_OR_NULL_TYPE: List<String> = listOf(STRING, NULL)
        public val NULL_TYPE: List<String> = listOf(NULL)
    }
}
