package kotlinx.schema.ksp.ir

import com.google.devtools.ksp.getDeclaredProperties
import com.google.devtools.ksp.isPublic
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.KSValueParameter
import com.google.devtools.ksp.symbol.Nullability
import kotlinx.schema.generator.core.InternalSchemaGeneratorApi
import kotlinx.schema.generator.core.ir.BaseIntrospectionContext
import kotlinx.schema.generator.core.ir.ObjectNode
import kotlinx.schema.generator.core.ir.Property
import kotlinx.schema.generator.core.ir.TypeId
import kotlinx.schema.generator.core.ir.TypeRef

/**
 * Shared introspection context for KSP-based introspectors.
 *
 * Eliminates toRef() duplication between KspClassIntrospector and KspFunctionIntrospector
 * by providing a single, well-tested implementation of the type resolution strategy.
 *
 * Extends [BaseIntrospectionContext] to inherit state management and cycle detection,
 * while implementing KSP-specific type resolution logic.
 *
 * Resolution strategy (applied in order):
 * 1. Basic types (primitives and collections) via [resolveBasicTypeOrNull]
 * 2. Generic type parameters and unknowns -> kotlin.Any via [handleAnyFallback]
 * 3. Sealed class hierarchies -> PolymorphicNode via [handleSealedClass]
 * 4. Enum classes -> EnumNode via [handleEnum]
 * 5. Regular objects/classes -> ObjectNode via [handleObjectOrClass]
 */
@OptIn(InternalSchemaGeneratorApi::class)
internal class KspIntrospectionContext : BaseIntrospectionContext<KSType>() {
    /**
     * Converts a KSType to a TypeRef using the standard resolution strategy.
     *
     * This method implements the common type resolution pattern used across all KSP
     * introspectors. It tries each handler in priority order, using elvis operator
     * chain to return the first successful match.
     *
     * All types should be handled by one of the resolution steps. If not, an exception
     * is thrown to fail fast and help identify missing handler cases during development.
     *
     * @param type The KSType to convert
     * @return TypeRef representing the type in the schema IR
     * @throws IllegalArgumentException if the type cannot be handled by any handler
     */
    override fun toRef(type: KSType): TypeRef {
        val nullable = type.nullability == Nullability.NULLABLE

        // Try each handler in order, using elvis operator chain for single return
        return requireNotNull(
            resolveBasicTypeOrNull(type)
                ?: handleAnyFallback(type)
                ?: handleSealedClass(type, nullable)
                ?: handleEnum(type, nullable)
                ?: handleObjectOrClass(type, nullable),
        ) {
            "Unexpected type that couldn't be handled: ${type.declaration.qualifiedName}"
        }
    }

    /**
     * Creates a Property instance with consistent defaults.
     *
     * Shared by KspFunctionIntrospector and KspClassIntrospector to ensure
     * uniform property construction across introspectors.
     *
     * @param name Property name
     * @param type Property type reference
     * @param description Property description (from annotations or KDoc)
     * @param hasDefaultValue Whether the property has a default value
     * @return Property instance with defaultValue set to null (KSP limitation)
     */
    internal fun createProperty(
        name: String,
        type: TypeRef,
        description: String?,
        hasDefaultValue: Boolean,
    ): Property =
        Property(
            name = name,
            type = type,
            description = description,
            hasDefaultValue = hasDefaultValue,
            defaultValue = null, // KSP cannot extract default values at compile-time
        )

    /**
     * Extracts description from annotations with KDoc fallback.
     *
     * Unifies the description extraction pattern used across introspectors:
     * 1. Try annotations first (via descriptionOrNull())
     * 2. Fall back to KDoc if available
     *
     * @param annotated The annotated element to extract description from
     * @param kdocFallback Lazy KDoc description provider (only evaluated if annotations don't provide description)
     * @return Description string or null if not found
     */
    internal fun extractDescription(
        annotated: KSAnnotated,
        kdocFallback: () -> String?,
    ): String? =
        annotated.annotations.firstNotNullOfOrNull { it.descriptionOrNull() }
            ?: kdocFallback()

    /**
     * Extracts description for a property or parameter with multiple fallback sources.
     *
     * This function supports the following description resolution chain:
     * 1. Annotations on the property/parameter (e.g., @Description)
     * 2. Property's own KDoc (via elementKdocFallback)
     * 3. Parent's KDoc tag (@param or @property) for the property/parameter name
     *
     * @param annotated The annotated property/parameter element
     * @param propertyName The name of the property/parameter (used for parent KDoc lookup)
     * @param parentKdoc The parent's KDoc string (class KDoc for @property or function KDoc for @param)
     * @param kdocTagName The tag name to search in parent KDoc ("param" or "property")
     * @param elementKdocFallback Lazy KDoc description provider for the element itself
     * @return Description string or null if not found in any source
     */
    internal fun extractPropertyDescription(
        annotated: KSAnnotated,
        propertyName: String,
        parentKdoc: String?,
        kdocTagName: String,
        elementKdocFallback: () -> String?,
    ): String? =
        // 1. Try annotations first
        annotated.annotations.firstNotNullOfOrNull { it.descriptionOrNull() }
            // 2. Fall back to property's own KDoc
            ?: elementKdocFallback()
            // 3. Finally, try parent KDoc tag
            ?: extractTagDescriptionFromKdoc(parentKdoc, kdocTagName, propertyName)

    /**
     * Extracts description for a constructor parameter with class KDoc fallback.
     *
     * Constructor parameters can be documented using either @param or @property tags
     * in the class KDoc. This function tries both with @param taking precedence.
     *
     * Resolution chain:
     * 1. Annotations on the parameter (e.g., @Description)
     * 2. Class KDoc @param tag
     * 3. Class KDoc @property tag
     *
     * @param param The constructor parameter
     * @param paramName The parameter name
     * @param classKdoc The class KDoc string
     * @return Description string or null if not found in any source
     */
    private fun extractConstructorParamDescription(
        param: KSValueParameter,
        paramName: String,
        classKdoc: String?,
    ): String? =
        param.annotations.firstNotNullOfOrNull { it.descriptionOrNull() }
            ?: extractTagDescriptionFromKdoc(classKdoc, "param", paramName)
            ?: extractTagDescriptionFromKdoc(classKdoc, "property", paramName)

    /**
     * Attempts to resolve basic types (primitives and collections) to TypeRef.
     *
     * This is the shared prefix logic used by both KspClassIntrospector and KspFunctionIntrospector
     * for handling primitive types and collections before diverging to handle complex types.
     *
     * Returns null if the type requires complex handling (classes, enums, sealed, etc.).
     *
     * @param type The KSType to resolve
     * @return TypeRef if this is a primitive or collection type, null otherwise
     */
    internal fun resolveBasicTypeOrNull(type: KSType): TypeRef? {
        val nullable = type.nullability == Nullability.NULLABLE

        // Try primitive types first, then collections, using elvis operator chain
        return KspTypeMappers.primitiveFor(type)?.let { TypeRef.Inline(it, nullable) }
            ?: KspTypeMappers.collectionTypeRefOrNull(type, ::toRef)
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
    internal fun handleAnyFallback(type: KSType): TypeRef? {
        val declAnyFallback = type.declaration !is KSClassDeclaration || type.declaration.qualifiedName == null
        if (!declAnyFallback) return null

        val anyId = TypeId("kotlin.Any")
        withCycleDetection(type, anyId) {
            ObjectNode(
                name = "kotlin.Any",
                properties = emptyList(),
                required = emptySet(),
                description = null,
            )
        }

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
     * @return TypeRef.Ref to the polymorphic node if this is a sealed class, null otherwise
     */
    internal fun handleSealedClass(
        type: KSType,
        nullable: Boolean,
    ): TypeRef? {
        val decl = type.sealedClassDeclOrNull() ?: return null
        val id = decl.typeId()

        withCycleDetection(type, id) {
            // Find all sealed subclasses
            val sealedSubclasses = decl.getSealedSubclasses().toList()

            // Create SubtypeRef for each sealed subclass using their typeId()
            val subtypes =
                sealedSubclasses.map {
                    kotlinx.schema.generator.core.ir
                        .SubtypeRef(it.typeId())
                }

            // Build discriminator mapping: discriminator value (simple name) -> TypeId (full qualified name)
            val discriminatorMapping =
                sealedSubclasses.associate { it.simpleName.asString() to it.typeId() }

            // Process each sealed subclass
            sealedSubclasses.forEach { toRef(it.asType(emptyList())) }

            kotlinx.schema.generator.core.ir.PolymorphicNode(
                baseName = decl.simpleName.asString(),
                subtypes = subtypes,
                discriminator =
                    kotlinx.schema.generator.core.ir.Discriminator(
                        // TODO allow to configure discriminator property name
                        name = "type",
                        required = true,
                        mapping = discriminatorMapping,
                    ),
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
    internal fun handleEnum(
        type: KSType,
        nullable: Boolean,
    ): TypeRef? {
        val decl = type.enumClassDeclOrNull() ?: return null
        val id = decl.typeId()

        withCycleDetection(type, id) {
            val entries =
                decl.declarations
                    .filterIsInstance<KSClassDeclaration>()
                    .filter { it.classKind == com.google.devtools.ksp.symbol.ClassKind.ENUM_ENTRY }
                    .map { it.simpleName.asString() }
                    .toList()

            kotlinx.schema.generator.core.ir.EnumNode(
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
     * @return TypeRef.Ref to the object node if this is a class/object, null otherwise
     */
    internal fun handleObjectOrClass(
        type: KSType,
        nullable: Boolean,
    ): TypeRef? {
        val decl = type.declaration as? KSClassDeclaration ?: return null
        val id = decl.typeId()

        withCycleDetection(type, id) {
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
                    val description = extractConstructorParamDescription(p, name, decl.docString)
                    addProperty(name, p.type.resolve(), description, p.hasDefault)
                }
            } else {
                decl.getDeclaredProperties().filter { it.isPublic() }.forEach { prop ->
                    val name = prop.simpleName.asString()
                    // Extract description from: annotations -> property KDoc -> class KDoc @property tag
                    val description =
                        extractPropertyDescription(
                            annotated = prop,
                            propertyName = name,
                            parentKdoc = decl.docString,
                            kdocTagName = "property",
                            elementKdocFallback = { prop.descriptionFromKdoc() },
                        )
                    addProperty(
                        name,
                        prop.type.resolve(),
                        description,
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
}
