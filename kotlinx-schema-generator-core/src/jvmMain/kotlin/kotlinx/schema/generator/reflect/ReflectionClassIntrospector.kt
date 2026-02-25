package kotlinx.schema.generator.reflect

import kotlinx.schema.generator.core.ir.SchemaIntrospector
import kotlinx.schema.generator.core.ir.TypeGraph
import kotlin.reflect.KClass
import kotlin.reflect.full.createType

/**
 * Introspects Kotlin classes using reflection to build a [TypeGraph].
 *
 * This introspector analyzes class structures including properties, constructors,
 * and type hierarchies to generate schema IR nodes.
 *
 *  ## Example
 *  ```kotlin
 *  val typeGraph = ReflectionClassIntrospector.introspect(MyClass::class)
 *  ```
 *
 * ## Limitations
 * - Requires classes to have a primary constructor
 * - Type parameters are not fully supported
 */
public object ReflectionClassIntrospector : SchemaIntrospector<KClass<*>, Unit> {
    override val config: Unit = Unit

    override fun introspect(root: KClass<*>): TypeGraph {
        val context = ReflectionIntrospectionContext()
        val rootRef = context.toRef(root.createType())
        return TypeGraph(root = rootRef, nodes = context.nodes)
    }
}
