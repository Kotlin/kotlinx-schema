package kotlinx.schema.ksp.ir

import com.google.devtools.ksp.getDeclaredProperties
import com.google.devtools.ksp.isPublic
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSType
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
internal class KspClassIntrospector : SchemaIntrospector<KSClassDeclaration> {
    @Suppress("CyclomaticComplexMethod")
    override fun introspect(root: KSClassDeclaration): TypeGraph {
        val nodes = LinkedHashMap<TypeId, TypeNode>()
        val visiting = HashSet<KSClassDeclaration>()

        fun TypeId.ensure(node: TypeNode) {
            if (!nodes.containsKey(this)) nodes[this] = node
        }

        /**
         * Handles generic type parameters or unknown declarations by falling back to kotlin.Any.
         *
         * This handler is invoked when the type declaration is not a KSClassDeclaration or lacks
         * a qualified name (e.g., generic type parameters like `T` in `fun <T> foo(param: T)`).
         *
         * @param type The KSType to check
         * @return TypeRef.Ref to kotlin.Any if fallback is needed, null otherwise
         */
        fun handleAnyFallback(type: KSType): TypeRef? {
            val declAnyFallback = type.declaration !is KSClassDeclaration || type.declaration.qualifiedName == null
            if (!declAnyFallback) return null

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

        /**
         * Handles sealed class hierarchies by generating a PolymorphicNode.
         *
         * Creates a polymorphic schema with discriminator-based subtype resolution. Each sealed
         * subclass is recursively processed and registered in the type graph. The discriminator
         * maps simple class names to their fully qualified TypeIds.
         *
         * @param type The KSType to check
         * @param nullable Whether the type reference should be nullable
         * @param toRef Recursive mapper for processing subclass types
         * @return TypeRef.Ref to the polymorphic node if this is a sealed class, null otherwise
         */
        fun handleSealedClass(
            type: KSType,
            nullable: Boolean,
            toRef: (KSType) -> TypeRef,
        ): TypeRef? {
            val decl = type.sealedClassDeclOrNull() ?: return null
            val id = decl.typeId()

            processWithCycleDetection(decl, id, nodes, visiting) {
                // Find all sealed subclasses
                val sealedSubclasses = decl.getSealedSubclasses().toList()

                // Create SubtypeRef for each sealed subclass using their typeId()
                val subtypes = sealedSubclasses.map { SubtypeRef(it.typeId()) }

                // Build discriminator mapping: discriminator value (simple name) -> TypeId (full qualified name)
                val discriminatorMapping =
                    sealedSubclasses.associate { it.simpleName.asString() to it.typeId() }

                // Process each sealed subclass
                sealedSubclasses.forEach { toRef(it.asType(emptyList())) }

                PolymorphicNode(
                    baseName = decl.simpleName.asString(),
                    subtypes = subtypes,
                    discriminator = Discriminator(name = "type", required = true, mapping = discriminatorMapping),
                    description = extractDescription(decl) { decl.descriptionFromKdoc() },
                )
            }

            return TypeRef.Ref(id, nullable)
        }

        /**
         * Handles enum classes by generating an EnumNode.
         *
         * Extracts all enum entries and creates a schema node that constrains values to the
         * declared enum constants. Enum entries are identified by ClassKind.ENUM_ENTRY.
         *
         * @param type The KSType to check
         * @param nullable Whether the type reference should be nullable
         * @return TypeRef.Ref to the enum node if this is an enum class, null otherwise
         */
        fun handleEnum(
            type: KSType,
            nullable: Boolean,
        ): TypeRef? {
            val decl = type.enumClassDeclOrNull() ?: return null
            val id = decl.typeId()

            processWithCycleDetection(decl, id, nodes, visiting) {
                val entries =
                    decl.declarations
                        .filterIsInstance<KSClassDeclaration>()
                        .filter { it.classKind == ClassKind.ENUM_ENTRY }
                        .map { it.simpleName.asString() }
                        .toList()

                EnumNode(
                    name = decl.qualifiedName?.asString() ?: decl.simpleName.asString(),
                    entries = entries,
                    description = extractDescription(decl) { decl.descriptionFromKdoc() },
                )
            }

            return TypeRef.Ref(id, nullable)
        }

        /**
         * Handles regular objects and data classes by generating an ObjectNode.
         *
         * Prefers primary constructor parameters for data classes (extracting parameter names,
         * types, and default value presence). Falls back to public properties for objects and
         * classes without primary constructors. Properties without defaults are marked as required.
         *
         * Note: KSP does not provide access to default value expressions at compile-time
         * (https://github.com/google/ksp/issues/1868), so only the presence of defaults is tracked.
         *
         * @param type The KSType to check
         * @param nullable Whether the type reference should be nullable
         * @param toRef Recursive mapper for processing property types
         * @return TypeRef.Ref to the object node if this is a class/object, null otherwise
         */
        fun handleObjectOrClass(
            type: KSType,
            nullable: Boolean,
            toRef: (KSType) -> TypeRef,
        ): TypeRef? {
            val decl = type.declaration as? KSClassDeclaration ?: return null
            val id = decl.typeId()

            processWithCycleDetection(decl, id, nodes, visiting) {
                val props = ArrayList<Property>()
                val required = LinkedHashSet<String>()

                /**
                 * Helper to add a property and track whether it's required.
                 *
                 * Properties without default values are automatically added to the required set.
                 */
                fun addProperty(
                    name: String,
                    type: KSType,
                    description: String?,
                    hasDefaultValue: Boolean,
                ) {
                    if (!hasDefaultValue) required += name
                    props += createProperty(name, toRef(type), description, hasDefaultValue)
                }

                // Prefer primary constructor parameters for data classes; fall back to public properties
                val params = decl.primaryConstructor?.parameters.orEmpty()
                if (params.isNotEmpty()) {
                    // Note: KSP does not provide access to default value expressions at compile-time.
                    // https://github.com/google/ksp/issues/1868
                    // Only runtime reflection can extract actual default values.
                    params.forEach { p ->
                        val name = p.name?.asString() ?: return@forEach
                        addProperty(name, p.type.resolve(), extractDescription(p) { null }, p.hasDefault)
                    }
                } else {
                    decl.getDeclaredProperties().filter { it.isPublic() }.forEach { prop ->
                        addProperty(
                            prop.simpleName.asString(),
                            prop.type.resolve(),
                            extractDescription(prop) { prop.descriptionFromKdoc() },
                            false,
                        )
                    }
                }

                ObjectNode(
                    name = decl.qualifiedName?.asString() ?: decl.simpleName.asString(),
                    properties = props,
                    required = required,
                    description = extractDescription(decl) { decl.descriptionFromKdoc() },
                )
            }

            return TypeRef.Ref(id, nullable)
        }

        /**
         * Converts a KSType to a TypeRef by attempting each handler in priority order.
         *
         * Resolution strategy:
         * 1. Basic types (primitives and collections) via [resolveBasicTypeOrNull]
         * 2. Generic type parameters and unknowns -> kotlin.Any via [handleAnyFallback]
         * 3. Sealed class hierarchies -> PolymorphicNode via [handleSealedClass]
         * 4. Enum classes -> EnumNode via [handleEnum]
         * 5. Regular objects/classes -> ObjectNode via [handleObjectOrClass]
         * 6. Final fallback -> String primitive
         *
         * @param type The KSType to convert
         * @return TypeRef representing the type in the schema IR
         */
        fun toRef(type: KSType): TypeRef {
            val nullable = type.nullability == Nullability.NULLABLE

            // Try each handler in order, using elvis operator chain for single return
            return resolveBasicTypeOrNull(type, ::toRef)
                ?: handleAnyFallback(type)
                ?: handleSealedClass(type, nullable, ::toRef)
                ?: handleEnum(type, nullable)
                ?: handleObjectOrClass(type, nullable, ::toRef)
                ?: TypeRef.Inline(PrimitiveNode(PrimitiveKind.STRING), nullable)
        }

        val rootRef = TypeRef.Ref(root.typeId())
        // ensure root node is populated
        toRef(root.asType(emptyList()))
        return TypeGraph(root = rootRef, nodes = nodes)
    }
}
