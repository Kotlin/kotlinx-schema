package kotlinx.schema.generator.core.ir

/** A facade for building [TypeGraph] from a particular source
 * (e.g. KSP or Serialization).
 *
 * Architecture Pattern for Introspector Implementations:
 *
 * 1. Create a context class extending `BaseIntrospectionContext<TDecl, TType>`
 * 2. Implement `toRef()` or `convertToTypeRef()` as main type conversion entry point
 * 3. Implement handlers for: primitives, collections, enums, sealed, objects
 * 4. Create introspector objects implementing `SchemaIntrospector<T>`
 * 5. Introspectors instantiate context and call `toRef()` / `convertToTypeRef()`
 * 6. Return TypeGraph(root, context.nodes)
 */
public interface SchemaIntrospector<T> {
    public fun introspect(root: T): TypeGraph
}
