package kotlinx.schema.ksp

import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import org.junit.jupiter.api.Test

/**
 * Unit tests for FunctionSourceCodeGenerator.
 *
 * Tests function-specific code generation:
 * - Extension function handling (receiver type prefix)
 * - Regular vs extension function logic
 *
 * Note: Precedence logic and escaping are tested in SourceCodeGeneratorHelpersTest.
 */
class FunctionSourceCodeGeneratorTest {
    @Test
    fun `should generate extension functions for extension function schemas`() {
        // Given
        val options = emptyMap<String, String>()
        val parameters = emptyMap<String, Any?>()

        // When
        val code =
            FunctionSourceCodeGenerator.generateCode(
                packageName = "com.example",
                functionName = "testExtension",
                options = options,
                parameters = parameters,
                inputSchemaString = """{"type":"function"}""",
                isExtensionFunction = true,
                receiverType = "String",
            )

        // Then
        code shouldContain "package com.example"
        code shouldContain "fun String.testExtensionJsonSchemaString(): String"
        code shouldNotContain "fun String.testExtensionJsonSchema()"
    }

    @Test
    fun `should generate extension functions with schema object when enabled`() {
        // Given
        val options = mapOf("kotlinx.schema.withSchemaObject" to "true")
        val parameters = emptyMap<String, Any?>()

        // When
        val code =
            FunctionSourceCodeGenerator.generateCode(
                packageName = "com.example",
                functionName = "testExtension",
                options = options,
                parameters = parameters,
                inputSchemaString = """{"type":"function"}""",
                isExtensionFunction = true,
                receiverType = "String",
            )

        // Then
        code shouldContain "fun String.testExtensionJsonSchemaString(): String"
        code shouldContain "fun String.testExtensionJsonSchema(): kotlinx.serialization.json.JsonObject"
        code shouldContain "Json.decodeFromString(String.testExtensionJsonSchemaString())"
    }

    @Test
    fun `should ignore receiverType when isExtensionFunction is false`() {
        // Given
        val options = emptyMap<String, String>()
        val parameters = emptyMap<String, Any?>()

        // When
        val code =
            FunctionSourceCodeGenerator.generateCode(
                packageName = "com.example",
                functionName = "regularFunction",
                options = options,
                parameters = parameters,
                inputSchemaString = """{"type":"function"}""",
                isExtensionFunction = false,
                receiverType = "String", // Should be ignored
            )

        // Then - receiverType should be ignored
        code shouldContain "fun regularFunctionJsonSchemaString(): String"
        code shouldNotContain "String.regularFunctionJsonSchemaString"
    }
}
