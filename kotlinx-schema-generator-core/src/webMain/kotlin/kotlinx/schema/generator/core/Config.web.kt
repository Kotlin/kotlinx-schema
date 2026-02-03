package kotlinx.schema.generator.core

internal actual object Config {
    actual val descriptionAnnotationNames: Set<String>
        get() = setOf("Description")
    actual val descriptionValueAttributes: Set<String>
        get() = setOf("value", "description")
}
