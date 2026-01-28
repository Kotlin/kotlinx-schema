package kotlinx.schema.ksp.ir

import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.Nullability
import kotlinx.schema.generator.core.ir.Property
import kotlinx.schema.generator.core.ir.TypeId
import kotlinx.schema.generator.core.ir.TypeNode
import kotlinx.schema.generator.core.ir.TypeRef

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
 * Processes a class declaration with cycle detection, ensuring each type is visited only once.
 *
 * This helper manages the visiting set lifecycle and node registration for KspClassIntrospector.
 * It prevents infinite recursion when processing recursive or mutually recursive type structures.
 *
 * Usage pattern:
 * ```kotlin
 * processWithCycleDetection(decl, id, nodes, visiting) {
 *     // Build node here
 *     ObjectNode(...)
 * }
 * ```
 *
 * @param decl The class declaration to process
 * @param id The TypeId for this class
 * @param nodes The current node map
 * @param visiting The set tracking currently visiting declarations
 * @param nodeBuilder Lambda that constructs the TypeNode for this declaration
 * @return true if the node was created, false if it was already visited
 */
internal inline fun processWithCycleDetection(
    decl: KSClassDeclaration,
    id: TypeId,
    nodes: MutableMap<TypeId, TypeNode>,
    visiting: MutableSet<KSClassDeclaration>,
    nodeBuilder: () -> TypeNode,
): Boolean {
    if (nodes.containsKey(id) || decl in visiting) {
        return false
    }

    visiting += decl
    try {
        val node = nodeBuilder()
        nodes[id] = node
        return true
    } finally {
        visiting -= decl
    }
}

/**
 * Attempts to resolve basic types (primitives and collections) to TypeRef.
 *
 * This is the shared prefix logic used by both KspClassIntrospector and KspFunctionIntrospector
 * for handling primitive types and collections before diverging to handle complex types.
 *
 * Returns null if the type requires complex handling (classes, enums, sealed, etc.).
 *
 * @param type The KSType to resolve
 * @param recursiveMapper Function to recursively resolve nested types (for collection elements)
 * @return TypeRef if this is a primitive or collection type, null otherwise
 */
internal fun resolveBasicTypeOrNull(
    type: KSType,
    recursiveMapper: (KSType) -> TypeRef,
): TypeRef? {
    val nullable = type.nullability == Nullability.NULLABLE

    // Try primitive types first, then collections, using elvis operator chain
    return KspTypeMappers.primitiveFor(type)?.let { TypeRef.Inline(it, nullable) }
        ?: KspTypeMappers.collectionTypeRefOrNull(type, recursiveMapper)
}
