package kotlinx.schema.ksp

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.validate

/**
 * KSP processor that generates extension properties for classes annotated with @Schema.
 *
 * For a class annotated with @Schema, this processor generates an extension property:
 * ```kotlin
 * val MyClass.jsonSchemaString: String get() = "..."
 * ```
 */
internal class SchemaExtensionProcessor(
    private val codeGenerator: CodeGenerator,
    private val sourceCoGenerator: SourceCodeGenerator,
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

        symbols.filterIsInstance<KSClassDeclaration>().forEach { classDeclaration ->
            if (!classDeclaration.validate()) {
                ret.add(classDeclaration)
                return@forEach
            }

            // If a root package is specified, skip classes outside of it
            if (rootPackage != null) {
                val pkg = classDeclaration.packageName.asString()
                val inRoot = pkg == rootPackage || pkg.startsWith("$rootPackage.")
                if (!inRoot) {
                    logger.info(
                        "[kotlinx-schema] Skipping ${classDeclaration.qualifiedName?.asString()} " +
                            "as it is outside rootPackage '$rootPackage'",
                    )
                    return@forEach
                }
            }

            @Suppress("TooGenericExceptionCaught")
            try {
                generateSchemaExtension(classDeclaration)
            } catch (e: Exception) {
                logger.error(
                    "Failed to generate schema extension " +
                        "for ${classDeclaration.qualifiedName?.asString()}: ${e.message}",
                )
                e.printStackTrace()
            }
        }

        return ret
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
        val file =
            codeGenerator.createNewFile(
                dependencies = Dependencies(true, classDeclaration.containingFile!!),
                packageName = packageName,
                fileName = fileName,
            )

        file.use { outputStream ->
            val writer = outputStream.bufferedWriter(Charsets.UTF_8)
            writer.write(
                sourceCoGenerator.generateCode(
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
            schemaAnnotation.arguments.filter { it.name != null }.associate {
                it.name!!.getShortName() to it.value
            }
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
}
