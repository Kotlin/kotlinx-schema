package kotlinx.schema.generator.json.serialization

import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.nulls.shouldBeNull
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
import kotlinx.serialization.Serializable
import kotlinx.serialization.serializer
import kotlin.test.Test
import kotlinx.schema.generator.json.serialization.SerializationClassSchemaIntrospector as SerializationIntrospector

class SerializationIntrospectorTest {
    @Serializable
    @Description("A user model")
    data class User(
        @property:Description("The name of the user")
        val name: String,
        val age: Int?,
        val email: String = "n/a",
        val tags: List<String>,
        val attributes: Map<String, Int>?,
    )

    @Serializable
    @Description("Available colors")
    @Suppress("unused")
    enum class Color { RED, GREEN, BLUE }

    @Serializable
    data class WithEnum(
        val color: Color,
    )

    @Serializable
    sealed class Shape {
        @Serializable
        @Description("Circle shape")
        data class Circle(
            val radius: Double,
        ) : Shape()

        @Serializable
        @Description("Rectangle shape")
        data class Rectangle(
            val width: Double,
            val height: Double,
        ) : Shape()
    }

    private val introspector = SerializationIntrospector()

    @Test
    @Suppress("LongMethod")
    fun `introspects object with primitives list map nullability and defaults`() {
        val descriptor = serializer<User>().descriptor
        val graph = introspector.introspect(descriptor)

        // Root must be a ref to the User id (serial name)
        val rootRef = graph.root.shouldBeInstanceOf<TypeRef.Ref>()
        rootRef.id.value shouldBe User.serializer().descriptor.serialName
        rootRef.nullable shouldBe false

        val userNode = graph.nodes[rootRef.id].shouldBeInstanceOf<ObjectNode>()

        // Check that description is null due to kotlix.serialization limitation
        userNode.description.shouldBeNull()

        // Required should include all without defaults: name, age, tags, attributes (email has default)
        userNode.required.shouldContainExactlyInAnyOrder(setOf("name", "age", "tags", "attributes"))

        // Check property description and types
        val props = userNode.properties.associateBy { it.name }
        props.getValue("name") shouldNotBeNull {
            description shouldBe null
            type.shouldBeInstanceOf<TypeRef.Inline> { inline ->
                inline.node.shouldBeInstanceOf<PrimitiveNode> { prim ->
                    prim.kind shouldBe PrimitiveKind.STRING
                }
                inline.nullable shouldBe false
            }
        }

        props.getValue("age") shouldNotBeNull {
            type.shouldBeInstanceOf<TypeRef.Inline> { inline ->
                inline.node.shouldBeInstanceOf<PrimitiveNode> { prim ->
                    // Int maps to INT
                    prim.kind shouldBe PrimitiveKind.INT
                }
                inline.nullable shouldBe true
            }
        }

        // email has default, still type is primitive string
        props.getValue("email") shouldNotBeNull {
            type.shouldBeInstanceOf<TypeRef.Inline> { inline ->
                inline.node.shouldBeInstanceOf<PrimitiveNode> { prim ->
                    prim.kind shouldBe PrimitiveKind.STRING
                }
            }
        }

        // tags is List<String>
        props.getValue("tags") shouldNotBeNull {
            type.shouldBeInstanceOf<TypeRef.Inline> { inline ->
                inline.node.shouldBeInstanceOf<ListNode> { list ->
                    list.element.shouldBeInstanceOf<TypeRef.Inline> { el ->
                        el.node.shouldBeInstanceOf<PrimitiveNode> { prim ->
                            prim.kind shouldBe PrimitiveKind.STRING
                        }
                    }
                }
            }
        }

        // attributes is Map<String, Int>?
        props.getValue("attributes") shouldNotBeNull {
            type.shouldBeInstanceOf<TypeRef.Inline> { inline ->
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
    }

    @Test
    fun `introspects enum and adds node with entries and description`() {
        val graph = introspector.introspect(WithEnum.serializer().descriptor)

        val rootRef = graph.root.shouldBeInstanceOf<TypeRef.Ref>()
        val withEnumNode = graph.nodes[rootRef.id].shouldNotBeNull().shouldBeInstanceOf<ObjectNode>()
        val colorProp = withEnumNode.properties.first { it.name == "color" }

        val colorRef = colorProp.type.shouldBeInstanceOf<TypeRef.Ref>()
        val enumNode = graph.nodes[colorRef.id].shouldNotBeNull().shouldBeInstanceOf<EnumNode>()
        enumNode.entries.shouldContainExactlyInAnyOrder(listOf("RED", "GREEN", "BLUE"))
    }

    @Test
    fun `introspects sealed polymorphic adds polymorphic node and subtype objects`() {
        val graph = introspector.introspect(Shape.serializer().descriptor)

        val rootRef = graph.root.shouldBeInstanceOf<TypeRef.Ref>()
        val polyNode = graph.nodes[rootRef.id].shouldNotBeNull().shouldBeInstanceOf<PolymorphicNode>()

        // Discriminator should be required due to ALL_JSON_OBJECTS
        polyNode.discriminator shouldNotBeNull {
            name shouldBe "type"
            required shouldBe true
        }

        // Ensure subtypes discovered and object nodes present
        val subtypeIds = polyNode.subtypes.map { it.id.value }.toSet()
        subtypeIds.shouldContainExactlyInAnyOrder(
            setOf(
                Shape.Circle
                    .serializer()
                    .descriptor.serialName,
                Shape.Rectangle
                    .serializer()
                    .descriptor.serialName,
            ),
        )

        subtypeIds.forEach { id ->
            graph.nodes[TypeId(id)].shouldNotBeNull().shouldBeInstanceOf<ObjectNode>()
        }
    }
}
