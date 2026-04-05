package kotlinx.schema.generator.json.serialization

import kotlinx.schema.generator.json.serialization.SerializationClassJsonSchemaGeneratorTest.NestedProperty
import kotlinx.schema.generator.json.serialization.SerializationClassJsonSchemaGeneratorTest.TestClosedPolymorphism
import kotlinx.schema.generator.json.serialization.SerializationClassJsonSchemaGeneratorTest.TestEnum
import kotlinx.schema.generator.json.serialization.SerializationClassJsonSchemaGeneratorTest.TestObject
import kotlinx.serialization.SerialInfo
import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

@SerialInfo
annotation class CustomDescription(
    val value: String,
)

@Serializable
@CustomDescription("A test class")
data class TestClass(
    @property:CustomDescription("A string property")
    val stringProperty: String,
    @property:CustomDescription("An int property")
    val intProperty: Int,
    @property:CustomDescription("A long property")
    val longProperty: Long,
    @property:CustomDescription("A double property")
    val doubleProperty: Double,
    @property:CustomDescription("A float property")
    val floatProperty: Float,
    @property:CustomDescription("A nullable boolean property")
    val booleanNullableProperty: Boolean?,
    @property:CustomDescription("A nullable string property")
    val nullableProperty: String? = null,
    @property:CustomDescription("A list of strings")
    val listProperty: List<String> = emptyList(),
    @property:CustomDescription("A map of integers")
    val mapProperty: Map<String, Int> = emptyMap(),
    @property:CustomDescription("A custom nested property")
    val nestedProperty: NestedProperty = NestedProperty("foo", 1),
    @property:CustomDescription("A custom nested nullable property")
    val nestedNullableProperty: NestedProperty? = null,
    @property:CustomDescription("A list of nested properties")
    val nestedListProperty: List<NestedProperty> = emptyList(),
    @property:CustomDescription("A custom nested nullable list property")
    val nestedNullableListProperty: List<NestedProperty>? = null,
    @property:CustomDescription("A map of nested properties")
    val nestedMapProperty: Map<String, NestedProperty> = emptyMap(),
    @property:CustomDescription("A custom polymorphic property")
    val polymorphicProperty: TestClosedPolymorphism = TestClosedPolymorphism.SubClass1("id1", "property1"),
    @property:CustomDescription("An enum property")
    val enumProperty: TestEnum = TestEnum.One,
    @property:CustomDescription("A test object property")
    val objectProperty: TestObject = TestObject,
    @property:CustomDescription("A custom inline value class")
    val inlineValueClass: InlineValueClass = InlineValueClass(10.0),
    @property:CustomDescription("A custom inline value class nullable")
    val inlineValueClassNullable: InlineValueClass? = null,
)

@JvmInline
@Serializable
value class InlineValueClass(
    val value: Double,
)

@CustomDescription("Distance in meters")
@JvmInline
@Serializable
value class DescribedInlineValueClass(
    val value: Double,
)
