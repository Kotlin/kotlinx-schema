package kotlinx.schema.generator.core.ir

/** A facade for building [TypeGraph] from a particular source (e.g. KSP or Serialization). */
public interface SchemaIntrospector<T> {
    public fun introspect(root: T): TypeGraph
}
