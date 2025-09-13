package kotlinx.schema

@Schema
@Description("Personal information")
data class Person(
    @property:Description("Person's first name")
    val firstName: String,
)