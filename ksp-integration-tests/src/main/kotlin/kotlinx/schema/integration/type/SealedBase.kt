package kotlinx.schema.integration.type

import kotlinx.schema.Description
import kotlinx.schema.Schema

@Schema(withSchemaObject = true)
sealed class SealedBase(
    @Description("Base property")
    val baseProp: String,
) {
    @Schema
    data class SubclassA(
        @Description("A's property")
        val propA: Int,
    ) : SealedBase("a-fixed")

    @Schema
    data object SubclassB : SealedBase("b-fixed")
}
