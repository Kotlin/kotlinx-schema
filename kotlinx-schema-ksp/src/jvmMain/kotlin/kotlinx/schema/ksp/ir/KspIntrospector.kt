package kotlinx.schema.ksp.ir

import com.google.devtools.ksp.getDeclaredProperties
import com.google.devtools.ksp.isPublic
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.Nullability
import kotlinx.schema.Description
import kotlinx.schema.generator.core.ir.DefaultPresence
import kotlinx.schema.generator.core.ir.EnumNode
import kotlinx.schema.generator.core.ir.ListNode
import kotlinx.schema.generator.core.ir.MapNode
import kotlinx.schema.generator.core.ir.ObjectNode
import kotlinx.schema.generator.core.ir.PrimitiveKind
import kotlinx.schema.generator.core.ir.PrimitiveNode
import kotlinx.schema.generator.core.ir.Property
import kotlinx.schema.generator.core.ir.SchemaIntrospector
import kotlinx.schema.generator.core.ir.TypeGraph
import kotlinx.schema.generator.core.ir.TypeId
import kotlinx.schema.generator.core.ir.TypeNode
import kotlinx.schema.generator.core.ir.TypeRef

/**
 * KSP-backed Schema IR introspector. Focuses on classes and enums; generics use star-projection.
 */
public class KspIntrospector : SchemaIntrospector<KSClassDeclaration> {
    fun AAA() {
    }

    override fun introspect(root: KSClassDeclaration): TypeGraph {
        val nodes = LinkedHashMap<TypeId, TypeNode>()
        val visiting = HashSet<KSClassDeclaration>()

        fun TypeId.ensure(node: TypeNode) {
            if (!nodes.containsKey(this)) nodes[this] = node
        }

        fun KSAnnotation.tryDescription(): String? =
            if ((
                    annotationType
                        .resolve()
                        .declaration.qualifiedName
                        ?.asString() == Description::class.qualifiedName
                )
            ) {
                (arguments.firstOrNull { it.name?.asString() == "value" }?.value as? String)
            } else {
                null
            }

        fun KSAnnotated.descriptionOrNull(): String? = annotations.mapNotNull { it.tryDescription() }.firstOrNull()

        fun typeIdFor(decl: KSClassDeclaration): TypeId =
            TypeId(decl.qualifiedName?.asString() ?: decl.simpleName.asString())

        fun primitiveFor(type: KSType): PrimitiveNode? {
            val qn = type.declaration.qualifiedName?.asString()
            return when (qn) {
                "kotlin.String" -> PrimitiveNode(PrimitiveKind.STRING)
                "kotlin.Boolean" -> PrimitiveNode(PrimitiveKind.BOOLEAN)
                "kotlin.Int", "kotlin.Byte", "kotlin.Short" -> PrimitiveNode(PrimitiveKind.INT)
                "kotlin.Long" -> PrimitiveNode(PrimitiveKind.LONG)
                "kotlin.Float" -> PrimitiveNode(PrimitiveKind.FLOAT)
                "kotlin.Double" -> PrimitiveNode(PrimitiveKind.DOUBLE)
                else -> null
            }
        }

        fun toRef(type: KSType): TypeRef {
            val nullable = type.nullability == Nullability.NULLABLE

            primitiveFor(type)?.let { prim ->
                return TypeRef.Inline(prim, nullable)
            }

            val qn = type.declaration.qualifiedName?.asString()

            // Collections
            if (qn == "kotlin.collections.List" || qn == "kotlin.collections.Set") {
                val elem =
                    type.arguments
                        .firstOrNull()
                        ?.type
                        ?.resolve() ?: return TypeRef.Inline(
                        ListNode(
                            TypeRef.Inline(PrimitiveNode(PrimitiveKind.STRING)),
                        ),
                        nullable,
                    )
                return TypeRef.Inline(ListNode(element = toRef(elem)), nullable)
            }
            if (qn == "kotlin.collections.Map") {
                val key =
                    type.arguments
                        .getOrNull(0)
                        ?.type
                        ?.resolve() ?: return TypeRef.Inline(
                        MapNode(
                            TypeRef.Inline(PrimitiveNode(PrimitiveKind.STRING)),
                            TypeRef.Inline(PrimitiveNode(PrimitiveKind.STRING)),
                        ),
                        nullable,
                    )
                val value =
                    type.arguments
                        .getOrNull(1)
                        ?.type
                        ?.resolve() ?: return TypeRef.Inline(
                        MapNode(
                            TypeRef.Inline(PrimitiveNode(PrimitiveKind.STRING)),
                            TypeRef.Inline(PrimitiveNode(PrimitiveKind.STRING)),
                        ),
                        nullable,
                    )
                return TypeRef.Inline(MapNode(key = toRef(key), value = toRef(value)), nullable)
            }
            if (qn == "kotlin.Array") {
                val elem =
                    type.arguments
                        .firstOrNull()
                        ?.type
                        ?.resolve() ?: return TypeRef.Inline(
                        ListNode(
                            TypeRef.Inline(PrimitiveNode(PrimitiveKind.STRING)),
                        ),
                        nullable,
                    )
                return TypeRef.Inline(ListNode(element = toRef(elem)), nullable)
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

            // Enums
            if (type.declaration is KSClassDeclaration &&
                (type.declaration as KSClassDeclaration).classKind == ClassKind.ENUM_CLASS
            ) {
                val decl = type.declaration as KSClassDeclaration
                val id = typeIdFor(decl)
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
                            description = decl.descriptionOrNull(),
                        )
                    nodes[id] = node
                    visiting -= decl
                }
                return TypeRef.Ref(id, nullable)
            }

            // Objects/classes
            val decl = type.declaration as? KSClassDeclaration
            if (decl != null) {
                val id = typeIdFor(decl)
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
                            val desc = p.annotations.mapNotNull { it.tryDescription() }.firstOrNull()
                            val tref = toRef(pType)
                            val presence = if (p.hasDefault) DefaultPresence.Absent else DefaultPresence.Required
                            if (!p.hasDefault) required += name
                            props += Property(name = name, type = tref, description = desc, defaultPresence = presence)
                        }
                    } else {
                        decl.getDeclaredProperties().filter { it.isPublic() }.forEach { prop ->
                            val name = prop.simpleName.asString()
                            val pType = prop.type.resolve()
                            val desc = prop.annotations.mapNotNull { it.tryDescription() }.firstOrNull()
                            val tref = toRef(pType)
                            // KSP doesn't easily provide default presence here; treat as required conservatively
                            val presence = DefaultPresence.Required
                            required += name
                            props += Property(name = name, type = tref, description = desc, defaultPresence = presence)
                        }
                    }

                    val node =
                        ObjectNode(
                            name = decl.qualifiedName?.asString() ?: decl.simpleName.asString(),
                            properties = props,
                            required = required,
                            description = decl.descriptionOrNull(),
                        )
                    nodes[id] = node
                    visiting -= decl
                }
                return TypeRef.Ref(id, nullable)
            }

            // Fallback to string
            return TypeRef.Inline(PrimitiveNode(PrimitiveKind.STRING), nullable)
        }

        val rootRef = TypeRef.Ref(typeIdFor(root))
        // ensure root node is populated
        toRef(root.asType(emptyList()))
        return TypeGraph(root = rootRef, nodes = nodes)
    }
}
