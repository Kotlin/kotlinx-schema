package kotlinx.schema.generator.json.serialization

import kotlinx.schema.generator.core.ir.DescriptionExtractor
import kotlinx.schema.generator.core.ir.SchemaIntrospector
import kotlinx.schema.generator.core.ir.TypeGraph
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.json.Json

/**
 * Introspects kotlinx.serialization descriptors into Schema IR.
 *
 * This introspector uses [SerializationIntrospectionContext] to convert
 * kotlinx.serialization [SerialDescriptor] instances into the Schema IR type system.
 *
 * @property json The Json configuration used to extract discriminator settings for polymorphic types.
 *                Defaults to a Json instance with encodeDefaults = false.
 */
public class SerializationClassSchemaIntrospector(
    override val config: Config = Config(),
    private val json: Json =
        Json {
            encodeDefaults = false
            classDiscriminator = "type"
            classDiscriminatorMode = kotlinx.serialization.json.ClassDiscriminatorMode.ALL_JSON_OBJECTS
        },
) : SchemaIntrospector<SerialDescriptor, SerializationClassSchemaIntrospector.Config> {
    public data class Config(
        val descriptionExtractor: DescriptionExtractor = DescriptionExtractor { null },
    )

    /**
     * Introspects a serial descriptor into a [TypeGraph].
     *
     * @param root The root serial descriptor to introspect
     * @return A TypeGraph containing the root type reference and all discovered type nodes
     */
    public override fun introspect(root: SerialDescriptor): TypeGraph {
        val context = SerializationIntrospectionContext(json, config)
        val rootRef = context.toRef(root)
        return TypeGraph(root = rootRef, nodes = context.nodes())
    }
}
