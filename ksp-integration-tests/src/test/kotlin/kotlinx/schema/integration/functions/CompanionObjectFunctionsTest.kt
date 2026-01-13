package kotlinx.schema.integration.functions

import io.kotest.assertions.json.shouldEqualJson
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test

/**
 * Integration tests for companion object function schema generation.
 *
 * Tests verify that:
 * - Normal companion object functions generate correct schemas
 * - Suspend companion object functions generate correct schemas
 * - Multiple companion objects with functions can coexist
 * - Factory pattern functions work correctly
 */
class CompanionObjectFunctionsTest {
    @Test
    fun `DatabaseConnection create generates correct schema`() {
        val schemaString = createJsonSchemaString()
        val schema = createJsonSchema()

        schemaString shouldEqualJson Json.encodeToString(schema)

        val parsed = Json.parseToJsonElement(schemaString).jsonObject

        parsed["type"]?.jsonPrimitive?.content shouldBe "function"
        parsed["name"]?.jsonPrimitive?.content shouldBe "create"
        parsed["description"]?.jsonPrimitive?.content shouldContain "database connection"

        val properties = parsed["parameters"]?.jsonObject?.get("properties")?.jsonObject.shouldNotBeNull()
        properties["host"].shouldNotBeNull()
        properties["port"].shouldNotBeNull()
        properties["database"].shouldNotBeNull()
        properties["timeout"].shouldNotBeNull()
    }

    @Test
    fun `DatabaseConnection connectAsync suspend function generates correct schema`() {
        val schemaString = connectAsyncJsonSchemaString()
        val schema = connectAsyncJsonSchema()

        schemaString shouldEqualJson Json.encodeToString(schema)

        val parsed = Json.parseToJsonElement(schemaString).jsonObject

        parsed["type"]?.jsonPrimitive?.content shouldBe "function"
        parsed["name"]?.jsonPrimitive?.content shouldBe "connectAsync"
        parsed["description"]?.jsonPrimitive?.content shouldContain "asynchronously"

        val properties = parsed["parameters"]?.jsonObject?.get("properties")?.jsonObject.shouldNotBeNull()
        properties["host"].shouldNotBeNull()
        properties["port"].shouldNotBeNull()
        properties["username"].shouldNotBeNull()
        properties["password"].shouldNotBeNull()
        properties["options"].shouldNotBeNull()
    }

    @Test
    fun `ApiClient build generates correct schema`() {
        val schemaString = buildJsonSchemaString()
        val schema = buildJsonSchema()

        schemaString shouldEqualJson Json.encodeToString(schema)

        val parsed = Json.parseToJsonElement(schemaString).jsonObject

        parsed["name"]?.jsonPrimitive?.content shouldBe "build"
        parsed["description"]?.jsonPrimitive?.content shouldContain "API client"

        val properties = parsed["parameters"]?.jsonObject?.get("properties")?.jsonObject.shouldNotBeNull()
        properties["baseUrl"].shouldNotBeNull()
        properties["apiKey"].shouldNotBeNull()
        properties["timeout"].shouldNotBeNull()
        properties["debug"].shouldNotBeNull()
        properties["headers"].shouldNotBeNull()
    }

    @Test
    fun `ApiClient initializeAsync suspend function generates correct schema`() {
        val schemaString = initializeAsyncJsonSchemaString()
        val schema = initializeAsyncJsonSchema()

        schemaString shouldEqualJson Json.encodeToString(schema)

        val parsed = Json.parseToJsonElement(schemaString).jsonObject

        parsed["name"]?.jsonPrimitive?.content shouldBe "initializeAsync"
        parsed["description"]?.jsonPrimitive?.content shouldContain "asynchronously"

        val properties = parsed["parameters"]?.jsonObject?.get("properties")?.jsonObject.shouldNotBeNull()
        properties["baseUrl"].shouldNotBeNull()
        properties["apiKey"].shouldNotBeNull()
        properties["verifySSL"].shouldNotBeNull()
    }

    @Test
    fun `DataValidator validateAsync suspend function generates correct schema`() {
        val schemaString = validateAsyncJsonSchemaString()
        val schema = validateAsyncJsonSchema()

        schemaString shouldEqualJson Json.encodeToString(schema)

        val parsed = Json.parseToJsonElement(schemaString).jsonObject

        parsed["name"]?.jsonPrimitive?.content shouldBe "validateAsync"
        parsed["description"]?.jsonPrimitive?.content shouldContain "asynchronously"

        val properties = parsed["parameters"]?.jsonObject?.get("properties")?.jsonObject.shouldNotBeNull()
        properties["data"].shouldNotBeNull()
        properties["endpoints"].shouldNotBeNull()
        properties["timeout"].shouldNotBeNull()
    }

    @Test
    fun `all companion object function schemas have correct structure`() {
        val schemas = listOf(
            createJsonSchemaString(),
            testConnectionJsonSchemaString(),
            connectAsyncJsonSchemaString(),
            closeAllConnectionsJsonSchemaString(),
            buildJsonSchemaString(),
            createDefaultJsonSchemaString(),
            initializeAsyncJsonSchemaString(),
            discoverEndpointsJsonSchemaString(),
            validateEmailJsonSchemaString(),
            validateJsonSchemaString(),
            validateAsyncJsonSchemaString(),
            batchValidateJsonSchemaString(),
        )

        schemas.forEach { schemaString ->
            val parsed = Json.parseToJsonElement(schemaString).jsonObject

            // Verify FunctionCallingSchema structure
            parsed["type"].shouldNotBeNull()
            parsed["name"].shouldNotBeNull()
            parsed["parameters"].shouldNotBeNull()

            parsed["type"]?.jsonPrimitive?.content shouldBe "function"

            // Verify parameters object
            val parameters = parsed["parameters"]?.jsonObject.shouldNotBeNull()
            parameters["type"].shouldNotBeNull()
            parameters["properties"].shouldNotBeNull()
            parameters["required"].shouldNotBeNull()

            parameters["type"]?.jsonPrimitive?.content shouldBe "object"
        }
    }
}
