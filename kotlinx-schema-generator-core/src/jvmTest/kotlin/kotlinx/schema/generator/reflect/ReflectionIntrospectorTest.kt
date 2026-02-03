package kotlinx.schema.generator.reflect

import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.schema.Description
import kotlinx.schema.generator.core.ir.EnumNode
import kotlinx.schema.generator.core.ir.ListNode
import kotlinx.schema.generator.core.ir.MapNode
import kotlinx.schema.generator.core.ir.ObjectNode
import kotlinx.schema.generator.core.ir.PolymorphicNode
import kotlinx.schema.generator.core.ir.PrimitiveKind
import kotlinx.schema.generator.core.ir.PrimitiveNode
import kotlinx.schema.generator.core.ir.TypeId
import kotlinx.schema.generator.core.ir.TypeRef
import kotlin.test.Test

class ReflectionIntrospectorTest {
    @Description("A user model")
    data class User(
        @property:Description("The name of the user")
        val name: String,
        val age: Int?,
        val email: String = "n/a",
        val tags: List<String>,
        val attributes: Map<String, Int>?,
    )

    @Description("Available colors")
    @Suppress("unused")
    enum class Color { RED, GREEN, BLUE }

    data class WithEnum(
        val color: Color,
    )

    sealed class Shape {
        @Description("Circle shape")
        data class Circle(
            val radius: Double,
        ) : Shape()

        @Description("Rectangle shape")
        data class Rectangle(
            val width: Double,
            val height: Double,
        ) : Shape()
    }

    private val introspector = ReflectionClassIntrospector

    @Test
    @Suppress("LongMethod")
    fun `introspects object with primitives list map nullability and defaults`() {
        val graph = introspector.introspect(User::class)

        // Root must be a ref to the User id (serial name)
        val rootRef = graph.root.shouldBeInstanceOf<TypeRef.Ref>()
        rootRef.id.value shouldBe User::class.qualifiedName
        rootRef.nullable shouldBe false

        val userNode = graph.nodes[rootRef.id].shouldBeInstanceOf<ObjectNode>()

        // Verify class-level description
        userNode.description shouldBe "A user model"

        // Required should include all without defaults: name, age, tags, attributes (email has default)
        userNode.required.shouldContainExactlyInAnyOrder(setOf("name", "age", "tags", "attributes"))

        // Properties: check types
        val props = userNode.properties.associateBy { it.name }

        // Verify property with description and required status
        props.getValue("name").apply {
            description shouldBe "The name of the user"
            hasDefaultValue shouldBe false
            type.shouldBeInstanceOf<TypeRef.Inline> { inline ->
                inline.node.shouldBeInstanceOf<PrimitiveNode> { prim ->
                    prim.kind shouldBe PrimitiveKind.STRING
                }
                inline.nullable shouldBe false
            }
        }

        // age is nullable but still required (no default value)
        props.getValue("age").apply {
            hasDefaultValue shouldBe false
            type.shouldBeInstanceOf<TypeRef.Inline> { inline ->
                inline.node.shouldBeInstanceOf<PrimitiveNode> { prim ->
                    prim.kind shouldBe PrimitiveKind.INT
                }
                inline.nullable shouldBe true
            }
        }

        // email has default value, so hasDefaultValue should be true
        props.getValue("email").apply {
            hasDefaultValue shouldBe true
            type.shouldBeInstanceOf<TypeRef.Inline> { inline ->
                inline.node.shouldBeInstanceOf<PrimitiveNode> { prim ->
                    prim.kind shouldBe PrimitiveKind.STRING
                }
            }
        }

        // tags is required (no default)
        props.getValue("tags").apply {
            hasDefaultValue shouldBe false
        }

        // attributes is nullable but required (no default)
        props.getValue("attributes").apply {
            hasDefaultValue shouldBe false
        }

        // Verify collection types
        props.getValue("tags").type.shouldBeInstanceOf<TypeRef.Inline> { inline ->
            inline.node.shouldBeInstanceOf<ListNode> { list ->
                list.element.shouldBeInstanceOf<TypeRef.Inline> { el ->
                    el.node.shouldBeInstanceOf<PrimitiveNode> { prim ->
                        prim.kind shouldBe PrimitiveKind.STRING
                    }
                }
            }
        }

        props.getValue("attributes").type.shouldBeInstanceOf<TypeRef.Inline> { inline ->
            inline.nullable shouldBe true
            inline.node.shouldBeInstanceOf<MapNode> { map ->
                map.key.shouldBeInstanceOf<TypeRef.Inline> { k ->
                    k.node.shouldBeInstanceOf<PrimitiveNode> { prim ->
                        prim.kind shouldBe PrimitiveKind.STRING
                    }
                }
                map.value.shouldBeInstanceOf<TypeRef.Inline> { v ->
                    v.node.shouldBeInstanceOf<PrimitiveNode> { prim ->
                        prim.kind shouldBe PrimitiveKind.INT
                    }
                }
            }
        }
    }

    @Test
    fun `introspects enum and adds node with entries and description`() {
        val graph = introspector.introspect(WithEnum::class)

        val rootRef = graph.root.shouldBeInstanceOf<TypeRef.Ref>()
        val withEnumNode = graph.nodes[rootRef.id].shouldBeInstanceOf<ObjectNode>()
        val colorProp = withEnumNode.properties.first { it.name == "color" }

        val colorRef = colorProp.type.shouldBeInstanceOf<TypeRef.Ref>()
        val enumNode = graph.nodes[colorRef.id].shouldBeInstanceOf<EnumNode>()

        // Verify enum entries and description
        enumNode.entries.shouldContainExactlyInAnyOrder(listOf("RED", "GREEN", "BLUE"))
        enumNode.description shouldBe "Available colors"
    }

    @Test
    fun `introspects sealed polymorphic adds polymorphic node and subtype objects`() {
        val graph = introspector.introspect(Shape::class)

        val rootRef = graph.root.shouldBeInstanceOf<TypeRef.Ref>()
        val polyNode = graph.nodes[rootRef.id].shouldNotBeNull().shouldBeInstanceOf<PolymorphicNode>()

        // Verify polymorphic node has no description (Shape class is not annotated)
        polyNode.description shouldBe null

        // Discriminator should be required
        polyNode.discriminator shouldNotBeNull {
            name shouldBe "type"
            required shouldBe true
        }

        // Verify subtypes use qualified names (Parent.Child pattern)
        val subtypeIds = polyNode.subtypes.map { it.id.value }.toSet()
        subtypeIds.shouldContainExactlyInAnyOrder(
            setOf(
                "Shape.Circle",
                "Shape.Rectangle",
            ),
        )

        // Verify each subtype node is registered with qualified name
        val circleNode = graph.nodes[TypeId("Shape.Circle")].shouldNotBeNull().shouldBeInstanceOf<ObjectNode>()
        circleNode.description shouldBe "Circle shape"

        val rectangleNode = graph.nodes[TypeId("Shape.Rectangle")].shouldNotBeNull().shouldBeInstanceOf<ObjectNode>()
        rectangleNode.description shouldBe "Rectangle shape"
    }
}
