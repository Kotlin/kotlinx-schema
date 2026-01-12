package kotlinx.schema.integration.type

import kotlinx.schema.Description
import kotlinx.schema.Schema

/**
 * Multicellular eukaryotic organism of the kingdom Metazoa
 */
@Description("Multicellular eukaryotic organism of the kingdom Metazoa")
@Schema
sealed class Animal {
    /**
     * Animal's name
     */
    @Description("Animal's name")
    abstract val name: String

    @Schema(withSchemaObject = true)
    data class Dog(
        @Description("Animal's name")
        override val name: String,
    ) : Animal()

    @Schema(withSchemaObject = true)
    data class Cat(
        @Description("Animal's name")
        override val name: String,
    ) : Animal()
}
