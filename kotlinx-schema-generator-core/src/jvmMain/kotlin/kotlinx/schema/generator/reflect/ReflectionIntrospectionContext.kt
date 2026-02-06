package kotlinx.schema.generator.reflect

import kotlinx.schema.generator.core.InternalSchemaGeneratorApi
import kotlinx.schema.generator.core.ir.BaseIntrospectionContext
import kotlinx.schema.generator.core.ir.ListNode
import kotlinx.schema.generator.core.ir.MapNode
import kotlinx.schema.generator.core.ir.ObjectNode
import kotlinx.schema.generator.core.ir.PrimitiveKind
import kotlinx.schema.generator.core.ir.PrimitiveNode
import kotlinx.schema.generator.core.ir.Property
import kotlinx.schema.generator.core.ir.TypeId
import kotlinx.schema.generator.core.ir.TypeRef
import kotlinx.schema.generator.core.ir.Annotation as IrAnnotation
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.KType
import kotlin.reflect.jvm.javaField

@OptIn(InternalSchemaGeneratorApi::class)
internal abstract class ReflectionIntrospectionContext : BaseIntrospectionContext<KClass<*>, KType>() {
    private companion object {
        const val ANNOTATION_MIN = "Min"
    }
    protected fun convertKTypeToTypeRef(type: KType): TypeRef {
        val classifier =
            requireNotNull(type.classifier as? KClass<*>) {
                "Unsupported classifier: ${type.classifier}. " +
                    "Only KClass classifiers are supported. Type parameters and other classifiers " +
                    "cannot be introspected using reflection."
            }
        val isNullable = type.isMarkedNullable

        return when {
            isListLike(classifier) -> {
                val elementType = type.arguments.firstOrNull()?.type
                val elementRef =
                    elementType?.let { convertKTypeToTypeRef(it) }
                        ?: TypeRef.Inline(PrimitiveNode(PrimitiveKind.STRING), false)
                TypeRef.Inline(ListNode(elementRef), isNullable)
            }

            Map::class.java.isAssignableFrom(classifier.java) -> {
                val keyType = type.arguments.getOrNull(0)?.type
                val valueType = type.arguments.getOrNull(1)?.type
                val keyRef =
                    keyType?.let { convertKTypeToTypeRef(it) }
                        ?: TypeRef.Inline(PrimitiveNode(PrimitiveKind.STRING), false)
                val valueRef =
                    valueType?.let { convertKTypeToTypeRef(it) }
                        ?: TypeRef.Inline(PrimitiveNode(PrimitiveKind.STRING), false)
                TypeRef.Inline(MapNode(keyRef, valueRef), isNullable)
            }

            else -> {
                convertToTypeRef(classifier, isNullable)
            }
        }
    }

    @Suppress("ReturnCount")
    internal open fun convertToTypeRef(
        klass: KClass<*>,
        nullable: Boolean = false,
        useSimpleName: Boolean = false,
    ): TypeRef {
        typeRefCache[klass]?.let { cachedRef ->
            return if (nullable && !cachedRef.nullable) {
                cachedRef.withNullable(true)
            } else {
                cachedRef
            }
        }

        primitiveKindFor(klass)?.let { primitiveKind ->
            val ref = TypeRef.Inline(PrimitiveNode(primitiveKind), nullable)
            if (!nullable) typeRefCache[klass] = ref
            return ref
        }

        return when {
            isListLike(klass) -> handleListType(klass, nullable)
            Map::class.java.isAssignableFrom(klass.java) -> handleMapType(klass, nullable)
            isEnumClass(klass) -> handleEnumType(klass, nullable)
            else -> handleObjectType(klass, nullable, useSimpleName)
        }
    }

    protected fun handleListType(
        klass: KClass<*>,
        nullable: Boolean,
    ): TypeRef {
        val elementRef = TypeRef.Inline(PrimitiveNode(PrimitiveKind.STRING), false)
        val ref = TypeRef.Inline(ListNode(elementRef), nullable)
        if (!nullable) typeRefCache[klass] = ref
        return ref
    }

    protected fun handleMapType(
        klass: KClass<*>,
        nullable: Boolean,
    ): TypeRef {
        val keyRef = TypeRef.Inline(PrimitiveNode(PrimitiveKind.STRING), false)
        val valueRef = TypeRef.Inline(PrimitiveNode(PrimitiveKind.STRING), false)
        val ref = TypeRef.Inline(MapNode(keyRef, valueRef), nullable)
        if (!nullable) typeRefCache[klass] = ref
        return ref
    }

    protected fun handleEnumType(
        klass: KClass<*>,
        nullable: Boolean,
    ): TypeRef {
        val id = createTypeId(klass)

        withCycleDetection(klass, id) {
            createEnumNode(klass)
        }

        val ref = TypeRef.Ref(id, nullable)
        if (!nullable) typeRefCache[klass] = ref
        return ref
    }

    protected fun handleObjectType(
        klass: KClass<*>,
        nullable: Boolean,
        useSimpleName: Boolean,
        parentPrefix: String? = null,
    ): TypeRef {
        val id =
            when {
                parentPrefix != null -> TypeId(generateQualifiedName(klass, parentPrefix))
                useSimpleName -> TypeId(klass.simpleName ?: "Unknown")
                else -> createTypeId(klass)
            }

        withCycleDetection(klass, id) {
            createObjectNode(klass, parentPrefix)
        }

        val ref = TypeRef.Ref(id, nullable)
        if (!nullable) typeRefCache[klass] = ref
        return ref
    }

    protected abstract fun createObjectNode(
        klass: KClass<*>,
        parentPrefix: String? = null,
    ): ObjectNode

    protected fun extractConstructorProperties(
        klass: KClass<*>,
        defaultValues: Map<String, Any?>,
    ): Pair<List<Property>, Set<String>> {
        val properties = mutableListOf<Property>()
        val requiredProperties = mutableSetOf<String>()

        klass.constructors.firstOrNull()?.parameters?.forEach { param ->
            val propertyName = param.name ?: return@forEach
            val hasDefault = param.isOptional

            val property = findPropertyByName(klass, propertyName)

            val propertyType = param.type
            val typeRef = convertKTypeToTypeRef(propertyType)

            val defaultValue = if (hasDefault) defaultValues[propertyName] else null

            properties +=
                Property(
                    name = propertyName,
                    type = typeRef,
                    description = property?.let { extractDescription(it.annotations) },
                    hasDefaultValue = hasDefault,
                    defaultValue = defaultValue,
                )

            if (!hasDefault) {
                requiredProperties += propertyName
            }
        }

        return properties to requiredProperties
    }

    protected fun generateQualifiedName(
        klass: KClass<*>,
        parentPrefix: String?,
    ): String {
        val simpleName = klass.simpleName ?: "Unknown"
        return if (parentPrefix != null) {
            "$parentPrefix.$simpleName"
        } else {
            simpleName
        }
    }

    protected fun findPropertyByName(
        klass: KClass<*>,
        propertyName: String,
    ): kotlin.reflect.KProperty<*>? =
        klass.members
            .filterIsInstance<kotlin.reflect.KProperty<*>>()
            .firstOrNull { it.name == propertyName }


    protected fun extractValidationAnnotationsFromProperty(property: kotlin.reflect.KProperty<*>): List<IrAnnotation> {
        val constraints = mutableListOf<IrAnnotation>()

        val processAnnotation = { ann: Annotation ->
            val qualifiedName = ann.annotationClass.qualifiedName
            if (qualifiedName != null &&
                (qualifiedName.startsWith("jakarta.validation.constraints.") ||
                    qualifiedName.startsWith("javax.validation.constraints."))) {
                val simpleName = ann.annotationClass.simpleName
                try {
                    if (simpleName == ANNOTATION_MIN) {
                        val value = ann.annotationClass.members.firstOrNull { it.name == "value" }?.call(ann)
                        value?.toString()?.toLongOrNull()?.let { constraints += IrAnnotation.Min(it) }
                    }
                } catch (_: Exception) {
                }
            }
        }

        property.annotations.forEach(processAnnotation)
        (property as? KProperty1<*, *>)?.javaField?.annotations?.forEach(processAnnotation)

        return constraints
    }
}
