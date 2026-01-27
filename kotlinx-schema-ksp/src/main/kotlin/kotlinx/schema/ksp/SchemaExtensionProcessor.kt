package kotlinx.schema.ksp

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.validate
import kotlinx.schema.ksp.functions.CompanionFunctionStrategy
import kotlinx.schema.ksp.functions.InstanceFunctionStrategy
import kotlinx.schema.ksp.functions.ObjectFunctionStrategy
import kotlinx.schema.ksp.functions.TopLevelFunctionStrategy
import kotlinx.schema.ksp.strategy.CodeGenerationContext
import kotlinx.schema.ksp.type.ClassSchemaStrategy

/**
 * KSP processor that generates extension properties for classes and functions,
 * annotated with `@Schema`.
 *
 * For a class annotated with @Schema, this processor generates an extension property:
 * ```kotlin
 * val MyClass.jsonSchemaString: String get() = "..."
 * ```
 */
internal class SchemaExtensionProcessor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger,
    private val options: Map<String, String>,
) : SymbolProcessor {
    internal companion object {
        private const val KOTLINX_SCHEMA_ANNOTATION = "kotlinx.schema.Schema"

        const val PARAM_WITH_SCHEMA_OBJECT = "withSchemaObject"

        const val PARAM_VISIBILITY = "visibility"

        /**
         * A constant representing a configuration key used to specify whether schema generation should include
         * an extension property that provides a schema as a Kotlin object,e.g. `JsonObject`.
         *
         * When enabled (set to "true"), the generated code will include an additional extension property for
         * the target class, allowing direct access to the schema as Kotlin object. Otherwise, only the stringified
         * JSON schema will be generated.
         *
         * This value is typically expected to be provided as an option to the KSP processor and defaults to "false".
         */
        const val OPTION_WITH_SCHEMA_OBJECT = "kotlinx.schema.$PARAM_WITH_SCHEMA_OBJECT"

        /**
         * Key used to enable or disable the functionality of the schema generation plugin.
         *
         * If this constant is set to "false" in the processor options, the plugin will be disabled and
         * schema generation will be skipped. Any other value or the absence of this key in the options
         * will default to enabling the plugin.
         *
         * This parameter can be configured in the KSP processor's options.
         */
        const val OPTION_ENABLED = "kotlinx.schema.enabled"

        /**
         * Represents the key used to retrieve the root package name for schema generation
         * from the compiler options passed to the plugin. This option allows users to specify
         * a base package, restricting schema processing to classes contained within it or its subpackages.
         *
         * Usage of this parameter is optional; if not provided, no package-based filtering is applied.
         * When specified, only classes within the defined root package or its subpackages will be processed.
         */
        const val OPTION_ROOT_PACKAGE = "kotlinx.schema.rootPackage"

        const val OPTION_VISIBILITY = "kotlinx.schema.$PARAM_VISIBILITY"
    }

    // Strategy instances for different declaration types
    private val classStrategy = ClassSchemaStrategy()
    private val functionStrategies =
        listOf(
            TopLevelFunctionStrategy(),
            InstanceFunctionStrategy(),
            CompanionFunctionStrategy(),
            ObjectFunctionStrategy(),
        )

    override fun finish() {
        logger.info("[kotlinx-schema] ✅ Done!")
    }

    override fun onError() {
        logger.error(
            "[kotlinx-schema] 💥 Error! KSP Processor Options: ${
                options.entries.joinToString(
                    prefix = "[",
                    separator = ", ",
                    postfix = "]",
                ) { it.toString() }
            }",
        )
    }

    override fun process(resolver: Resolver): List<KSAnnotated> {
        val schemaAnnotationName = KOTLINX_SCHEMA_ANNOTATION
        val symbols = resolver.getSymbolsWithAnnotation(schemaAnnotationName)
        val ret = mutableListOf<KSAnnotated>()

        val enabled = options[OPTION_ENABLED]?.trim()?.takeIf { it.isNotEmpty() } != "false"
        val rootPackage = options[OPTION_ROOT_PACKAGE]?.trim()?.takeIf { it.isNotEmpty() }
        logger.info("[kotlinx-schema] Options: ${options.entries.joinToString()} | rootPackage=$rootPackage")

        if (!enabled) {
            logger.info("[kotlinx-schema] Plugin disabled")
            return emptyList()
        }

        // Process classes annotated with @Schema
        processClassDeclarations(symbols, ret, rootPackage)

        // Process functions annotated with @Schema
        processFunctionDeclarations(symbols, ret, rootPackage)

        return ret
    }

    private fun processFunctionDeclarations(
        symbols: Sequence<KSAnnotated>,
        ret: MutableList<KSAnnotated>,
        rootPackage: String?,
    ) {
        symbols
            .filterIsInstance<KSFunctionDeclaration>()
            .filter { filterByRootPackage(it, rootPackage, logger) }
            .forEach { functionDeclaration ->
                if (!functionDeclaration.validate()) {
                    ret.add(functionDeclaration)
                    return@forEach
                }

                @Suppress("TooGenericExceptionCaught")
                try {
                    generateFunctionSchemaExtension(functionDeclaration)
                } catch (e: Exception) {
                    logger.error(
                        "Failed to generate function schema extension " +
                            "for ${functionDeclaration.qualifiedName?.asString()}: ${e.message}",
                    )
                }
            }
    }

    /**
     * Processes a sequence of class declarations, validating and generating schema
     * extensions for eligible classes. Any invalid or ignored classes are added
     * to the provided list for further handling. Optionally filters by a specified
     * root package.
     *
     * @param symbols The sequence of annotated symbols to process, expected to
     *     include class declarations.
     * @param unprocessable A mutable list to collect invalid or unprocessed class declarations.
     * @param rootPackage The optional root package name to constrain class processing.
     *     Classes outside this package (or its subpackages) are skipped.
     */
    private fun processClassDeclarations(
        symbols: Sequence<KSAnnotated>,
        unprocessable: MutableList<KSAnnotated>,
        rootPackage: String?,
    ) {
        symbols
            .filterIsInstance<KSClassDeclaration>()
            .filter { filterByRootPackage(it, rootPackage, logger) }
            .forEach { classDeclaration ->
                if (!classDeclaration.validate()) {
                    unprocessable.add(classDeclaration)
                    return@forEach
                }

                @Suppress("TooGenericExceptionCaught")
                try {
                    generateSchemaExtension(classDeclaration)
                } catch (e: Exception) {
                    logger.error(
                        "Failed to generate schema extension " +
                            "for ${classDeclaration.qualifiedName?.asString()}: ${e.message}",
                    )
                }
            }
    }

    private fun generateSchemaExtension(classDeclaration: KSClassDeclaration) {
        val className = classDeclaration.simpleName.asString()
        val packageName = classDeclaration.packageName.asString()
        val parameters =
            getSchemaParameters(
                classDeclaration,
                KOTLINX_SCHEMA_ANNOTATION,
                schemaAnnotationDefaults,
            )
        logger.info("Parameters = $parameters")

        val qualifiedName = classDeclaration.qualifiedName?.asString() ?: "$packageName.$className"

        // Create context for strategy
        val context = CodeGenerationContext(options, parameters, logger)

        // Generate schema using the strategy
        val schemaString = classStrategy.generateSchema(classDeclaration, context)

        // Generate code using the strategy
        classStrategy.generateCode(classDeclaration, schemaString, context, codeGenerator)

        logger.info("Generated schema extension for $qualifiedName")
    }

    /**
     * Gets the default parameter values from the Schema annotation class using KSP symbol processing
     */
    private val schemaAnnotationDefaults: Map<String, Any?> =
        mapOf(
            "value" to "json", // Default from Schema annotation
            OPTION_WITH_SCHEMA_OBJECT to false, // Default from Schema annotation
        )

    private fun generateFunctionSchemaExtension(functionDeclaration: KSFunctionDeclaration) {
        val functionName = functionDeclaration.simpleName.asString()
        val packageName = functionDeclaration.packageName.asString()
        val parameters =
            getSchemaParameters(
                functionDeclaration,
                KOTLINX_SCHEMA_ANNOTATION,
                schemaAnnotationDefaults,
            )
        logger.info("Function Parameters = $parameters")

        val qualifiedName = functionDeclaration.qualifiedName?.asString() ?: "$packageName.$functionName"

        // Create context for strategy
        val context = CodeGenerationContext(options, parameters, logger)

        // Find the appropriate strategy for this function
        val strategy =
            functionStrategies.firstOrNull { it.appliesTo(functionDeclaration) }
                ?: run {
                    logger.warn(
                        "No strategy found for function: $qualifiedName, falling back to TopLevelFunctionStrategy",
                    )
                    functionStrategies.first() // TopLevelFunctionStrategy is first
                }

        // Generate schema using the strategy
        val schemaString = strategy.generateSchema(functionDeclaration, context)

        // Generate code using the strategy
        strategy.generateCode(functionDeclaration, schemaString, context, codeGenerator)

        logger.info("Generated function schema for $qualifiedName using ${strategy::class.simpleName}")
    }
}
