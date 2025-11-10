package kotlinx.schema.ksp

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.validate
import kotlinx.schema.generator.core.SchemaGenerator

private const val KOTLINX_SCHEMA_ANNOTATION = "kotlinx.schema.Schema"

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
    private val logger: KSPLogger,
    private val options: Map<String, String>,
) : SymbolProcessor {
    private val schemaGenerator: SchemaGenerator<KSClassDeclaration, out Any> = KspSchemaGenerator

    override fun process(resolver: Resolver): List<KSAnnotated> {
        val schemaAnnotationName = KOTLINX_SCHEMA_ANNOTATION
        val symbols = resolver.getSymbolsWithAnnotation(schemaAnnotationName)
        val ret = mutableListOf<KSAnnotated>()

        val enabled = options["kotlinx.schema.enabled"]?.trim()?.takeIf { it.isNotEmpty() } != "false"
        val rootPackage = options["kotlinx.schema.rootPackage"]?.trim()?.takeIf { it.isNotEmpty() }
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
                "$className<$starProjections>"
            } else {
                className
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
            val writer = outputStream.writer()
            writer.write(
                generateCode(
                    packageName = packageName,
                    className = className,
                    classNameWithGenerics = classNameWithGenerics,
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
            classDeclaration.annotations
                .firstOrNull {
                    it.shortName.getShortName() == "Schema"
                }
        if (schemaAnnotation == null) {
            return mapOf()
        }

        // Get default values from the Schema annotation class using reflection
        val defaultParameters = getSchemaAnnotationDefaults()

        val parameters =
            schemaAnnotation
                .arguments
                .filter { it.name != null }
                .associate {
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
            "withSchemaObject" to false, // Default from Schema annotation
        )

    private fun generateCode(
        packageName: String,
        className: String,
        classNameWithGenerics: String,
        parameters: Map<String, Any?>,
        schemaString: String,
    ): String =
        if (false && parameters["withSchemaObject"] == true) {
            @Suppress("RedundantVisibilityModifier")
            // language=kotlin
            """
            |@file:Suppress("UnusedReceiverParameter", "MaxLineLength")
            |
            |package $packageName
            |
            |import kotlinx.serialization.json.Json
            |import kotlinx.serialization.json.JsonObject
            |import kotlin.reflect.KClass
            |
            |/**
            | * Generated extension property providing JSON schema for $className.
            | * Generated by kotlinx-schema-ksp processor.
            | */
            |public val KClass<$classNameWithGenerics>.jsonSchemaString: String
            |    get() =
            |        // language=JSON
            |        ${schemaString.escapeForKotlinString()}
            |    
            |/**
            | * Generated extension property providing JSON schema as JsonObject for $className.
            | * Generated by kotlinx-schema-ksp processor.
            | */
            |public val KClass<$classNameWithGenerics>.jsonSchema: JsonObject
            |    get() = Json.decodeFromString<JsonObject>(jsonSchemaString)
            |
            """.trimMargin()
        } else {
            // language=kotlin
            """
            |@file:Suppress("UnusedReceiverParameter", "MaxLineLength")
            |
            |package $packageName
            |
            |import kotlinx.serialization.json.Json
            |import kotlinx.serialization.json.JsonObject
            |import kotlin.reflect.KClass
            |
            |/**
            | * Generated extension property providing JSON schema for $className.
            | * Generated by kotlinx-schema-ksp processor.
            | */
            |public val KClass<$classNameWithGenerics>.jsonSchemaString: String
            |    get() =
            |        // language=JSON
            |        ${schemaString.escapeForKotlinString()}
            |
            |/**
            | * Generated extension property providing JSON schema as JsonObject for $className.
            | * Generated by kotlinx-schema-ksp processor.
            | */
            |public val KClass<$classNameWithGenerics>.jsonSchema: JsonObject
            |    get() = Json.decodeFromString<JsonObject>(jsonSchemaString)
            |
            """.trimMargin()
        }

    /**
     * Escapes a string for use as a Kotlin raw string literal, preserving $ and triple quotes.
     */
    private fun String.escapeForKotlinString(): String {
        val escaped =
            this
                .replace("$", "\${'$'}")
                .replace("\"\"\"", "\\\"\\\"\\\"")
        return "\"\"\"" + escaped + "\"\"\""
    }
}
