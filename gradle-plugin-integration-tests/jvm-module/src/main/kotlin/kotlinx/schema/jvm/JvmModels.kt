package kotlinx.schema.jvm

import kotlinx.schema.Description
import kotlinx.schema.Schema

/**
 * Simple Person model for testing
 */
@Schema
data class Person(
    @Description("Full name")
    val name: String,
    @Description("Email address")
    val email: String,
)

/**
 * Simple Company model for testing
 */
@Schema
data class Company(
    @Description("Company name")
    val name: String,
    @Description("Number of employees")
    val employeeCount: Int,
)
