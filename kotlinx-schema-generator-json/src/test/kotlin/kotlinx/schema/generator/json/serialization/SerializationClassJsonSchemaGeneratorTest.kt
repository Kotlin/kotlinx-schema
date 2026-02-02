package kotlinx.schema.generator.json.serialization

import io.kotest.assertions.json.shouldEqualJson
import kotlinx.schema.Description
import kotlinx.schema.generator.core.SchemaGeneratorService
import kotlinx.schema.json.JsonSchema
import kotlinx.schema.json.encodeToString
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.json.Json
import kotlin.test.Test

class SerializationClassJsonSchemaGeneratorTest {
    @Serializable
    @Description("Personal information")
    data class Person(
        @property:Description("Person's first name")
        val firstName: String,
    )

    private val expectedPersonSchemaString =
        // language=json
        $$"""
          {
         "$schema": "https://json-schema.org/draft/2020-12/schema",
         "$id": "kotlinx.schema.generator.json.serialization.SerializationClassJsonSchemaGeneratorTest.Person",
         "required": [ "firstName" ],
          "type": "object",
          "properties": {
            "firstName": {
              "type": "string"
            }
          },
          "additionalProperties": false
        }
        """

    val generator =
        SchemaGeneratorService.getGenerator(SerialDescriptor::class, JsonSchema::class)
            ?: error(
                "SerializationClassJsonSchemaGenerator must be registered",
            )

    @Test
    fun `Should generate JsonSchema from SerialDescriptor`() {
        val schema =
            generator.generateSchema(
                Person.serializer().descriptor,
            )

        val actualSchemaJson = schema.encodeToString(Json)

        actualSchemaJson shouldEqualJson expectedPersonSchemaString
    }
}
