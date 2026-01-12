package kotlinx.schema.ksp

import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import org.junit.jupiter.api.Test

/**
 * Unit tests for ClassSourceCodeGenerator.
 *
 * Tests verify the precedence logic for withSchemaObject option:
 * - Global KSP option takes precedence over annotation parameter
 * - Annotation parameter is used as fallback when global option is not set
 */
class SourceCodeGeneratorTest {
    @Test
    fun `should generate only jsonSchemaString by default`() {
        // Given
        val options = emptyMap<String, String>()
        val parameters = emptyMap<String, Any?>()

        // When
        val code =
            ClassSourceCodeGenerator.generateCode(
                packageName = "com.example",
                classNameWithGenerics = "com.example.TestClass",
                options = options,
                parameters = parameters,
                schemaString = """{"type":"object"}""",
            )

        // Then
        code shouldContain "package com.example"
        code shouldContain "val kotlin.reflect.KClass<com.example.TestClass>.jsonSchemaString: String"
        code shouldNotContain
            "val kotlin.reflect.KClass<com.example.TestClass>.jsonSchema: kotlinx.serialization.json.JsonObject"
    }

    @Test
    fun `global option true overrides annotation false (precedence)`() {
        // Given
        val options = mapOf("kotlinx.schema.withSchemaObject" to "true") // Global says true
        val parameters = mapOf("withSchemaObject" to false) // Annotation says false

        // When
        val code =
            ClassSourceCodeGenerator.generateCode(
                packageName = "com.example",
                classNameWithGenerics = "com.example.TestClass",
                options = options,
                parameters = parameters,
                schemaString = """{"type":"object"}""",
            )

        // Then - global option takes precedence, generates both properties
        code shouldContain "val kotlin.reflect.KClass<com.example.TestClass>.jsonSchemaString: String"
        code shouldContain
            "val kotlin.reflect.KClass<com.example.TestClass>.jsonSchema: kotlinx.serialization.json.JsonObject"
    }

    @Test
    fun `global option false overrides annotation true (precedence)`() {
        // Given
        val options = mapOf("kotlinx.schema.withSchemaObject" to "false") // Global says false
        val parameters = mapOf("withSchemaObject" to true) // Annotation says true

        // When
        val code =
            ClassSourceCodeGenerator.generateCode(
                packageName = "com.example",
                classNameWithGenerics = "com.example.TestClass",
                options = options,
                parameters = parameters,
                schemaString = """{"type":"object"}""",
            )

        // Then - global option takes precedence, generates only jsonSchemaString
        code shouldContain "val kotlin.reflect.KClass<com.example.TestClass>.jsonSchemaString: String"
        code shouldNotContain
            "val kotlin.reflect.KClass<com.example.TestClass>.jsonSchema: kotlinx.serialization.json.JsonObject"
    }

    @Test
    fun `should generate both properties when annotation parameter is true`() {
        // Given
        val options = emptyMap<String, String>() // No global option
        val parameters = mapOf("withSchemaObject" to true) // Annotation says true

        // When
        val code =
            ClassSourceCodeGenerator.generateCode(
                packageName = "com.example",
                classNameWithGenerics = "com.example.TestClass",
                options = options,
                parameters = parameters,
                schemaString = """{"type":"object"}""",
            )

        // Then
        code shouldContain "val kotlin.reflect.KClass<com.example.TestClass>.jsonSchemaString: String"
        code shouldContain
            "val kotlin.reflect.KClass<com.example.TestClass>.jsonSchema: kotlinx.serialization.json.JsonObject"
    }

    @Test
    fun `should generate only jsonSchemaString when annotation parameter is false`() {
        // Given
        val options = emptyMap<String, String>() // No global option
        val parameters = mapOf("withSchemaObject" to false) // Annotation says false

        // When
        val code =
            ClassSourceCodeGenerator.generateCode(
                packageName = "com.example",
                classNameWithGenerics = "com.example.TestClass",
                options = options,
                parameters = parameters,
                schemaString = """{"type":"object"}""",
            )

        // Then
        code shouldContain "val kotlin.reflect.KClass<com.example.TestClass>.jsonSchemaString: String"
        code shouldNotContain
            "val kotlin.reflect.KClass<com.example.TestClass>.jsonSchema: kotlinx.serialization.json.JsonObject"
    }

    @Test
    fun `should handle generic classes with star projections`() {
        // Given
        val options = emptyMap<String, String>()
        val parameters = emptyMap<String, Any?>()

        // When - single type parameter
        val codeSingle =
            ClassSourceCodeGenerator.generateCode(
                packageName = "com.example",
                classNameWithGenerics = "com.example.Container<*>",
                options = options,
                parameters = parameters,
                schemaString = """{"type":"object"}""",
            )

        // When - multiple type parameters
        val codeMultiple =
            ClassSourceCodeGenerator.generateCode(
                packageName = "com.example",
                classNameWithGenerics = "com.example.Result<*, *>",
                options = options,
                parameters = parameters,
                schemaString = """{"type":"object"}""",
            )

        // Then
        codeSingle shouldContain "val kotlin.reflect.KClass<com.example.Container<*>>.jsonSchemaString: String"
        codeMultiple shouldContain "val kotlin.reflect.KClass<com.example.Result<*, *>>.jsonSchemaString: String"
    }

    @Test
    fun `should generate well-structured and properly formatted code`() {
        // Given
        val optionsWithSchemaObject = mapOf("kotlinx.schema.withSchemaObject" to "true")
        val optionsBasic = emptyMap<String, String>()
        val parameters = emptyMap<String, Any?>()

        // =================================================================
        // Test 1: Package and class reference generation
        // =================================================================
        val codeWithCustomPackage =
            ClassSourceCodeGenerator.generateCode(
                packageName = "com.example.models",
                classNameWithGenerics = "com.example.models.UserModel",
                options = optionsBasic,
                parameters = parameters,
                schemaString = """{"type":"object"}""",
            )

        // Should generate correct package declaration
        codeWithCustomPackage shouldContain "package com.example.models"
        // Should generate extension with fully qualified class name
        codeWithCustomPackage shouldContain
            "val kotlin.reflect.KClass<com.example.models.UserModel>.jsonSchemaString: String"

        // =================================================================
        // Test 2: String escaping and wrapping
        // =================================================================
        val codeWithDollarSign =
            ClassSourceCodeGenerator.generateCode(
                packageName = "com.example",
                classNameWithGenerics = "com.example.TestClass",
                options = optionsBasic,
                parameters = parameters,
                schemaString = $$"""{"$id":"test"}""",
            )

        val codeWithTripleQuotes =
            ClassSourceCodeGenerator.generateCode(
                packageName = "com.example",
                classNameWithGenerics = "com.example.TestClass",
                options = optionsBasic,
                parameters = parameters,
                schemaString = """{"description":"Use \"\"\" for strings"}""",
            )

        // Should escape dollar signs for Kotlin string interpolation
        codeWithDollarSign shouldContain $$"${'$'}id"
        // Should escape triple quotes to prevent breaking raw string literal
        codeWithTripleQuotes shouldContain "\\\"\\\"\\\" for strings"
        // Should wrap schema in triple quotes (raw string literal)
        codeWithTripleQuotes shouldContain "\"\"\""

        // =================================================================
        // Test 3: Code structure - suppressions, markers, and documentation
        // =================================================================
        val codeWithBothProperties =
            ClassSourceCodeGenerator.generateCode(
                packageName = "com.example",
                classNameWithGenerics = "com.example.TestClass",
                options = optionsWithSchemaObject,
                parameters = parameters,
                schemaString = """{"type":"object"}""",
            )

        // Should include file-level suppressions for generated code
        codeWithBothProperties shouldContain "@file:Suppress("
        codeWithBothProperties shouldContain "MaxLineLength"
        codeWithBothProperties shouldContain "RedundantVisibilityModifier"
        codeWithBothProperties shouldContain "RemoveRedundantQualifierName"
        codeWithBothProperties shouldContain "UnusedReceiverParameter"

        // Should include language marker for IDE syntax highlighting
        codeWithBothProperties shouldContain "// language=JSON"

        // Should include KDoc comments for generated properties
        codeWithBothProperties shouldContain
            "Generated extension property providing JSON schema for [com.example.TestClass]"
        codeWithBothProperties shouldContain
            "Generated extension property providing JSON schema as JsonObject for [com.example.TestClass]"
        codeWithBothProperties shouldContain "Generated by kotlinx-schema-ksp processor"
    }

    @Test
    fun `should handle any string value for global option that is not true as false`() {
        // Given
        val options = mapOf("kotlinx.schema.withSchemaObject" to "maybe") // Not "true"
        val parameters = emptyMap<String, Any?>()

        // When
        val code =
            ClassSourceCodeGenerator.generateCode(
                packageName = "com.example",
                classNameWithGenerics = "com.example.TestClass",
                options = options,
                parameters = parameters,
                schemaString = """{"type":"object"}""",
            )

        // Then - should only generate jsonSchemaString because option is not "true"
        code shouldContain "val kotlin.reflect.KClass<com.example.TestClass>.jsonSchemaString: String"
        code shouldNotContain
            "val kotlin.reflect.KClass<com.example.TestClass>.jsonSchema: kotlinx.serialization.json.JsonObject"
    }
}
