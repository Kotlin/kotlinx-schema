package kotlinx.schema.generator.core

import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.matchers.types.shouldBeSameInstanceAs
import org.junit.jupiter.api.Test
import kotlin.reflect.KClass

class SchemaGeneratorServiceTest {
    class TestGenerator : SchemaGenerator<Int, String> {
        override fun targetType(): KClass<Int> = Int::class

        override fun schemaType(): KClass<String> = String::class

        override fun generateSchema(target: Int): String {
            error("Not needed for test")
        }

        override fun generateSchemaString(target: Int): String {
            error("Not needed for test")
        }

        override fun encodeToString(schema: String): String {
            error("Not needed for test")
        }
    }

    @Test
    fun `Should detect generators`() {
        val generators = SchemaGeneratorService.registeredGenerators()

        generators.shouldHaveSize(1)

        generators.map { it::class } shouldContain TestGenerator::class

        val generator = generators.single { it::class == TestGenerator::class } as TestGenerator
        generator.shouldBeInstanceOf<TestGenerator>()
    }

    @Test
    fun `Should get generator by type`() {
        val generator =
            SchemaGeneratorService
                .getGenerator(
                    targetType = Int::class,
                    schemaType = String::class,
                )
        generator.shouldBeInstanceOf<TestGenerator>()

        SchemaGeneratorService
            .getGenerator<Int, Any>(
                targetType = Int::class,
            ) shouldBeSameInstanceAs generator

        SchemaGeneratorService
            .getGenerator<Any, String>(
                schemaType = String::class,
            ) shouldBeSameInstanceAs generator
    }
}
