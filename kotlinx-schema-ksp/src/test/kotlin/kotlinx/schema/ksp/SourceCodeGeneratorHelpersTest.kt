package kotlinx.schema.ksp

import com.google.devtools.ksp.processing.KSPLogger
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import io.mockk.mockk
import kotlinx.schema.ksp.strategy.CodeGenerationContext
import org.junit.jupiter.api.Test

/**
 * Unit tests for SourceCodeGeneratorHelpers.
 */
class SourceCodeGeneratorHelpersTest {
    private val mockLogger: KSPLogger = mockk(relaxed = true)

    @Test
    fun `buildKClassExtensions generates code with public visibility`() {
        // Given
        val context =
            CodeGenerationContext(
                options = mapOf("kotlinx.schema.visibility" to "public"),
                parameters = emptyMap(),
                logger = mockLogger,
            )

        // When
        val result =
            SourceCodeGeneratorHelpers.buildKClassExtensions(
                packageName = "com.example",
                classNameWithGenerics = "MyClass",
                functionName = "myFunction",
                schemaString = """{"type":"object"}""",
                context = context,
            )

        // Then
        result shouldContain "public fun kotlin.reflect.KClass<MyClass>.myFunctionJsonSchemaString(): String ="
    }

    @Test
    fun `buildKClassExtensions generates code with internal visibility`() {
        // Given
        val context =
            CodeGenerationContext(
                options = mapOf("kotlinx.schema.visibility" to "internal"),
                parameters = emptyMap(),
                logger = mockLogger,
            )

        // When
        val result =
            SourceCodeGeneratorHelpers.buildKClassExtensions(
                packageName = "com.example",
                classNameWithGenerics = "MyClass",
                functionName = "myFunction",
                schemaString = """{"type":"object"}""",
                context = context,
            )

        // Then
        result shouldContain "internal fun kotlin.reflect.KClass<MyClass>.myFunctionJsonSchemaString(): String ="
    }

    @Test
    fun `buildKClassExtensions generates code with private visibility`() {
        // Given
        val context =
            CodeGenerationContext(
                options = mapOf("kotlinx.schema.visibility" to "private"),
                parameters = emptyMap(),
                logger = mockLogger,
            )

        // When
        val result =
            SourceCodeGeneratorHelpers.buildKClassExtensions(
                packageName = "com.example",
                classNameWithGenerics = "MyClass",
                functionName = "myFunction",
                schemaString = """{"type":"object"}""",
                context = context,
            )

        // Then
        result shouldContain "private fun kotlin.reflect.KClass<MyClass>.myFunctionJsonSchemaString(): String ="
    }

    @Test
    fun `buildKClassExtensions generates code with no visibility modifier`() {
        // Given
        val context =
            CodeGenerationContext(
                options = mapOf("kotlinx.schema.visibility" to ""),
                parameters = emptyMap(),
                logger = mockLogger,
            )

        // When
        val result =
            SourceCodeGeneratorHelpers.buildKClassExtensions(
                packageName = "com.example",
                classNameWithGenerics = "MyClass",
                functionName = "myFunction",
                schemaString = """{"type":"object"}""",
                context = context,
            )

        // Then
        result shouldContain "fun kotlin.reflect.KClass<MyClass>.myFunctionJsonSchemaString(): String ="
        result shouldNotContain "public fun"
        result shouldNotContain "internal fun"
        result shouldNotContain "private fun"
    }

    @Test
    fun `buildKClassExtensions includes visibility in both string and object functions`() {
        // Given
        val context =
            CodeGenerationContext(
                options =
                    mapOf(
                        "kotlinx.schema.visibility" to "internal",
                        "kotlinx.schema.withSchemaObject" to "true",
                    ),
                parameters = emptyMap(),
                logger = mockLogger,
            )

        // When
        val result =
            SourceCodeGeneratorHelpers.buildKClassExtensions(
                packageName = "com.example",
                classNameWithGenerics = "MyClass",
                functionName = "myFunction",
                schemaString = """{"type":"object"}""",
                context = context,
            )

        // Then
        result shouldContain "internal fun kotlin.reflect.KClass<MyClass>.myFunctionJsonSchemaString(): String ="
        @Suppress("MaxLineLength")
        result shouldContain
            "internal fun kotlin.reflect.KClass<MyClass>.myFunctionJsonSchema(): kotlinx.serialization.json.JsonObject ="
    }
}
