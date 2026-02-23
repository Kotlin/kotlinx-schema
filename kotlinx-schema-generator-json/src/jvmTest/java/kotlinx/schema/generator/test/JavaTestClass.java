package kotlinx.schema.generator.test;

import kotlinx.schema.Description;

import java.util.List;
import java.util.Map;

/**
 * Test class to verify that schema generation also works for Java classes based on their primary constructors
 */
@SuppressWarnings("unused")
public class JavaTestClass {
    public JavaTestClass(
        @Description("A string property")
        String stringProperty,
        int intProperty,
        long longProperty,
        double doubleProperty,
        float floatProperty,
        Boolean booleanNullableProperty,
        String nullableProperty,
        List<String> listProperty,
        Map<String, Integer> mapProperty,
        NestedProperty nestedProperty,
        List<NestedProperty> nestedListProperty,
        Map<String, NestedProperty> nestedMapProperty,
        TestEnum enumProperty
    ) {}

    @Description("Nested property class")
    public static class NestedProperty {
        public NestedProperty(
            @Description("Nested foo property")
            String foo,
            int bar
        ) {}
    }

    public enum TestEnum {
        One,
        Two
    }

    /**
     * Java reflection class representing this class.
     * Used for schema generation.
     */
    public static final Class<JavaTestClass> CLASS = JavaTestClass.class;
}
