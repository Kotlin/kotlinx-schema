package kotlinx.schema.json

import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Helper function to decode JSON and validate a property.
 */
inline fun <reified T : PropertyDefinition> decodeAndValidate(
    json: kotlinx.serialization.json.Json,
    jsonString: String,
    validation: T.() -> Unit,
): T {
    val decoded = json.decodeFromString<T>(jsonString)
    decoded.validation()
    return decoded
}

/**
 * Helper function to create a jsonSchema with a single property for testing.
 */
fun testSchemaWithProperty(
    propertyName: String = "testProp",
    propertyBuilder: PropertyBuilder.() -> PropertyDefinition,
): JsonSchema =
    jsonSchema {
        name = "TestSchema"
        schema {
            property(propertyName, block = propertyBuilder)
        }
    }

/**
 * Helper function to extract and validate the first property from a schema.
 */
inline fun <reified T : PropertyDefinition> JsonSchema.firstPropertyAs(): T {
    val prop = schema.properties.shouldNotBeNull().values.first()
    return prop as T
}

/**
 * Helper function to validate enum values.
 */
fun <T> List<T>?.shouldContainExactly(vararg expected: T) {
    this.shouldNotBeNull()
    this shouldBe expected.toList()
}
