package kotlinx.schema.generator.reflect

import jakarta.validation.constraints.Min
import kotlinx.schema.generator.core.ir.ObjectNode
import kotlinx.schema.generator.core.ir.TypeRef
import kotlinx.schema.generator.core.ir.Annotation as IrAnnotation
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertEquals

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
        assertNotNull(root)

        val inline = root as? TypeRef.Ref
        assertNotNull(inline)

        val objectNode = graph.nodes[inline.id] as? ObjectNode
        assertNotNull(objectNode)

        val ageProperty = objectNode.properties.firstOrNull { it.name == "age" }
        assertNotNull(ageProperty)

        val minConstraint = ageProperty.constraints.filterIsInstance<IrAnnotation.Min>().firstOrNull()
        assertNotNull(minConstraint)
        assertEquals(5L, minConstraint.value)
    }
}