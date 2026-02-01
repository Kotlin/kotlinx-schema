package kotlinx.schema.generator.json.serialization

import kotlinx.schema.generator.core.ir.SchemaIntrospector
import kotlinx.schema.generator.core.ir.TypeGraph
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json

/**
 * Introspects kotlinx.serialization descriptors into Schema IR.
 *
 * This introspector uses [SerializationIntrospectionContext] to convert
 * kotlinx.serialization [KSerializer] descriptors into the Schema IR type system.
 *
 * @property json The Json configuration used to extract discriminator settings for polymorphic types.
 *                Defaults to a Json instance with encodeDefaults = false.
 */
public class SerializationClassSchemaIntrospector(
    private val json: Json = Json {
        encodeDefaults = false
        classDiscriminator = "type"
        classDiscriminatorMode = kotlinx.serialization.json.ClassDiscriminatorMode.ALL_JSON_OBJECTS
    },
) : SchemaIntrospector<KSerializer<*>> {
    /**
     * Introspects a serializer's descriptor into a [TypeGraph].
     *
     * @param root The root serializer to introspect
     * @return A TypeGraph containing the root type reference and all discovered type nodes
     */
    public override fun introspect(root: KSerializer<*>): TypeGraph {
        val context = SerializationIntrospectionContext(json)
        val rootRef = context.toRef(root.descriptor)
        return TypeGraph(root = rootRef, nodes = context.nodes())
    }
}
