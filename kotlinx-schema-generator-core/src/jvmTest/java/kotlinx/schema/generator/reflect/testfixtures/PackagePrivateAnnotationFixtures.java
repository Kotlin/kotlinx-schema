package kotlinx.schema.generator.reflect.testfixtures;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Test fixtures for the package-private annotation reflection fallback in {@code extractDescription}.
 *
 * <p>The nested {@code LLMDescription} annotation has no {@code public} modifier — it is
 * package-private. This simulates third-party Kotlin {@code internal} annotations (which compile
 * to package-private interfaces) that are accessed from outside their package. From a different
 * package, {@code KProperty1.get()} may throw {@link IllegalAccessException}; the fallback in
 * {@code extractDescription} recovers by calling {@code getDeclaredMethod()} on the proxy class
 * with {@code isAccessible = true}.
 */
public final class PackagePrivateAnnotationFixtures {

    /**
     * Package-private annotation with two elements: {@code value} (shorthand) and
     * {@code description} (verbose). Matched by simple name "LLMDescription" via
     * {@code Introspections}.
     */
    @Target({ElementType.METHOD, ElementType.TYPE})
    @Retention(RetentionPolicy.RUNTIME)
    @interface LLMDescription {
        String value() default "";

        String description() default "";
    }

    /** Holder with a method annotated with the package-private {@code @LLMDescription}. */
    public static final class AnnotatedHolder {
        @LLMDescription(description = "product search tool")
        public static void searchProducts() {}

        @LLMDescription("find user by id")
        public static void findUser() {}
    }

    private PackagePrivateAnnotationFixtures() {}
}
