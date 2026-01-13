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
 * Integration tests for instance (member) function schema generation.
 *
 * Tests verify that:
 * - Normal instance functions generate correct schemas
 * - Suspend instance functions generate correct schemas
 * - Schemas don't include implicit 'this' parameter
 * - Multiple instance functions can coexist in same class
 */
class InstanceFunctionsTest {
    @Test
    fun `UserService registerUser generates correct schema`() {
        val schemaString = registerUserJsonSchemaString()
        val schema = registerUserJsonSchema()

        schemaString shouldEqualJson Json.encodeToString(schema)

        val parsed = Json.parseToJsonElement(schemaString).jsonObject

        parsed["type"]?.jsonPrimitive?.content shouldBe "function"
        parsed["name"]?.jsonPrimitive?.content shouldBe "registerUser"
        parsed["description"]?.jsonPrimitive?.content shouldContain "Registers a new user"

        val properties =
            parsed["parameters"]
                ?.jsonObject
                ?.get("properties")
                ?.jsonObject
                .shouldNotBeNull()
        properties["username"].shouldNotBeNull()
        properties["email"].shouldNotBeNull()
        properties["age"].shouldNotBeNull()
        properties["sendWelcomeEmail"].shouldNotBeNull()
    }

    @Test
    fun `UserService authenticateUser suspend function generates correct schema`() {
        val schemaString = authenticateUserJsonSchemaString()
        val schema = authenticateUserJsonSchema()

        schemaString shouldEqualJson Json.encodeToString(schema)

        val parsed = Json.parseToJsonElement(schemaString).jsonObject

        parsed["type"]?.jsonPrimitive?.content shouldBe "function"
        parsed["name"]?.jsonPrimitive?.content shouldBe "authenticateUser"
        parsed["description"]?.jsonPrimitive?.content shouldContain "asynchronously"

        val properties =
            parsed["parameters"]
                ?.jsonObject
                ?.get("properties")
                ?.jsonObject
                .shouldNotBeNull()
        properties["identifier"].shouldNotBeNull()
        properties["password"].shouldNotBeNull()
        properties["rememberMe"].shouldNotBeNull()
    }

    @Test
    fun `ProductRepository saveProduct suspend function generates correct schema`() {
        val schemaString = saveProductJsonSchemaString()
        val schema = saveProductJsonSchema()

        schemaString shouldEqualJson Json.encodeToString(schema)

        val parsed = Json.parseToJsonElement(schemaString).jsonObject

        parsed["name"]?.jsonPrimitive?.content shouldBe "saveProduct"
        parsed["description"]?.jsonPrimitive?.content shouldContain "asynchronously"

        val properties =
            parsed["parameters"]
                ?.jsonObject
                ?.get("properties")
                ?.jsonObject
                .shouldNotBeNull()
        properties["id"].shouldNotBeNull()
        properties["name"].shouldNotBeNull()
        properties["price"].shouldNotBeNull()
        properties["stock"].shouldNotBeNull()
    }

    @Test
    fun `all instance function schemas have correct structure`() {
        val schemas =
            listOf(
                registerUserJsonSchemaString(),
                updateProfileJsonSchemaString(),
                authenticateUserJsonSchemaString(),
                loadUserPreferencesJsonSchemaString(),
                deleteAccountJsonSchemaString(),
                sendBulkNotificationJsonSchemaString(),
                findByCategoryJsonSchemaString(),
                saveProductJsonSchemaString(),
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
