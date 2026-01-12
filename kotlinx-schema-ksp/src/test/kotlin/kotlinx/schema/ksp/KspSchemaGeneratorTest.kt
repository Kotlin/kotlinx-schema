package kotlinx.schema.ksp

import com.google.devtools.ksp.symbol.KSCallableReference
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import io.kotest.matchers.collections.shouldHaveAtLeastSize
import io.kotest.matchers.nulls.shouldNotBeNull
import kotlinx.schema.generator.core.SchemaGeneratorService
import kotlin.test.Test
import kotlin.test.assertTrue

class KspSchemaGeneratorTest {
    @Test
    fun `Should register KspSchemaGenerator`() {
        SchemaGeneratorService.registeredGenerators() shouldHaveAtLeastSize 2
        val classGenerator =
            SchemaGeneratorService.getGenerator<KSClassDeclaration, Any>(targetType = KSClassDeclaration::class)
        classGenerator shouldNotBeNull {
            assertTrue(this is KspClassSchemaGenerator)
        }

        val typeGenerator =
            SchemaGeneratorService.getGenerator<KSFunctionDeclaration, Any>(targetType = KSFunctionDeclaration::class)
        typeGenerator shouldNotBeNull {
            assertTrue(this is KspFunctionSchemaGenerator)
        }
    }
}
