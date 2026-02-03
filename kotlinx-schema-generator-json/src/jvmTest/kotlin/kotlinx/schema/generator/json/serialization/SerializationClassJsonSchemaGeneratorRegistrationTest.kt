package kotlinx.schema.generator.json.serialization

import io.kotest.matchers.nulls.shouldNotBeNull
import kotlinx.schema.generator.core.SchemaGeneratorService
import kotlinx.schema.json.JsonSchema
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlin.test.Test

class SerializationClassJsonSchemaGeneratorRegistrationTest {
    @Test
    fun `Should register SerializationClassJsonSchemaGenerator`() {
        SchemaGeneratorService
            .getGenerator(SerialDescriptor::class, JsonSchema::class)
            .shouldNotBeNull()
    }
}
