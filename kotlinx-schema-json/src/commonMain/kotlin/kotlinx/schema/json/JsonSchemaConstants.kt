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
        public const val ANCHOR: String = $$"$anchor"
        public const val DYNAMIC_ANCHOR: String = $$"$dynamicAnchor"
        public const val DEFS: String = $$"$defs"
        public const val COMMENT: String = $$"$comment"

        // Type definition keys
        public const val TYPE: String = "type"
        public const val NULLABLE: String = "nullable"

        public const val PROPERTIES: String = "properties"
        public const val PATTERN_PROPERTIES: String = "patternProperties"
        public const val ADDITIONAL_PROPERTIES: String = "additionalProperties"
        public const val PROPERTY_NAMES: String = "propertyNames"
        public const val REQUIRED: String = "required"
        public const val DEPENDENT_REQUIRED: String = "dependentRequired"
        public const val DEPENDENT_SCHEMAS: String = "dependentSchemas"
        public const val DEPENDENCIES: String = "dependencies"
        public const val MIN_PROPERTIES: String = "minProperties"
        public const val MAX_PROPERTIES: String = "maxProperties"
        public const val UNEVALUATED_PROPERTIES: String = "unevaluatedProperties"

        public const val ITEMS: String = "items"
        public const val PREFIX_ITEMS: String = "prefixItems"
        public const val CONTAINS: String = "contains"
        public const val MIN_CONTAINS: String = "minContains"
        public const val MAX_CONTAINS: String = "maxContains"
        public const val MIN_ITEMS: String = "minItems"
        public const val MAX_ITEMS: String = "maxItems"
        public const val UNIQUE_ITEMS: String = "uniqueItems"
        public const val UNEVALUATED_ITEMS: String = "unevaluatedItems"

        public const val MIN_LENGTH: String = "minLength"
        public const val MAX_LENGTH: String = "maxLength"
        public const val PATTERN: String = "pattern"
        public const val FORMAT: String = "format"
        public const val CONTENT_ENCODING: String = "contentEncoding"
        public const val CONTENT_MEDIA_TYPE: String = "contentMediaType"
        public const val CONTENT_SCHEMA: String = "contentSchema"

        public const val MINIMUM: String = "minimum"
        public const val MAXIMUM: String = "maximum"
        public const val EXCLUSIVE_MINIMUM: String = "exclusiveMinimum"
        public const val EXCLUSIVE_MAXIMUM: String = "exclusiveMaximum"
        public const val MULTIPLE_OF: String = "multipleOf"

        public const val ENUM: String = "enum"
        public const val CONST: String = "const"
        public const val DEFAULT: String = "default"
        public const val DEPRECATED: String = "deprecated"
        public const val READ_ONLY: String = "readOnly"
        public const val WRITE_ONLY: String = "writeOnly"
        public const val EXAMPLES: String = "examples"
        public const val TITLE: String = "title"
        public const val DESCRIPTION: String = "description"

        // Special JSON Schema function keys
        public const val ONE_OF: String = "oneOf"
        public const val ANY_OF: String = "anyOf"
        public const val ALL_OF: String = "allOf"
        public const val IF: String = "if"
        public const val THEN: String = "then"
        public const val ELSE: String = "else"
        public const val NOT: String = "not"

        // Definition references related keys
        public const val REF: String = $$"$ref"
        public const val DYNAMIC_REF: String = $$"$dynamicRef"
        public const val REF_PREFIX: String = "#/$DEFS/"
    }

    /**
     * Set of all known JSON Schema 2020-12 keywords.
     * Used to identify unknown keywords that should be treated as annotations.
     */
    public val KNOWN_KEYWORDS: Set<String> = setOf(
        // Core
        Keys.SCHEMA, Keys.ID, Keys.REF, Keys.DYNAMIC_REF, Keys.ANCHOR, Keys.DYNAMIC_ANCHOR, Keys.COMMENT, Keys.DEFS,
        // Metadata
        Keys.TITLE, Keys.DESCRIPTION, Keys.DEFAULT, Keys.EXAMPLES, Keys.DEPRECATED,
        Keys.READ_ONLY, Keys.WRITE_ONLY,
        // Type
        Keys.TYPE, Keys.ENUM, Keys.CONST,
        // Numeric
        Keys.MULTIPLE_OF, Keys.MINIMUM, Keys.MAXIMUM, Keys.EXCLUSIVE_MINIMUM, Keys.EXCLUSIVE_MAXIMUM,
        // String
        Keys.MIN_LENGTH, Keys.MAX_LENGTH, Keys.PATTERN, Keys.FORMAT,
        Keys.CONTENT_ENCODING, Keys.CONTENT_MEDIA_TYPE, Keys.CONTENT_SCHEMA,
        // Array
        Keys.ITEMS, Keys.PREFIX_ITEMS, Keys.UNEVALUATED_ITEMS, Keys.CONTAINS,
        Keys.MIN_CONTAINS, Keys.MAX_CONTAINS, Keys.MIN_ITEMS, Keys.MAX_ITEMS, Keys.UNIQUE_ITEMS,
        // Object
        Keys.PROPERTIES, Keys.PATTERN_PROPERTIES, Keys.ADDITIONAL_PROPERTIES,
        Keys.UNEVALUATED_PROPERTIES, Keys.PROPERTY_NAMES, Keys.REQUIRED,
        Keys.MIN_PROPERTIES, Keys.MAX_PROPERTIES,
        Keys.DEPENDENT_REQUIRED, Keys.DEPENDENT_SCHEMAS, Keys.DEPENDENCIES,
        // Composition
        Keys.ONE_OF, Keys.ANY_OF, Keys.ALL_OF, Keys.NOT,
        // Conditional
        Keys.IF, Keys.THEN, Keys.ELSE,
        // OpenAPI
        "discriminator", Keys.NULLABLE
    )

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
