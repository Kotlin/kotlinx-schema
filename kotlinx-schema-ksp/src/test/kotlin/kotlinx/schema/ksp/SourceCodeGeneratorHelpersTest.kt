package kotlinx.schema.ksp

import com.google.devtools.ksp.processing.KSPLogger
import io.kotest.matchers.shouldBe
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
    fun `visibilityPrefix for public visibility`() {
        // Given
        val context =
            CodeGenerationContext(
                options = mapOf("kotlinx.schema.visibility" to "public"),
                parameters = emptyMap(),
                logger = mockLogger,
            )

        // When
        val result = SourceCodeGeneratorHelpers.visibilityPrefix(context)

        // Then
        result shouldBe "public "
    }

    @Test
    fun `visibilityPrefix for internal visibility`() {
        // Given
        val context =
            CodeGenerationContext(
                options = mapOf("kotlinx.schema.visibility" to "internal"),
                parameters = emptyMap(),
                logger = mockLogger,
            )

        // When
        val result = SourceCodeGeneratorHelpers.visibilityPrefix(context)

        // Then
        result shouldBe "internal "
    }

    @Test
    fun `visibilityPrefix for private visibility`() {
        // Given
        val context =
            CodeGenerationContext(
                options = mapOf("kotlinx.schema.visibility" to "private"),
                parameters = emptyMap(),
                logger = mockLogger,
            )

        // When
        val result = SourceCodeGeneratorHelpers.visibilityPrefix(context)

        // Then
        result shouldBe "private "
    }

    @Test
    fun `visibilityPrefix for empty visibility`() {
        // Given
        val context =
            CodeGenerationContext(
                options = mapOf("kotlinx.schema.visibility" to ""),
                parameters = emptyMap(),
                logger = mockLogger,
            )

        // When
        val result = SourceCodeGeneratorHelpers.visibilityPrefix(context)

        // Then
        result shouldBe ""
    }

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
        result shouldContain
            "internal fun kotlin.reflect.KClass<MyClass>.myFunctionJsonSchema(): kotlinx.serialization.json.JsonObject ="
    }

    @Test
    fun `shouldGenerateSchemaObject returns true when annotation parameter is true`() {
        // When
        val result =
            SourceCodeGeneratorHelpers.shouldGenerateSchemaObject(
                options = emptyMap(),
                parameters = mapOf("withSchemaObject" to true),
            )

        // Then
        result shouldBe true
    }

    @Test
    fun `shouldGenerateSchemaObject returns false when annotation parameter is false`() {
        // When
        val result =
            SourceCodeGeneratorHelpers.shouldGenerateSchemaObject(
                options = emptyMap(),
                parameters = mapOf("withSchemaObject" to false),
            )

        // Then
        result shouldBe false
    }

    @Test
    fun `shouldGenerateSchemaObject returns true when option is true`() {
        // When
        val result =
            SourceCodeGeneratorHelpers.shouldGenerateSchemaObject(
                options = mapOf("kotlinx.schema.withSchemaObject" to "true"),
                parameters = emptyMap(),
            )

        // Then
        result shouldBe true
    }

    @Test
    fun `shouldGenerateSchemaObject returns false when option is false`() {
        // When
        val result =
            SourceCodeGeneratorHelpers.shouldGenerateSchemaObject(
                options = mapOf("kotlinx.schema.withSchemaObject" to "false"),
                parameters = emptyMap(),
            )

        // Then
        result shouldBe false
    }

    @Test
    fun `shouldGenerateSchemaObject annotation parameter takes precedence over option`() {
        // When - annotation parameter is false, option is true
        val result1 =
            SourceCodeGeneratorHelpers.shouldGenerateSchemaObject(
                options = mapOf("kotlinx.schema.withSchemaObject" to "true"),
                parameters = mapOf("withSchemaObject" to false),
            )

        // Then - annotation parameter wins
        result1 shouldBe false

        // When - annotation parameter is true, option is false
        val result2 =
            SourceCodeGeneratorHelpers.shouldGenerateSchemaObject(
                options = mapOf("kotlinx.schema.withSchemaObject" to "false"),
                parameters = mapOf("withSchemaObject" to true),
            )

        // Then - annotation parameter wins
        result2 shouldBe true
    }

    @Test
    fun `shouldGenerateSchemaObject returns false by default when neither is set`() {
        // When
        val result =
            SourceCodeGeneratorHelpers.shouldGenerateSchemaObject(
                options = emptyMap(),
                parameters = emptyMap(),
            )

        // Then
        result shouldBe false
    }
}
