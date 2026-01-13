package kotlinx.schema.integration.functions

import io.kotest.assertions.json.shouldEqualJson
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test

/**
 * Integration tests for top-level function schema generation.
 *
 * Tests verify that:
 * - Normal top-level functions generate correct schemas
 * - Suspend top-level functions generate correct schemas
 * - Function names, descriptions, and parameters are correctly captured
 * - Default values are properly indicated
 * - Nullable parameters are correctly represented
 */
class TopLevelFunctionsTest {
    @Test
    fun `greetPerson generates correct function schema`() {
        val schemaString = greetPersonJsonSchemaString()
        val schema = greetPersonJsonSchema()

        // Verify schema can be decoded
        schemaString shouldEqualJson Json.encodeToString(schema)

        // Parse and verify structure
        val parsed = Json.parseToJsonElement(schemaString).jsonObject

        // Verify function metadata
        parsed["type"]?.jsonPrimitive?.content shouldBe "function"
        parsed["name"]?.jsonPrimitive?.content shouldBe "greetPerson"
        parsed["description"]?.jsonPrimitive?.content shouldContain "greeting message"

        // Verify parameters
        val parameters = parsed["parameters"]?.jsonObject.shouldNotBeNull()
        parameters["type"]?.jsonPrimitive?.content shouldBe "object"

        val properties = parameters["properties"]?.jsonObject.shouldNotBeNull()
        properties["name"].shouldNotBeNull()
        properties["greeting"].shouldNotBeNull()

        // Verify required parameters (those without defaults)
        val required = parameters["required"]?.jsonArray.shouldNotBeNull()
        required.map { it.jsonPrimitive.content } shouldContain "name"
    }

    @Test
    fun `calculateSum generates correct function schema`() {
        val schemaString = calculateSumJsonSchemaString()
        val schema = calculateSumJsonSchema()

        schemaString shouldEqualJson Json.encodeToString(schema)

        val parsed = Json.parseToJsonElement(schemaString).jsonObject

        parsed["name"]?.jsonPrimitive?.content shouldBe "calculateSum"
        parsed["description"]?.jsonPrimitive?.content shouldContain "sum of two numbers"

        val properties = parsed["parameters"]?.jsonObject?.get("properties")?.jsonObject.shouldNotBeNull()
        properties["a"].shouldNotBeNull()
        properties["b"].shouldNotBeNull()

        // Both parameters are required (no defaults)
        val required = parsed["parameters"]?.jsonObject?.get("required")?.jsonArray.shouldNotBeNull()
        required.map { it.jsonPrimitive.content } shouldContain "a"
        required.map { it.jsonPrimitive.content } shouldContain "b"
    }

    @Test
    fun `fetchUserData suspend function generates correct schema`() {
        val schemaString = fetchUserDataJsonSchemaString()
        val schema = fetchUserDataJsonSchema()

        schemaString shouldEqualJson Json.encodeToString(schema)

        val parsed = Json.parseToJsonElement(schemaString).jsonObject

        // Suspend functions should generate same schema as normal functions
        parsed["type"]?.jsonPrimitive?.content shouldBe "function"
        parsed["name"]?.jsonPrimitive?.content shouldBe "fetchUserData"
        parsed["description"]?.jsonPrimitive?.content shouldContain "asynchronously"

        val properties = parsed["parameters"]?.jsonObject?.get("properties")?.jsonObject.shouldNotBeNull()
        properties["userId"].shouldNotBeNull()
        properties["includeDetails"].shouldNotBeNull()

        // Only userId is required (includeDetails has default)
        val required = parsed["parameters"]?.jsonObject?.get("required")?.jsonArray.shouldNotBeNull()
        required.map { it.jsonPrimitive.content } shouldContain "userId"
    }

    @Test
    fun `processItems suspend function generates correct schema`() {
        val schemaString = processItemsJsonSchemaString()
        val schema = processItemsJsonSchema()

        schemaString shouldEqualJson Json.encodeToString(schema)

        val parsed = Json.parseToJsonElement(schemaString).jsonObject

        parsed["name"]?.jsonPrimitive?.content shouldBe "processItems"

        val properties = parsed["parameters"]?.jsonObject?.get("properties")?.jsonObject.shouldNotBeNull()
        properties["items"].shouldNotBeNull()
        properties["mode"].shouldNotBeNull()
    }

    @Test
    fun `searchProducts with nullable parameters generates correct schema`() {
        val schemaString = searchProductsJsonSchemaString()
        val schema = searchProductsJsonSchema()

        schemaString shouldEqualJson Json.encodeToString(schema)

        val parsed = Json.parseToJsonElement(schemaString).jsonObject

        parsed["name"]?.jsonPrimitive?.content shouldBe "searchProducts"

        val properties = parsed["parameters"]?.jsonObject?.get("properties")?.jsonObject.shouldNotBeNull()
        properties["query"].shouldNotBeNull()
        properties["minPrice"].shouldNotBeNull()
        properties["maxPrice"].shouldNotBeNull()
        properties["categories"].shouldNotBeNull()
        properties["includeOutOfStock"].shouldNotBeNull()

        // Only query is required (others have defaults or are nullable)
        val required = parsed["parameters"]?.jsonObject?.get("required")?.jsonArray.shouldNotBeNull()
        required.map { it.jsonPrimitive.content } shouldContain "query"
    }

    @Test
    fun `updateConfiguration suspend function with map parameter generates correct schema`() {
        val schemaString = updateConfigurationJsonSchemaString()
        val schema = updateConfigurationJsonSchema()

        schemaString shouldEqualJson Json.encodeToString(schema)

        val parsed = Json.parseToJsonElement(schemaString).jsonObject

        parsed["name"]?.jsonPrimitive?.content shouldBe "updateConfiguration"
        parsed["description"]?.jsonPrimitive?.content shouldContain "configuration"

        val properties = parsed["parameters"]?.jsonObject?.get("properties")?.jsonObject.shouldNotBeNull()
        properties["key"].shouldNotBeNull()
        properties["value"].shouldNotBeNull()
        properties["metadata"].shouldNotBeNull()

        // key and value are required, metadata is nullable
        val required = parsed["parameters"]?.jsonObject?.get("required")?.jsonArray.shouldNotBeNull()
        required.map { it.jsonPrimitive.content } shouldContain "key"
        required.map { it.jsonPrimitive.content } shouldContain "value"
    }

    @Test
    fun `all generated schemas have FunctionCallingSchema structure`() {
        val schemas = listOf(
            greetPersonJsonSchemaString(),
            calculateSumJsonSchemaString(),
            fetchUserDataJsonSchemaString(),
            processItemsJsonSchemaString(),
            searchProductsJsonSchemaString(),
            updateConfigurationJsonSchemaString(),
        )

        schemas.forEach { schemaString ->
            val parsed = Json.parseToJsonElement(schemaString).jsonObject

            // All schemas should have these required fields
            parsed["type"].shouldNotBeNull()
            parsed["name"].shouldNotBeNull()
            parsed["parameters"].shouldNotBeNull()

            // Type should be "function"
            parsed["type"]?.jsonPrimitive?.content shouldBe "function"

            // Parameters should be an object with properties and required
            val parameters = parsed["parameters"]?.jsonObject.shouldNotBeNull()
            parameters["type"].shouldNotBeNull()
            parameters["properties"].shouldNotBeNull()
            parameters["required"].shouldNotBeNull()
        }
    }
}
