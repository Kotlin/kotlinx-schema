package kotlinx.schema.ksp

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.validate

/**
 * KSP processor that generates extension properties for classes annotated with @Schema.
 *
 * For a class annotated with @Schema, this processor generates an extension property:
 * ```kotlin
 * val MyClass.jsonSchemaString: String get() = "..."
 * ```
 */
@Suppress("TooManyFunctions")
internal class SchemaExtensionProcessor(
    private val codeGenerator: CodeGenerator,
    private val classSourceCodeGenerator: ClassSourceCodeGenerator = ClassSourceCodeGenerator,
    private val functionSourceCodeGenerator: FunctionSourceCodeGenerator = FunctionSourceCodeGenerator,
    private val logger: KSPLogger,
    private val options: Map<String, String>,
) : SymbolProcessor {
    private companion object {
        private const val KOTLINX_SCHEMA_ANNOTATION = "kotlinx.schema.Schema"

        private const val PARAM_WITH_SCHEMA_OBJECT = "withSchemaObject"

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
        private const val OPTION_WITH_SCHEMA_OBJECT = "kotlinx.schema.$PARAM_WITH_SCHEMA_OBJECT"

        /**
         * Key used to enable or disable the functionality of the schema generation plugin.
         *
         * If this constant is set to "false" in the processor options, the plugin will be disabled and
         * schema generation will be skipped. Any other value or the absence of this key in the options
         * will default to enabling the plugin.
         *
         * This parameter can be configured in the KSP processor's options.
         */
        private const val OPTION_ENABLED = "kotlinx.schema.enabled"

        /**
         * Represents the key used to retrieve the root package name for schema generation
         * from the compiler options passed to the plugin. This option allows users to specify
         * a base package, restricting schema processing to classes contained within it or its subpackages.
         *
         * Usage of this parameter is optional; if not provided, no package-based filtering is applied.
         * When specified, only classes within the defined root package or its subpackages will be processed.
         */
        private const val OPTION_ROOT_PACKAGE = "kotlinx.schema.rootPackage"
    }

    private val schemaGenerator = KspClassSchemaGenerator()
    private val functionSchemaGenerator = KspFunctionSchemaGenerator()

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

    @Suppress("LongMethod")
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

    /**
     * Filters a declaration based on whether it resides within the specified root package.
     * Logs a message if the declaration is skipped for being outside the root package.
     *
     * @param declaration The Kotlin Symbol Processing (KSP) declaration to filter.
     * @param rootPackage The optional root package name used to constrain the filtering.
     *                     If null, no filtering is applied based on the root package.
     * @return `true` if the declaration is in the specified root package or no root package is specified,
     *         `false` otherwise.
     */
    private fun filterByRootPackage(
        declaration: KSDeclaration,
        rootPackage: String?,
    ): Boolean {
        if (rootPackage != null) {
            val pkg = declaration.packageName.asString()
            val inRoot = pkg == rootPackage || pkg.startsWith("$rootPackage.")
            if (!inRoot) {
                logger.info(
                    "[kotlinx-schema] Skipping ${declaration.qualifiedName?.asString()} " +
                        "as it is outside rootPackage '$rootPackage'",
                )
                return false
            }
        }
        return true
    }

    private fun processFunctionDeclarations(
        symbols: Sequence<KSAnnotated>,
        ret: MutableList<KSAnnotated>,
        rootPackage: String?,
    ) {
        symbols
            .filterIsInstance<KSFunctionDeclaration>()
            .filter { filterByRootPackage(it, rootPackage) }
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
                    e.printStackTrace()
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
     * @param ret A mutable list to collect invalid or unprocessed class declarations.
     * @param rootPackage The optional root package name to constrain class processing.
     *     Classes outside this package (or its subpackages) are skipped.
     */
    private fun processClassDeclarations(
        symbols: Sequence<KSAnnotated>,
        ret: MutableList<KSAnnotated>,
        rootPackage: String?,
    ) {
        symbols
            .filterIsInstance<KSClassDeclaration>()
            .filter { filterByRootPackage(it, rootPackage) }
            .forEach { classDeclaration ->
                if (!classDeclaration.validate()) {
                    ret.add(classDeclaration)
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
        val parameters = getSchemaParameters(classDeclaration)
        logger.info("Parameters = $parameters")

        val qualifiedName = classDeclaration.qualifiedName?.asString() ?: "$packageName.$className"

        // Handle generic classes by using star projection
        val typeParameters = classDeclaration.typeParameters
        val classNameWithGenerics =
            if (typeParameters.isNotEmpty()) {
                val starProjections = typeParameters.joinToString(", ") { "*" }
                "$qualifiedName<$starProjections>"
            } else {
                qualifiedName
            }

        // Generate class-specific schema string
        val schemaString = schemaGenerator.generateSchemaString(classDeclaration)

        // Create the generated file
        val fileName = "${className}SchemaExtensions"
        val classSourceFile =
            requireNotNull(classDeclaration.containingFile) {
                "Class declaration must have a containing file"
            }
        val file =
            codeGenerator.createNewFile(
                dependencies = Dependencies(true, classSourceFile),
                packageName = packageName,
                fileName = fileName,
            )

        file.use { outputStream ->
            val writer = outputStream.bufferedWriter(Charsets.UTF_8)
            writer.write(
                classSourceCodeGenerator.generateCode(
                    packageName = packageName,
                    classNameWithGenerics = classNameWithGenerics,
                    options = options,
                    parameters = parameters,
                    schemaString = schemaString,
                ),
            )
            writer.flush()
        }

        logger.info("Generated schema extension for $qualifiedName")
    }

    private fun getSchemaParameters(classDeclaration: KSClassDeclaration): Map<String, Any?> {
        val schemaAnnotation =
            classDeclaration.annotations.firstOrNull {
                it.shortName.getShortName() == "Schema"
            }
        if (schemaAnnotation == null) {
            return mapOf()
        }

        // Get default values from the Schema annotation class using reflection
        val defaultParameters = getSchemaAnnotationDefaults()

        val parameters =
            schemaAnnotation.arguments
                .mapNotNull { arg ->
                    arg.name?.getShortName()?.let { it to arg.value }
                }.toMap()
        return defaultParameters.plus(parameters)
    }

    /**
     * Gets the default parameter values from the Schema annotation class using KSP symbol processing
     */
    private fun getSchemaAnnotationDefaults(): Map<String, Any?> =
        mapOf(
            "value" to "json", // Default from Schema annotation
            OPTION_WITH_SCHEMA_OBJECT to false, // Default from Schema annotation
        )

    private fun generateFunctionSchemaExtension(functionDeclaration: KSFunctionDeclaration) {
        val functionName = functionDeclaration.simpleName.asString()
        val packageName = functionDeclaration.packageName.asString()
        val parameters = getSchemaParameters(functionDeclaration)
        logger.info("Function Parameters = $parameters")

        val qualifiedName = functionDeclaration.qualifiedName?.asString() ?: "$packageName.$functionName"

        // Generate input schema (FunctionCallingSchema format)
        val inputSchemaString = functionSchemaGenerator.generateSchemaString(functionDeclaration)

        // Create the generated file
        val fileName = "${functionName}FunctionSchema"
        val functionSourceFile =
            requireNotNull(functionDeclaration.containingFile) {
                "Function declaration must have a containing file"
            }
        val file =
            codeGenerator.createNewFile(
                dependencies = Dependencies(true, functionSourceFile),
                packageName = packageName,
                fileName = fileName,
            )

        file.use { outputStream ->
            val writer = outputStream.bufferedWriter(Charsets.UTF_8)
            writer.write(
                functionSourceCodeGenerator.generateCode(
                    packageName = packageName,
                    functionName = functionName,
                    options = options,
                    parameters = parameters,
                    inputSchemaString = inputSchemaString,
                    isExtensionFunction = functionDeclaration.extensionReceiver != null,
                    receiverType =
                        functionDeclaration.extensionReceiver
                            ?.resolve()
                            ?.declaration
                            ?.qualifiedName
                            ?.asString(),
                ),
            )
            writer.flush()
        }

        logger.info("Generated function schema for $qualifiedName")
    }

    private fun getSchemaParameters(functionDeclaration: KSFunctionDeclaration): Map<String, Any?> {
        val schemaAnnotation =
            functionDeclaration.annotations.firstOrNull {
                it.shortName.getShortName() == "Schema"
            }
        if (schemaAnnotation == null) {
            return mapOf()
        }

        // Get default values from the Schema annotation class
        val defaultParameters = getSchemaAnnotationDefaults()

        val parameters =
            schemaAnnotation.arguments
                .mapNotNull { arg ->
                    arg.name?.getShortName()?.let { it to arg.value }
                }.toMap()
        return defaultParameters.plus(parameters)
    }
}
