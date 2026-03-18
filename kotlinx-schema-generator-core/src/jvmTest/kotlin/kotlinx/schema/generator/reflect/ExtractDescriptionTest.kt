package kotlinx.schema.generator.reflect

import io.kotest.matchers.shouldBe
import kotlinx.schema.generator.reflect.testfixtures.PackagePrivateAnnotationFixtures
import kotlin.test.Test

class ExtractDescriptionTest {
    //region Package-private annotation fallback
    /**
     * [PackagePrivateAnnotationFixtures.LLMDescription] is package-private in
     * `kotlinx.schema.generator.reflect.testfixtures`. When [extractDescription] (in
     * `kotlinx.schema.generator.reflect`) calls `KProperty1.get()` on its elements, the JVM
     * denies access across packages for a non-public interface and throws
     * [IllegalAccessException]. The fallback recovers via `getDeclaredMethod()` on the proxy
     * class with `isAccessible = true`.
     */
    @Test
    fun `extracts description from package-private annotation description= style via Java reflection fallback`() {
        val method =
            PackagePrivateAnnotationFixtures.AnnotatedHolder::class.java
                .getDeclaredMethod("searchProducts")

        extractDescription(method.annotations.toList()) shouldBe "product search tool"
    }

    @Test
    fun `extracts description from package-private annotation value= shorthand style via Java reflection fallback`() {
        val method =
            PackagePrivateAnnotationFixtures.AnnotatedHolder::class.java
                .getDeclaredMethod("findUser")

        extractDescription(method.annotations.toList()) shouldBe "find user by id"
    }
    //endregion
}
