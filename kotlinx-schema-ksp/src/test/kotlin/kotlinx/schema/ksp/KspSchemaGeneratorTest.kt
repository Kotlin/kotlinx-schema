package kotlinx.schema.ksp

import com.google.devtools.ksp.symbol.KSClassDeclaration
import io.kotest.matchers.collections.shouldHaveAtLeastSize
import io.kotest.matchers.nulls.shouldNotBeNull
import kotlinx.schema.generator.core.SchemaGeneratorService
import kotlin.test.Test
import kotlin.test.assertTrue

class KspSchemaGeneratorTest {
    @Test
    fun `Should register KspSchemaGenerator`() {
        SchemaGeneratorService.registeredGenerators() shouldHaveAtLeastSize 1
        val generator =
            SchemaGeneratorService.getGenerator<KSClassDeclaration, Any>(targetType = KSClassDeclaration::class)
        generator shouldNotBeNull {
            assertTrue(this is KspClassSchemaGenerator)
        }
    }
}
