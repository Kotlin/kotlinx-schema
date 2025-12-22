package kotlinx.schema.generator.reflect

import kotlinx.schema.generator.core.ir.DefaultPresence
import kotlinx.schema.generator.core.ir.Discriminator
import kotlinx.schema.generator.core.ir.ObjectNode
import kotlinx.schema.generator.core.ir.PolymorphicNode
import kotlinx.schema.generator.core.ir.PrimitiveNode
import kotlinx.schema.generator.core.ir.Property
import kotlinx.schema.generator.core.ir.SchemaIntrospector
import kotlinx.schema.generator.core.ir.SubtypeRef
import kotlinx.schema.generator.core.ir.TypeGraph
import kotlinx.schema.generator.core.ir.TypeId
import kotlinx.schema.generator.core.ir.TypeRef
import kotlin.reflect.KClass
import kotlin.reflect.KProperty

/**
 * Introspects Kotlin classes using reflection to build a [TypeGraph].
 *
 * This introspector analyzes class structures including properties, constructors,
 * and type hierarchies to generate schema IR nodes.
 *
 *  ## Example
 *  ```kotlin
 *  val typeGraph = ReflectionIntrospector.introspect(MyClass::class)
 *  ```
 *
 * ## Limitations
 * - Requires classes to have a primary constructor
 * - Type parameters are not fully supported
 */
public object ReflectionIntrospector : SchemaIntrospector<KClass<*>> {
    override fun introspect(root: KClass<*>): TypeGraph {
        val context = IntrospectionContext()
        val rootRef = context.convertToTypeRef(root)
        return TypeGraph(root = rootRef, nodes = context.discoveredNodes)
    }

    /**
     * Maintains state during class introspection including discovered nodes,
     * visited classes, and type reference cache.
     */
    @Suppress("TooManyFunctions")
    private class IntrospectionContext : BaseIntrospectionContext() {
        /**
         * Overrides base convertToTypeRef to add sealed class handling.
         */
        @Suppress("ReturnCount")
        override fun convertToTypeRef(
            klass: KClass<*>,
            nullable: Boolean,
            useSimpleName: Boolean,
        ): TypeRef {
            // Check cache first
            typeRefCache[klass]?.let { cachedRef ->
                return if (nullable && !cachedRef.nullable) {
                    cachedRef.withNullable(true)
                } else {
                    cachedRef
                }
            }

            // Try to convert to primitive type
            primitiveKindFor(klass)?.let { primitiveKind ->
                val ref = TypeRef.Inline(PrimitiveNode(primitiveKind), nullable)
                if (!nullable) typeRefCache[klass] = ref
                return ref
            }

            // Handle different type categories, including sealed classes
            return when {
                isListLike(klass) -> handleListType(klass, nullable)
                Map::class.java.isAssignableFrom(klass.java) -> handleMapType(klass, nullable)
                isEnumClass(klass) -> handleEnumType(klass, nullable)
                klass.isSealed -> handleSealedType(klass, nullable)
                else -> handleObjectType(klass, nullable, useSimpleName)
            }
        }

        private fun handleSealedType(
            klass: KClass<*>,
            nullable: Boolean,
        ): TypeRef {
            val id = createTypeId(klass)

            if (shouldProcessClass(klass, id)) {
                markAsVisiting(klass)
                val polymorphicNode = createPolymorphicNode(klass)
                discoveredNodes[id] = polymorphicNode

                // Process each sealed subclass
                klass.sealedSubclasses.forEach { subclass ->
                    convertToTypeRef(subclass, nullable = false, useSimpleName = true)
                }

                unmarkAsVisiting(klass)
            }

            val ref = TypeRef.Ref(id, nullable)
            if (!nullable) typeRefCache[klass] = ref
            return ref
        }

        private fun createPolymorphicNode(klass: KClass<*>): PolymorphicNode {
            val subtypes =
                klass.sealedSubclasses.map { subclass ->
                    SubtypeRef(TypeId(subclass.simpleName ?: "Unknown"))
                }

            // Build discriminator mapping: discriminator value -> TypeId
            val discriminatorMapping =
                klass.sealedSubclasses.associate { subclass ->
                    val simpleName = subclass.simpleName ?: "Unknown"
                    simpleName to TypeId(simpleName)
                }

            return PolymorphicNode(
                baseName = klass.simpleName ?: "UnknownSealed",
                subtypes = subtypes,
                discriminator =
                    Discriminator(
                        name = "type",
                        required = true,
                        mapping = discriminatorMapping,
                    ),
                description = extractDescription(klass.annotations),
            )
        }

        override fun createObjectNode(klass: KClass<*>): ObjectNode {
            val properties = mutableListOf<Property>()
            val requiredProperties = mutableSetOf<String>()

            // Find sealed parent classes to inherit property descriptions
            val sealedParents =
                klass.supertypes
                    .mapNotNull { it.classifier as? KClass<*> }
                    .filter { it.isSealed }

            // Build a map of parent property descriptions
            val parentPropertyDescriptions = mutableMapOf<String, String>()
            sealedParents.forEach { parent ->
                parent.members
                    .filterIsInstance<KProperty<*>>()
                    .forEach { prop ->
                        val desc = extractDescription(prop.annotations)
                        if (desc != null) {
                            parentPropertyDescriptions[prop.name] = desc
                        }
                    }
            }

            // Try to extract default values by creating an instance
            val defaultValues = DefaultValueExtractor.extractDefaultValues(klass)

            // Extract properties from primary constructor
            klass.constructors.firstOrNull()?.parameters?.forEach { param ->
                val propertyName = param.name ?: return@forEach
                val hasDefault = param.isOptional

                // Find the corresponding property to get annotations
                val property =
                    klass.members
                        .filterIsInstance<KProperty<*>>()
                        .firstOrNull { it.name == propertyName }

                val propertyType = param.type
                val typeRef = convertKTypeToTypeRef(propertyType)

                // Get description from property or inherit from parent
                val description =
                    property?.let { extractDescription(it.annotations) }
                        ?: parentPropertyDescriptions[propertyName]

                // Get the actual default value if available
                val defaultValue = if (hasDefault) defaultValues[propertyName] else null

                properties +=
                    Property(
                        name = propertyName,
                        type = typeRef,
                        description = description,
                        defaultPresence = if (hasDefault) DefaultPresence.Absent else DefaultPresence.Required,
                        defaultValue = defaultValue,
                    )

                if (!hasDefault) {
                    requiredProperties += propertyName
                }
            }

            return ObjectNode(
                name = klass.simpleName ?: "UnknownClass",
                properties = properties,
                required = requiredProperties,
                description = extractDescription(klass.annotations),
            )
        }
    }
}
