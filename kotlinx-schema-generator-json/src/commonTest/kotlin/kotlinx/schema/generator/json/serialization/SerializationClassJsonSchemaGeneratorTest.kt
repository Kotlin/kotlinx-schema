package kotlinx.schema.generator.json.serialization

import io.kotest.assertions.json.shouldEqualJson
import kotlinx.schema.json.encodeToString
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.test.Test

class SerializationClassJsonSchemaGeneratorTest {
    @Serializable
    data class Person(
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

    val generator = SerializationClassJsonSchemaGenerator()

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
