package kotlinx.schema.ksp

import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSName
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SymbolFilterTest {
    private val logger = mockk<KSPLogger>(relaxed = true)

    private fun mockClass(qualifiedName: String): KSClassDeclaration {
        val pkg = qualifiedName.substringBeforeLast('.', "")
        return mockk<KSClassDeclaration> {
            every { this@mockk.qualifiedName } returns mockk<KSName> { every { asString() } returns qualifiedName }
            every { packageName } returns mockk<KSName> { every { asString() } returns pkg }
        }
    }

    private fun mockFunction(qualifiedName: String): KSFunctionDeclaration {
        val pkg = qualifiedName.substringBeforeLast('.', "")
        return mockk<KSFunctionDeclaration> {
            every { this@mockk.qualifiedName } returns mockk<KSName> { every { asString() } returns qualifiedName }
            every { packageName } returns mockk<KSName> { every { asString() } returns pkg }
        }
    }

    private inline fun <reified T : KSDeclaration> filterSymbols(
        symbols: List<T>,
        rootPackage: String? = null,
        include: List<String> = emptyList(),
        exclude: List<String> = emptyList(),
    ): List<String> =
        SymbolFilter(rootPackage, include, exclude, logger)
            .filter<T>(symbols.asSequence())
            .map { it.qualifiedName!!.asString() }
            .toList()

    //region globToRegex

    @ParameterizedTest(name = "glob `{0}` on `{1}` → {2}")
    @MethodSource("globMatchCases")
    fun `globToRegex matches correctly`(
        glob: String,
        input: String,
        expected: Boolean,
    ) {
        globToRegex(glob).matches(input) shouldBe expected
    }

    fun globMatchCases() =
        listOf(
            // single star — matches within one dot-segment only
            Arguments.of("com.example.*", "com.example.Foo", true),
            Arguments.of("com.example.*", "com.example.FooBar", true),
            Arguments.of("com.example.*", "com.example.sub.Foo", false),
            Arguments.of("com.example.*", "com.other.Foo", false),
            // double star — matches across dots
            Arguments.of("com.example.**", "com.example.Foo", true),
            Arguments.of("com.example.**", "com.example.sub.Foo", true),
            Arguments.of("com.example.**", "com.example.a.b.c.Foo", true),
            Arguments.of("com.example.**", "com.other.Foo", false),
            // suffix glob
            Arguments.of("**.*Dto", "com.example.UserDto", true),
            Arguments.of("**.*Dto", "com.example.sub.OrderDto", true),
            Arguments.of("**.*Dto", "com.example.UserService", false),
            // question mark — single non-dot character
            Arguments.of("com.example.Fo?", "com.example.Foo", true),
            Arguments.of("com.example.Fo?", "com.example.For", true),
            Arguments.of("com.example.Fo?", "com.example.Fo", false),
            Arguments.of("com.example.Fo?", "com.example.Fooo", false),
            Arguments.of("com.example.Fo?", "com.example.F.o", false),
            // exact match
            Arguments.of("com.example.MyClass", "com.example.MyClass", true),
            Arguments.of("com.example.MyClass", "com.example.MyClassX", false),
            Arguments.of("com.example.MyClass", "com.other.MyClass", false),
        )

    //endregion

    //region filterClasses and filterFunctions — pattern filtering

    data class FilterCase(
        val description: String,
        val allSymbols: List<String>,
        val rootPackage: String? = null,
        val include: List<String> = emptyList(),
        val exclude: List<String> = emptyList(),
        val expected: List<String>,
    ) {
        override fun toString() = description
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("filterCases")
    fun `filter KSClassDeclaration filters correctly`(case: FilterCase) {
        filterSymbols<KSClassDeclaration>(
            case.allSymbols.map { mockClass(it) },
            case.rootPackage,
            case.include,
            case.exclude,
        ) shouldContainExactlyInAnyOrder case.expected
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("filterCases")
    fun `filter KSFunctionDeclaration filters correctly`(case: FilterCase) {
        filterSymbols<KSFunctionDeclaration>(
            case.allSymbols.map { mockFunction(it) },
            case.rootPackage,
            case.include,
            case.exclude,
        ) shouldContainExactlyInAnyOrder case.expected
    }

    fun filterCases() =
        listOf(
            FilterCase(
                description = "no patterns passes all symbols",
                allSymbols = listOf("com.example.Foo", "com.other.Bar"),
                expected = listOf("com.example.Foo", "com.other.Bar"),
            ),
            FilterCase(
                description = "rootPackage filters out symbols outside the package",
                allSymbols = listOf("com.example.Foo", "com.example.sub.Bar", "com.other.Baz"),
                rootPackage = "com.example",
                expected = listOf("com.example.Foo", "com.example.sub.Bar"),
            ),
            FilterCase(
                description = "include pattern restricts to matching symbols",
                allSymbols = listOf("com.example.UserDto", "com.example.OrderDto", "com.example.UserService"),
                include = listOf("**.*Dto"),
                expected = listOf("com.example.UserDto", "com.example.OrderDto"),
            ),
            FilterCase(
                description = "exclude pattern removes matching symbols",
                allSymbols =
                    listOf(
                        "com.example.UserDto",
                        "com.example.internal.InternalHelper",
                        "com.example.OrderDto",
                    ),
                exclude = listOf("**.internal.**"),
                expected = listOf("com.example.UserDto", "com.example.OrderDto"),
            ),
            FilterCase(
                description = "exclude wins over include for matched symbols",
                allSymbols =
                    listOf(
                        "com.example.UserDto",
                        "com.example.internal.InternalDto",
                        "com.example.OrderDto",
                    ),
                include = listOf("**.*Dto"),
                exclude = listOf("**.internal.**"),
                expected = listOf("com.example.UserDto", "com.example.OrderDto"),
            ),
            FilterCase(
                description = "multiple include patterns - symbol matching any one passes",
                allSymbols = listOf("com.example.UserDto", "com.example.OrderService", "com.example.Unrelated"),
                include = listOf("**.*Dto", "**.*Service"),
                expected = listOf("com.example.UserDto", "com.example.OrderService"),
            ),
        )

    //endregion

    //region Structural edge cases

    @Test
    fun `no include match results in empty output`() {
        val classes = listOf(mockClass("com.example.UserService"), mockClass("com.example.OrderService"))
        filterSymbols<KSClassDeclaration>(classes, include = listOf("**.*Dto")).shouldBeEmpty()
    }

    @Test
    fun `filter drops non-matching declaration types`() {
        val classDecl = mockClass("com.example.Foo")
        val funcDecl = mockFunction("com.example.bar")
        val symbolFilter = SymbolFilter(null, emptyList(), emptyList(), logger)
        symbolFilter
            .filter<KSClassDeclaration>(sequenceOf(classDecl, funcDecl))
            .map { it.qualifiedName!!.asString() }
            .toList() shouldContainExactlyInAnyOrder listOf("com.example.Foo")
        symbolFilter
            .filter<KSFunctionDeclaration>(sequenceOf(classDecl, funcDecl))
            .map { it.qualifiedName!!.asString() }
            .toList() shouldContainExactlyInAnyOrder listOf("com.example.bar")
    }

    @Test
    fun `symbol with null qualifiedName is excluded when include patterns are present`() {
        val classDecl =
            mockk<KSClassDeclaration> {
                every { qualifiedName } returns null
                every { packageName } returns mockk<KSName> { every { asString() } returns "com.example" }
            }
        SymbolFilter(null, listOf("com.example.*"), emptyList(), logger)
            .filter<KSClassDeclaration>(sequenceOf(classDecl))
            .toList()
            .shouldBeEmpty()
    }

    @Test
    fun `symbol with null qualifiedName passes through when no include patterns are set`() {
        val classDecl =
            mockk<KSClassDeclaration> {
                every { qualifiedName } returns null
                every { packageName } returns mockk<KSName> { every { asString() } returns "com.example" }
            }
        SymbolFilter(null, emptyList(), emptyList(), logger)
            .filter<KSClassDeclaration>(sequenceOf(classDecl))
            .toList()
            .size shouldBe 1
    }

    //endregion
}
