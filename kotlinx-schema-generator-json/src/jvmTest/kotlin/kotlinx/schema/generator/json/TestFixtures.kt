@file:Suppress("unused")

package kotlinx.schema.generator.json

import kotlinx.schema.Description

/*
 * Shared test data classes and fixtures used across multiple test files.
 * Consolidates common test models to avoid duplication.
 */

// Simple nested class for testing object properties
@Description("Nested object")
data class Address(
    @property:Description("Street name")
    val street: String,
    @property:Description("City name")
    val city: String,
)

// Comprehensive class with all basic types for testing
@Description("Person with various optional fields")
data class PersonWithOptionals(
    @property:Description("Name")
    val name: String,
    @property:Description("Age")
    val age: Int? = null,
    @property:Description("Email")
    val email: String? = null,
    @property:Description("Score")
    val score: Double? = null,
    @property:Description("Active")
    val active: Boolean? = null,
)

// Enum for testing enum handling
enum class Status {
    ACTIVE,
    INACTIVE,
    PENDING,
}

// Class with enum property
@Description("Class with enum")
data class WithEnum(
    @property:Description("Status")
    val status: Status,
    @property:Description("Optional status")
    val optStatus: Status? = null,
)

// Class with numeric types
@Description("Class with numeric types")
data class WithNumericTypes(
    @property:Description("Int value")
    val intVal: Int,
    @property:Description("Long value")
    val longVal: Long,
    @property:Description("Float value")
    val floatVal: Float,
    @property:Description("Double value")
    val doubleVal: Double,
    @property:Description("Nullable int")
    val nullableInt: Int? = null,
    @property:Description("Nullable long")
    val nullableLong: Long? = null,
    @property:Description("Nullable float")
    val nullableFloat: Float? = null,
    @property:Description("Nullable double")
    val nullableDouble: Double? = null,
)

// Class with collections
@Description("Class with collections")
data class WithCollections(
    @property:Description("String list")
    val items: List<String>,
    @property:Description("String to int map")
    val data: Map<String, Int>,
    @property:Description("Nullable list")
    val optList: List<String>? = null,
    @property:Description("Nullable map")
    val optMap: Map<String, String>? = null,
)

// Class with nested object
@Description("Class with nested object")
data class WithNested(
    @property:Description("Name")
    val name: String,
    @property:Description("Address")
    val address: Address,
    @property:Description("Optional address")
    val optAddress: Address? = null,
)

// Deeply nested structure
@Description("Level 3")
data class Level3(
    @property:Description("Value")
    val value: String,
)

@Description("Level 2")
data class Level2(
    @property:Description("Value")
    val value: Int,
    @property:Description("Level 3")
    val level3: Level3,
    @property:Description("Optional value")
    val optional: String? = null,
)

@Description("Level 1")
data class Level1(
    @property:Description("Level 2")
    val level2: Level2,
    @property:Description("Value")
    val value: String,
)

@Description("Deep nested structure")
data class DeepNested(
    @property:Description("Level 1")
    val level1: Level1,
)

// List of nested objects
@Description("List of nested")
data class ListOfNested(
    @property:Description("Items")
    val items: List<Address>,
    @property:Description("Optional items")
    val optionalItems: List<Address>? = null,
)

// Map of nested objects
@Description("Map of nested")
data class MapOfNested(
    @property:Description("Data")
    val data: Map<String, Address>,
    @property:Description("Optional data")
    val optionalData: Map<String, Address>? = null,
)

// Mixed required, optional, and default fields
@Description("Mixed required and optional")
data class MixedRequiredOptional(
    @property:Description("Required string")
    val req1: String,
    @property:Description("Required int")
    val req2: Int,
    @property:Description("Optional string")
    val opt1: String? = null,
    @property:Description("Optional int")
    val opt2: Int? = null,
    @property:Description("Default string")
    val def1: String = "default",
    @property:Description("Default int")
    val def2: Int = 42,
)

// Edge cases
@Description("Empty class")
data class EmptyClass(
    val dummy: String = "ignored",
)

@Description("Single required field")
data class SingleRequired(
    @property:Description("Only field")
    val value: String,
)

// Comprehensive class for treatNullableOptionalAsRequired testing
@Description("All types with nullable optionals")
data class AllTypesOptional(
    @property:Description("String")
    val str: String = "",
    @property:Description("Int")
    val num: Int? = null,
    @property:Description("Long")
    val longNum: Long? = null,
    @property:Description("Float")
    val floatNum: Float? = null,
    @property:Description("Double")
    val doubleNum: Double? = null,
    @property:Description("Bool")
    val flag: Boolean? = null,
    @property:Description("List")
    val items: List<String>? = null,
    @property:Description("Map")
    val data: Map<String, String>? = null,
    @property:Description("Object")
    val nested: Address? = null,
)
