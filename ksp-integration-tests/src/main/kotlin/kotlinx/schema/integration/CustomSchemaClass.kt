package kotlinx.schema.integration

import kotlinx.schema.Description
import kotlinx.schema.Schema

/**
 * Class with custom schema annotation parameter
 */
@Description("A class using a custom schema type value.")
@Schema("custom-schema")
data class CustomSchemaClass(
    @Description("A field included to validate custom schema handling")
    val customField: String,
)
