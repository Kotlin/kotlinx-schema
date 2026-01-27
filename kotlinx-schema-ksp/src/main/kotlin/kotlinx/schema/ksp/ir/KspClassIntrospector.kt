package kotlinx.schema.ksp.ir

import com.google.devtools.ksp.getDeclaredProperties
import com.google.devtools.ksp.isPublic
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.Modifier
import com.google.devtools.ksp.symbol.Nullability
import kotlinx.schema.generator.core.ir.Discriminator
import kotlinx.schema.generator.core.ir.EnumNode
import kotlinx.schema.generator.core.ir.ObjectNode
import kotlinx.schema.generator.core.ir.PolymorphicNode
import kotlinx.schema.generator.core.ir.PrimitiveKind
import kotlinx.schema.generator.core.ir.PrimitiveNode
import kotlinx.schema.generator.core.ir.Property
import kotlinx.schema.generator.core.ir.SchemaIntrospector
import kotlinx.schema.generator.core.ir.SubtypeRef
import kotlinx.schema.generator.core.ir.TypeGraph
import kotlinx.schema.generator.core.ir.TypeId
import kotlinx.schema.generator.core.ir.TypeNode
import kotlinx.schema.generator.core.ir.TypeRef

/**
 * KSP-backed Schema IR introspector. Focuses on classes and enums; generics use star-projection.
 */
@Suppress("ReturnCount", "MaxLineLength", "NestedBlockDepth", "LongMethod", "MaxLineLength")
internal class KspClassIntrospector : SchemaIntrospector<KSClassDeclaration> {
    @Suppress("CyclomaticComplexMethod")
    override fun introspect(root: KSClassDeclaration): TypeGraph {
        val nodes = LinkedHashMap<TypeId, TypeNode>()
        val visiting = HashSet<KSClassDeclaration>()

        fun TypeId.ensure(node: TypeNode) {
            if (!nodes.containsKey(this)) nodes[this] = node
        }

        fun toRef(type: KSType): TypeRef {
            val nullable = type.nullability == Nullability.NULLABLE

            // Try primitive types first
            KspTypeMappers.primitiveFor(type)?.let { prim ->
                return TypeRef.Inline(prim, nullable)
            }

            // Try collection types (List, Set, Map, Array)
            KspTypeMappers.collectionTypeRefOrNull(type, ::toRef)?.let { collectionRef ->
                return collectionRef
            }

            // Generic type parameters or unknown declarations -> star-projection to kotlin.Any
            val declAnyFallback = type.declaration !is KSClassDeclaration || type.declaration.qualifiedName == null
            if (declAnyFallback) {
                val anyId = TypeId("kotlin.Any")
                anyId.ensure(
                    ObjectNode(
                        name = "kotlin.Any",
                        properties = emptyList(),
                        required = emptySet(),
                        description = null,
                    ),
                )
                return TypeRef.Ref(anyId, false)
            }

            // Sealed classes (polymorphic hierarchies)
            if (type.declaration is KSClassDeclaration &&
                (type.declaration as KSClassDeclaration).modifiers.contains(Modifier.SEALED)
            ) {
                val decl = type.declaration as KSClassDeclaration

                val id = decl.typeId()
                if (!nodes.containsKey(id) && decl !in visiting) {
                    visiting += decl

                    // Find all sealed subclasses
                    val sealedSubclasses = decl.getSealedSubclasses().toList()

                    // Create SubtypeRef for each sealed subclass using their typeId()
                    val subtypes =
                        sealedSubclasses.map { subclass ->
                            SubtypeRef(subclass.typeId())
                        }

                    // Build discriminator mapping: discriminator value (simple name) -> TypeId (full qualified name)
                    val discriminatorMapping =
                        sealedSubclasses.associate { subclass ->
                            val simpleName = subclass.simpleName.asString()
                            simpleName to subclass.typeId()
                        }

                    val node =
                        PolymorphicNode(
                            baseName = decl.simpleName.asString(),
                            subtypes = subtypes,
                            discriminator =
                                Discriminator(
                                    name = "type",
                                    required = true,
                                    mapping = discriminatorMapping,
                                ),
                            description = decl.descriptionOrDefault(decl.descriptionFromKdoc()),
                        )
                    nodes[id] = node

                    // Process each sealed subclass
                    sealedSubclasses.forEach { subclass ->
                        toRef(subclass.asType(emptyList()))
                    }

                    visiting -= decl
                }
                return TypeRef.Ref(id, nullable)
            }

            // Enums
            if (type.declaration is KSClassDeclaration &&
                (type.declaration as KSClassDeclaration).classKind == ClassKind.ENUM_CLASS
            ) {
                val decl = type.declaration as KSClassDeclaration

                val id = decl.typeId()
                if (!nodes.containsKey(id) && decl !in visiting) {
                    visiting += decl
                    val entries =
                        decl.declarations
                            .filterIsInstance<KSClassDeclaration>()
                            .filter { it.classKind == ClassKind.ENUM_ENTRY }
                            .map { it.simpleName.asString() }
                            .toList()
                    val node =
                        EnumNode(
                            name = decl.qualifiedName?.asString() ?: decl.simpleName.asString(),
                            entries = entries,
                            description = decl.descriptionOrDefault(decl.descriptionFromKdoc()),
                        )
                    nodes[id] = node
                    visiting -= decl
                }
                return TypeRef.Ref(id, nullable)
            }

            // Objects/classes
            val decl = type.declaration as? KSClassDeclaration
            if (decl != null) {
                val id = decl.typeId()
                if (!nodes.containsKey(id) && decl !in visiting) {
                    visiting += decl
                    val props = ArrayList<Property>()
                    val required = LinkedHashSet<String>()

                    // Prefer primary constructor parameters for data classes; fall back to public properties
                    val params = decl.primaryConstructor?.parameters.orEmpty()
                    if (params.isNotEmpty()) {
                        params.forEach { p ->
                            val name = p.name?.asString() ?: return@forEach
                            val pType = p.type.resolve()
                            val desc =
                                p.annotations.firstNotNullOfOrNull { it.descriptionOrNull() } // todo: gen kdoc
                            val tref = toRef(pType)
                            val hasDefaultValue = p.hasDefault
                            if (!p.hasDefault) required += name
                            // Note: KSP does not provide access to default value expressions at compile-time.
                            // https://github.com/google/ksp/issues/1868
                            // Only runtime reflection can extract actual default values.
                            props +=
                                Property(
                                    name = name,
                                    type = tref,
                                    description = desc,
                                    hasDefaultValue = hasDefaultValue,
                                    defaultValue = null,
                                )
                        }
                    } else {
                        decl.getDeclaredProperties().filter { it.isPublic() }.forEach { prop ->
                            val name = prop.simpleName.asString()
                            val pType = prop.type.resolve()
                            val desc =
                                prop.annotations.firstNotNullOfOrNull { it.descriptionOrNull() }
                                    ?: prop.descriptionFromKdoc()
                            val tref = toRef(pType)
                            // KSP doesn't easily provide default presence here; treat as required conservatively
                            val hasDefaultValue = false
                            required += name
                            props +=
                                Property(
                                    name = name,
                                    type = tref,
                                    description = desc,
                                    hasDefaultValue = hasDefaultValue,
                                    defaultValue = null,
                                )
                        }
                    }

                    val node =
                        ObjectNode(
                            name = decl.qualifiedName?.asString() ?: decl.simpleName.asString(),
                            properties = props,
                            required = required,
                            description = decl.descriptionOrDefault(decl.descriptionFromKdoc()),
                        )
                    nodes[id] = node
                    visiting -= decl
                }
                return TypeRef.Ref(id, nullable)
            }

            // Fallback to string
            return TypeRef.Inline(PrimitiveNode(PrimitiveKind.STRING), nullable)
        }

        val rootRef = TypeRef.Ref(root.typeId())
        // ensure root node is populated
        toRef(root.asType(emptyList()))
        return TypeGraph(root = rootRef, nodes = nodes)
    }
}
