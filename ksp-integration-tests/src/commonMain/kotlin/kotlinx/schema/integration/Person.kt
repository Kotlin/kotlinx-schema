package kotlinx.schema.integration

import kotlinx.schema.Description
import kotlinx.schema.Schema

/**
 * Simple test model to verify basic KSP processing
 */
@Description("A person with a first and last name and age.")
@Schema(withSchemaObject = true)
data class Person(
    @Description("Given name of the person")
    val firstName: String,
    @Description("Family name of the person")
    val lastName: String,
    @Description("Age of the person in years")
    val age: Int,
)
