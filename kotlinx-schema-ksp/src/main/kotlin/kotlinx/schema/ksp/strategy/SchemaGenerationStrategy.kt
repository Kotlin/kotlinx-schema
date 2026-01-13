package kotlinx.schema.ksp.strategy

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.symbol.KSDeclaration

/**
 * Strategy interface for generating schemas from KSP declarations.
 *
 * This interface defines the contract for different schema generation strategies,
 * allowing the processor to handle different types of declarations (classes, functions)
 * and different generation patterns (top-level, extensions) through a unified interface.
 *
 * Implementations should be stateless and thread-safe.
 *
 * @param T The type of KSP declaration this strategy handles
 */
internal interface SchemaGenerationStrategy<T : KSDeclaration> {
    /**
     * Determines if this strategy applies to the given declaration.
     *
     * @param declaration The declaration to check
     * @return true if this strategy should handle the declaration, false otherwise
     */
    fun appliesTo(declaration: T): Boolean

    /**
     * Generates the schema string for the declaration.
     *
     * @param declaration The declaration to generate schema for
     * @param context The generation context with options and configuration
     * @return The generated schema as a JSON string
     */
    fun generateSchema(
        declaration: T,
        context: CodeGenerationContext,
    ): String

    /**
     * Generates the Kotlin source code file(s) for the declaration.
     *
     * This method is responsible for creating the actual .kt files with extension
     * properties or functions that provide access to the schema.
     *
     * @param declaration The declaration to generate code for
     * @param schemaString The pre-generated schema JSON string
     * @param context The generation context with options and configuration
     * @param codeGenerator The KSP code generator for creating files
     */
    fun generateCode(
        declaration: T,
        schemaString: String,
        context: CodeGenerationContext,
        codeGenerator: CodeGenerator,
    )
}
