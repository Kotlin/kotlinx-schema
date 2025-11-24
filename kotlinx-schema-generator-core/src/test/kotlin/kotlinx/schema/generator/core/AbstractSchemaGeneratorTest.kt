package kotlinx.schema.generator.core

import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import kotlinx.schema.generator.core.ir.SchemaIntrospector
import kotlinx.schema.generator.core.ir.TypeGraph
import kotlinx.schema.generator.core.ir.TypeGraphTransformer
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import kotlin.reflect.KClass

@ExtendWith(MockKExtension::class)
class AbstractSchemaGeneratorTest {
    @MockK
    private lateinit var introspector: SchemaIntrospector<KClass<*>>

    @MockK
    private lateinit var emitter: TypeGraphTransformer<Map<String, String>>

    @MockK
    private lateinit var typeGraph: TypeGraph

    private lateinit var generator: AbstractSchemaGenerator<KClass<*>, Map<String, String>>

    private val rootName = AbstractSchemaGeneratorTest::class.qualifiedName!!

    @BeforeEach
    fun setUp() {
        generator =
            object : AbstractSchemaGenerator<KClass<*>, Map<String, String>>(introspector, emitter) {
                override fun getRootName(target: KClass<*>): String = target.qualifiedName!!

                override fun targetType(): KClass<KClass<*>> = KClass::class

                @Suppress("UNCHECKED_CAST")
                override fun schemaType(): KClass<Map<String, String>> =
                    mapOf<String, String>()::class as KClass<Map<String, String>>

                override fun encodeToString(schema: Map<String, String>): String = "Schema: $schema"
            }
    }

    @Test
    fun generateSchema() {
        every { introspector.introspect(AbstractSchemaGeneratorTest::class) } returns typeGraph
        val expectedSchema = mapOf("schema" to "{}")
        every { emitter.transform(typeGraph, rootName) } returns expectedSchema

        val result = generator.generateSchema(AbstractSchemaGeneratorTest::class)

        result shouldBe expectedSchema
    }

    @Test
    fun generateSchemaString() {
        every { introspector.introspect(AbstractSchemaGeneratorTest::class) } returns typeGraph
        val expectedSchema = mapOf("schema" to "{}")
        every { emitter.transform(typeGraph, rootName) } returns expectedSchema

        val result = generator.generateSchemaString(AbstractSchemaGeneratorTest::class)

        result shouldBe "Schema: $expectedSchema"
    }
}
