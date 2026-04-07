package kotlinx.schema.generator.json.serialization

import io.kotest.assertions.json.shouldEqualJson
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.test.Test

class SerialNameAnnotationTest {
    //region Test models

    @Serializable
    @SerialName("PersonRecord")
    data class PersonWithRenamedProperties(
        val id: Int,
        @SerialName("user_name") val userName: String,
        @SerialName("email_address") val emailAddress: String = "n/a",
    )

    @Serializable
    @SerialName("Priority")
    @Suppress("unused")
    enum class PriorityWithRenamedEntries {
        @SerialName("p0_critical")
        CRITICAL,

        @SerialName("p1_high")
        HIGH,

        @SerialName("p2_low")
        LOW,
    }

    @Serializable
    @SerialName("TaskWithPriority")
    data class TaskWithPriority(
        val title: String,
        val priority: PriorityWithRenamedEntries,
    )

    //endregion

    private val generator = SerializationClassJsonSchemaGenerator()

    @Test
    fun `SerialName on properties produces custom keys in properties and required`() {
        val schema = generator.generateSchemaString(PersonWithRenamedProperties.serializer().descriptor)

        schema shouldEqualJson
            // language=JSON
            $$"""
            {
              "$schema": "https://json-schema.org/draft/2020-12/schema",
              "$id": "PersonRecord",
              "type": "object",
              "properties": {
                "id": { "type": "integer" },
                "user_name": { "type": "string" },
                "email_address": { "type": "string" }
              },
              "required": ["id", "user_name"],
              "additionalProperties": false
            }
            """.trimIndent()
    }

    @Test
    fun `SerialName on enum entries produces custom values in enum array`() {
        val schema = generator.generateSchemaString(PriorityWithRenamedEntries.serializer().descriptor)

        schema shouldEqualJson
            // language=JSON
            $$"""
            {
              "$schema": "https://json-schema.org/draft/2020-12/schema",
              "$id": "Priority",
              "type": "string",
              "enum": ["p0_critical", "p1_high", "p2_low"]
            }
            """.trimIndent()
    }

    @Test
    fun `SerialName on enum class affects defs key and ref target`() {
        val schema = generator.generateSchemaString(TaskWithPriority.serializer().descriptor)

        schema shouldEqualJson
            // language=JSON
            $$"""
            {
              "$schema": "https://json-schema.org/draft/2020-12/schema",
              "$id": "TaskWithPriority",
              "type": "object",
              "properties": {
                "title": { "type": "string" },
                "priority": { "$ref": "#/$defs/Priority" }
              },
              "required": ["title", "priority"],
              "additionalProperties": false,
              "$defs": {
                "Priority": {
                  "type": "string",
                  "enum": ["p0_critical", "p1_high", "p2_low"]
                }
              }
            }
            """.trimIndent()
    }
}
