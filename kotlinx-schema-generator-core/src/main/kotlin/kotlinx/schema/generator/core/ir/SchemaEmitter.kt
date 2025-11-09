package kotlinx.schema.generator.core.ir

/** Emitter that converts a [TypeGraph] to a target representation (e.g., JSON Schema). */
public interface SchemaEmitter<R> {
    public fun emit(
        graph: TypeGraph,
        rootName: String,
    ): R
}
