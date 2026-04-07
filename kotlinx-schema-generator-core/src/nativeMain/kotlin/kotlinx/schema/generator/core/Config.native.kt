package kotlinx.schema.generator.core

internal actual object Config {
    actual val descriptionAnnotationNames: List<String>
        get() = listOf("Description")
    actual val descriptionValueAttributes: List<String>
        get() = listOf("value", "description")
    actual val ignoreAnnotationNames: List<String>
        get() = listOf("schemaignore")
    actual val nameAnnotationNames: List<String>
        get() = listOf("kotlinx.serialization.SerialName")
    actual val nameValueAttributes: List<String>
        get() = listOf("value")
}
