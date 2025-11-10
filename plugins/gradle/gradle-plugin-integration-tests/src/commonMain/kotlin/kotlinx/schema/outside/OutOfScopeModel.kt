package kotlinx.schema.outside

import kotlinx.schema.Description
import kotlinx.schema.Schema

/**
 * A model placed outside the configured root package to verify filtering.
 */
@Description("A model outside the root package; should NOT be processed when rootPackage is set")
@Schema
@Suppress("unused")
public data class OutOfScopeModel(
    val value: String,
)
