package kotlinx.schema.generator.reflect

import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import jakarta.validation.constraints.Min
import kotlinx.schema.generator.core.ir.ObjectNode
import kotlinx.schema.generator.core.ir.TypeRef
import kotlinx.schema.generator.core.ir.Annotation as IrAnnotation
import kotlin.test.Test

class JakartaValidationIntrospectionTest {

    private val introspector = ReflectionClassIntrospector

    data class Sample(
        @field:Min(5)
        val age: Int
    )

    @Test
    fun `min annotation is extracted into property metadata`() {
        val graph = introspector.introspect(Sample::class)

        val root = graph.root
        root.shouldNotBeNull()

        val inline = root as? TypeRef.Ref
        inline.shouldNotBeNull()

        val objectNode = graph.nodes[inline.id] as? ObjectNode
        objectNode.shouldNotBeNull()

        val ageProperty = objectNode.properties.firstOrNull { it.name == "age" }
        ageProperty.shouldNotBeNull()

        val minConstraint = ageProperty.constraints.filterIsInstance<IrAnnotation.Min>().firstOrNull()
        minConstraint.shouldNotBeNull()
        minConstraint.value shouldBe 5L
    }
}