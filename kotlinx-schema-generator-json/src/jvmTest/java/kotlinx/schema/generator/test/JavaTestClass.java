package kotlinx.schema.generator.test;

import kotlinx.schema.Description;

import java.util.List;
import java.util.Map;

/**
 * Test class to verify that schema generation also works for Java classes based on their primary constructors
 */
@SuppressWarnings("unused")
@Description("Class Description")
public class JavaTestClass {
    public JavaTestClass(
        @Description("A string property")
        String stringProperty,
        @Description("An int property")
        int intProperty,
        @Description("A long property")
        long longProperty,
        @Description("A double property")
        double doubleProperty,
        @Description("A float property")
        float floatProperty,
        @Description("A nullable boolean property")
        Boolean booleanNullableProperty,
        @Description("A nullable string property")
        String nullableProperty,
        @Description("A list of strings")
        List<String> listProperty,
        @Description("A map of integers")
        Map<String, Integer> mapProperty,
        @Description("A nested property")
        NestedProperty nestedProperty,
        @Description("A list of nested properties")
        List<NestedProperty> nestedListProperty,
        @Description("A map of nested properties")
        Map<String, NestedProperty> nestedMapProperty,
        @Description("An enum property")
        TestEnum enumProperty,
        @Description("Simple record property")
        ProblemDescription recordProperty
    ) {
    }

    @Description("Nested property class")
    public static class NestedProperty {
        public NestedProperty(
            @Description("Nested foo property")
            String foo,
            int bar
        ) {
        }
    }

    public enum TestEnum {
        One,
        Two
    }

    @Description("Record description")
    public record ProblemDescription(
        @Description("String property") String description) {
    }
}
