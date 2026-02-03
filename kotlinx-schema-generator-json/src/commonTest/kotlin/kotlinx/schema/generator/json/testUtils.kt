package kotlinx.schema.generator.json

import io.kotest.assertions.json.shouldEqualJson
import io.kotest.matchers.nulls.shouldNotBeNull
import kotlinx.schema.json.JsonSchema
import kotlinx.schema.json.encodeToString
import kotlinx.serialization.json.Json
import org.intellij.lang.annotations.Language
import kotlin.test.fail

internal val json =
    Json {
        prettyPrint = true
        ignoreUnknownKeys = false
        prettyPrintIndent = "  "
    }

internal fun verifySchema(
    actualSchema: JsonSchema,
    @Language("json") expectedSchema: String,
) {
    val schemaString = actualSchema.encodeToString(json)

    schemaString shouldEqualJson expectedSchema

    try {
        val parsedSchema: JsonSchema = json.decodeFromString(schemaString)
        parsedSchema.shouldNotBeNull()
    } catch (e: Exception) {
        fail("Failed to parse generated schema: ${e.message}. Schema:\n$actualSchema", e)
    }
}
