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
 * Integration tests for object (singleton) function schema generation.
 *
 * Tests verify that:
 * - Normal object functions generate correct schemas
 * - Suspend object functions generate correct schemas
 * - Multiple objects with functions can coexist
 * - Object functions are accessible as top-level functions
 */
class ObjectFunctionsTest {
    @Test
    fun `ConfigurationManager loadConfig generates correct schema`() {
        val schemaString = loadConfigJsonSchemaString()
        val schema = loadConfigJsonSchema()

        schemaString shouldEqualJson Json.encodeToString(schema)

        val parsed = Json.parseToJsonElement(schemaString).jsonObject

        parsed["type"]?.jsonPrimitive?.content shouldBe "function"
        parsed["name"]?.jsonPrimitive?.content shouldBe "loadConfig"
        parsed["description"]?.jsonPrimitive?.content shouldContain "configuration from a file"

        val properties = parsed["parameters"]?.jsonObject?.get("properties")?.jsonObject.shouldNotBeNull()
        properties["filePath"].shouldNotBeNull()
        properties["createIfMissing"].shouldNotBeNull()
        properties["defaults"].shouldNotBeNull()
    }

    @Test
    fun `ConfigurationManager validateConfig suspend function generates correct schema`() {
        val schemaString = validateConfigJsonSchemaString()
        val schema = validateConfigJsonSchema()

        schemaString shouldEqualJson Json.encodeToString(schema)

        val parsed = Json.parseToJsonElement(schemaString).jsonObject

        parsed["type"]?.jsonPrimitive?.content shouldBe "function"
        parsed["name"]?.jsonPrimitive?.content shouldBe "validateConfig"
        parsed["description"]?.jsonPrimitive?.content shouldContain "asynchronously"

        val properties = parsed["parameters"]?.jsonObject?.get("properties")?.jsonObject.shouldNotBeNull()
        properties["config"].shouldNotBeNull()
        properties["schemaVersion"].shouldNotBeNull()
    }

    @Test
    fun `LogManager writeLog generates correct schema`() {
        val schemaString = writeLogJsonSchemaString()
        val schema = writeLogJsonSchema()

        schemaString shouldEqualJson Json.encodeToString(schema)

        val parsed = Json.parseToJsonElement(schemaString).jsonObject

        parsed["name"]?.jsonPrimitive?.content shouldBe "writeLog"
        parsed["description"]?.jsonPrimitive?.content shouldContain "log entry"

        val properties = parsed["parameters"]?.jsonObject?.get("properties")?.jsonObject.shouldNotBeNull()
        properties["level"].shouldNotBeNull()
        properties["message"].shouldNotBeNull()
        properties["exception"].shouldNotBeNull()
        properties["tags"].shouldNotBeNull()
    }

    @Test
    fun `LogManager archiveLogs suspend function generates correct schema`() {
        val schemaString = archiveLogsJsonSchemaString()
        val schema = archiveLogsJsonSchema()

        schemaString shouldEqualJson Json.encodeToString(schema)

        val parsed = Json.parseToJsonElement(schemaString).jsonObject

        parsed["name"]?.jsonPrimitive?.content shouldBe "archiveLogs"
        parsed["description"]?.jsonPrimitive?.content shouldContain "asynchronously"

        val properties = parsed["parameters"]?.jsonObject?.get("properties")?.jsonObject.shouldNotBeNull()
        properties["beforeTimestamp"].shouldNotBeNull()
        properties["compressionFormat"].shouldNotBeNull()
    }

    @Test
    fun `all object function schemas have correct structure`() {
        val schemas = listOf(
            loadConfigJsonSchemaString(),
            saveConfigJsonSchemaString(),
            validateConfigJsonSchemaString(),
            reloadConfigurationJsonSchemaString(),
            writeLogJsonSchemaString(),
            flushLogsJsonSchemaString(),
            queryLogsJsonSchemaString(),
            archiveLogsJsonSchemaString(),
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
