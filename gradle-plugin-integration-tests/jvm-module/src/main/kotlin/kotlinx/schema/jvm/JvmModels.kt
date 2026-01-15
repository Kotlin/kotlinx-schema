package kotlinx.schema.jvm

import kotlinx.schema.Description
import kotlinx.schema.Schema

/**
 * Simple JVM-only model for testing
 */
@Schema
data class JvmPerson(
    @Description("Full name")
    val name: String,
    @Description("Email address")
    val email: String,
)

@Schema
data class JvmCompany(
    @Description("Company name")
    val name: String,
    @Description("Number of employees")
    val employeeCount: Int,
)
