package kotlinx.schema.generator.core.ir

import kotlinx.schema.generator.core.InternalSchemaGeneratorApi

/**
 * Base context for introspection that maintains state and provides common utilities.
 *
 * This abstraction extracts common patterns from Reflection, KSP, and potential
 * Serialization introspectors, including:
 * - State management (discovered nodes, visiting set, type cache)
 * - Cycle detection lifecycle
 * - Type resolution patterns
 *
 * Type parameters:
 * - TDecl: Declaration type (KClass, KSClassDeclaration, SerialDescriptor)
 * - TType: Type usage type (KType, KSType, SerialDescriptor)
 *
 * Note: For kotlinx.serialization, SerialDescriptor serves as both declaration and type,
 * so both type parameters would be SerialDescriptor.
 *
 * @suppress Not part of public API - used internally by introspector implementations.
 */
@InternalSchemaGeneratorApi
@Suppress("AbstractClassCanBeConcreteClass")
public abstract class BaseIntrospectionContext<TDecl : Any, TType : Any> {
    /**
     * Map of discovered type nodes indexed by their type ID.
     * LinkedHashMap preserves discovery order for deterministic output.
     */
    @Suppress("VariableNaming", "ktlint:standard:backing-property-naming", "PropertyName")
    protected val _nodes: MutableMap<TypeId, TypeNode> = linkedMapOf()

    /**
     * Exposes discovered nodes as read-only copy for building TypeGraph.
     * Provides a consistent API across all introspector implementations (Reflection, KSP, Serialization).
     */
    public val nodes: Map<TypeId, TypeNode>
        get() = _nodes.toMap()

    /**
     * Set of declarations currently being visited (for cycle detection).
     * When a type references itself (directly or indirectly), we detect the cycle
     * and avoid infinite recursion by checking this set.
     *
     * Protected to allow subclasses (like KSP) to expose it to handler functions.
     */
    protected val visitingDeclarations: MutableSet<TDecl> = mutableSetOf()

    /**
     * Cache of type references to avoid redundant processing.
     * Stores non-nullable refs to declarations for reuse.
     */
    protected val typeRefCache: MutableMap<TDecl, TypeRef> = mutableMapOf()

    /**
     * Cycle detection helper that manages visiting set lifecycle.
     *
     * Pattern used by all introspectors:
     * 1. Check if already discovered or currently being visited
     * 2. Mark as visiting
     * 3. Build the node (which may recursively call toRef)
     * 4. Add to discovered nodes
     * 5. Unmark as visiting
     *
     * @param decl The declaration being processed
     * @param id The TypeId for this declaration
     * @param nodeBuilder Lambda that constructs the TypeNode
     * @return true if node was created, false if already visited/in progress
     */
    protected inline fun withCycleDetection(
        decl: TDecl,
        id: TypeId,
        nodeBuilder: () -> TypeNode,
    ): Boolean {
        if (id in _nodes || decl in visitingDeclarations) {
            return false
        }

        visitingDeclarations += decl
        try {
            val node = nodeBuilder()
            _nodes[id] = node
            return true
        } finally {
            visitingDeclarations -= decl
        }
    }
}
