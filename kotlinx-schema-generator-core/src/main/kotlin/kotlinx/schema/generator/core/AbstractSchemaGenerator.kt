package kotlinx.schema.generator.core

import kotlinx.schema.generator.core.ir.SchemaEmitter
import kotlinx.schema.generator.core.ir.SchemaIntrospector

/**
 * Abstract base class for generating schemas by combining introspection and representation logic.
 *
 * @param T the type of the object for which the schema is being generated
 * @param R the type of the resulting schema representation
 * @property introspector the component responsible for introspecting the input and producing a type graph
 * @property emitter the component responsible for converting the type graph into the desired schema representation
 */
public abstract class AbstractSchemaGenerator<T : Any, R : Any>(
    protected val introspector: SchemaIntrospector<T>,
    protected val emitter: SchemaEmitter<R>,
) : SchemaGenerator<T, R> {
    protected abstract fun getRootName(target: T): String

    override fun generateSchema(target: T): R {
        val graph = introspector.introspect(target)

        return emitter.emit(graph, getRootName(target))
    }

    override fun generateSchemaString(target: T): String {
        val jsonObject = generateSchema(target)
        return encodeToString(jsonObject)
    }
}
