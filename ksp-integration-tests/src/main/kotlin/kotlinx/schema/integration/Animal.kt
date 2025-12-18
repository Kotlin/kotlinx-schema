package kotlinx.schema.integration

import kotlinx.schema.Schema

/**
 * Multicellular eukaryotic organism of the kingdom Metazoa
 */
@Schema
public sealed class Animal {
    /**
     * Animal's name
     */
    abstract val name: String

    @Schema(withSchemaObject = true)
    public data class Dog(
        override val name: String,
    ) : Animal()

    @Schema(withSchemaObject = true)
    public data class Cat(
        override val name: String,
    ) : Animal()
}
