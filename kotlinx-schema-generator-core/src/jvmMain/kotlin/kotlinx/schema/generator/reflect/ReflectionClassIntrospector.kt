package kotlinx.schema.generator.reflect

import kotlinx.schema.generator.core.ir.Discriminator
import kotlinx.schema.generator.core.ir.ObjectNode
import kotlinx.schema.generator.core.ir.PolymorphicNode
import kotlinx.schema.generator.core.ir.PrimitiveKind
import kotlinx.schema.generator.core.ir.PrimitiveNode
import kotlinx.schema.generator.core.ir.Property
import kotlinx.schema.generator.core.ir.SchemaIntrospector
import kotlinx.schema.generator.core.ir.SubtypeRef
import kotlinx.schema.generator.core.ir.TypeGraph
import kotlinx.schema.generator.core.ir.TypeId
import kotlinx.schema.generator.core.ir.TypeRef
import kotlinx.schema.generator.core.ir.Annotation as IrAnnotation
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
 *  val typeGraph = ReflectionClassIntrospector.introspect(MyClass::class)
 *  ```
 *
 * ## Limitations
 * - Requires classes to have a primary constructor
 * - Type parameters are not fully supported
 */
public object ReflectionClassIntrospector : SchemaIntrospector<KClass<*>> {
    override fun introspect(root: KClass<*>): TypeGraph {
        val context = IntrospectionContext()
        val rootRef = context.convertToTypeRef(root)
        return TypeGraph(root = rootRef, nodes = context.nodes)
    }

    /**
     * Maintains state during class introspection including discovered nodes,
     * visited classes, and type reference cache.
     */
    private class IntrospectionContext : ReflectionIntrospectionContext() {
        /**
         * Overrides base convertToTypeRef to add sealed class handling before object handling.
         */
        override fun convertToTypeRef(
            klass: KClass<*>,
            nullable: Boolean,
            useSimpleName: Boolean,
        ): TypeRef {
            // Handle sealed classes specially (before calling super)
            if (klass.isSealed) {
                return handleSealedType(klass, nullable)
            }
            // Delegate to base implementation for all other cases
            return super.convertToTypeRef(klass, nullable, useSimpleName)
        }

        private fun handleSealedType(
            klass: KClass<*>,
            nullable: Boolean,
        ): TypeRef {
            val id = createTypeId(klass)

            withCycleDetection(klass, id) {
                val polymorphicNode = createPolymorphicNode(klass)

                // Process each sealed subclass with parent-qualified names
                val parentName = klass.simpleName ?: "UnknownSealed"
                klass.sealedSubclasses.forEach { subclass ->
                    handleObjectType(subclass, nullable = false, useSimpleName = false, parentPrefix = parentName)
                }

                polymorphicNode
            }

            val ref = TypeRef.Ref(id, nullable)
            if (!nullable) typeRefCache[klass] = ref
            return ref
        }

        private fun createPolymorphicNode(klass: KClass<*>): PolymorphicNode {
            val parentName = klass.simpleName ?: "UnknownSealed"

            val subtypes =
                klass.sealedSubclasses.map { subclass ->
                    SubtypeRef(TypeId(generateQualifiedName(subclass, parentName)))
                }

            // Build discriminator mapping: discriminator value -> TypeId
            val discriminatorMapping =
                klass.sealedSubclasses.associate { subclass ->
                    val simpleName = subclass.simpleName ?: "Unknown"
                    simpleName to TypeId(generateQualifiedName(subclass, parentName))
                }

            return PolymorphicNode(
                baseName = parentName,
                subtypes = subtypes,
                discriminator =
                    Discriminator(
                        required = true,
                        mapping = discriminatorMapping,
                    ),
                description = extractDescription(klass.annotations),
            )
        }

        @Suppress("LongMethod", "CyclomaticComplexMethod")
        override fun createObjectNode(
            klass: KClass<*>,
            parentPrefix: String?,
        ): ObjectNode {
            val properties = mutableListOf<Property>()
            val requiredProperties = mutableSetOf<String>()

            val sealedParents =
                klass.supertypes
                    .mapNotNull { it.classifier as? KClass<*> }
                    .filter { it.isSealed }

            val parentPropertyDescriptions = mutableMapOf<String, String>()
            val parentProperties = mutableSetOf<String>()
            sealedParents.forEach { parent ->
                parent.members
                    .filterIsInstance<KProperty<*>>()
                    .forEach { prop ->
                        parentProperties.add(prop.name)
                        val desc = extractDescription(prop.annotations)
                        if (desc != null) {
                            parentPropertyDescriptions[prop.name] = desc
                        }
                    }
            }

            if (sealedParents.isNotEmpty()) {
                val typeName = generateQualifiedName(klass, parentPrefix)
                properties +=
                    Property(
                        name = "type",
                        type = TypeRef.Inline(PrimitiveNode(PrimitiveKind.STRING), false),
                        description = null,
                        hasDefaultValue = false,
                        defaultValue = typeName,
                    )
                requiredProperties += "type"
            }

            val defaultValues = DefaultValueExtractor.extractDefaultValues(klass)

            val (constructorProperties, constructorRequired) = extractConstructorProperties(klass, defaultValues)

            val processedProperties = constructorProperties.map { it.name }.toSet()

            constructorProperties.forEach { prop ->
                val property = findPropertyByName(klass, prop.name)
                val validationConstraints = property?.let { extractValidationAnnotationsFromProperty(it) } ?: emptyList()
                val updatedDescription =
                    if (sealedParents.isNotEmpty() && prop.description == null && parentPropertyDescriptions.containsKey(prop.name)) {
                        parentPropertyDescriptions[prop.name]
                    } else {
                        prop.description
                    }
                properties += prop.copy(description = updatedDescription, constraints = validationConstraints)
            }

            requiredProperties += constructorRequired

            val inheritedPropertyNames = parentProperties - processedProperties
            inheritedPropertyNames.forEach { propertyName ->
                val property = findPropertyByName(klass, propertyName)

                if (property != null) {
                    val typeRef = convertKTypeToTypeRef(property.returnType)
                    val description = parentPropertyDescriptions[propertyName]

                    val fixedValue = defaultValues[propertyName]

                    properties +=
                        Property(
                            name = propertyName,
                            type = typeRef,
                            description = description,
                            hasDefaultValue = fixedValue != null,
                            defaultValue = fixedValue,
                            constraints = extractValidationAnnotationsFromProperty(property),
                        )

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
