package kotlinx.schema.generator.ir

import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.schema.Description
import kotlinx.schema.generator.json.SerializationIntrospector
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.ClassDiscriminatorMode
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import kotlin.test.Test

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

    private val json =
        Json {
            encodeDefaults = true
            classDiscriminator = "type"
            classDiscriminatorMode = ClassDiscriminatorMode.ALL_JSON_OBJECTS
        }

    @Test
    fun `introspects object with primitives list map nullability and defaults`() {
        val introspector = SerializationIntrospector(json)
        val kser = serializer<User>()
        val graph = introspector.introspect(kser)

        // Root must be a ref to the User id (serial name)
        val rootRef = graph.root.shouldBeInstanceOf<TypeRef.Ref>()
        rootRef.id.value shouldBe User.serializer().descriptor.serialName
        rootRef.nullable shouldBe false

        val userNode = graph.nodes[rootRef.id].shouldBeInstanceOf<ObjectNode>()

        // Required should include all without defaults: name, age, tags, attributes (email has default)
        userNode.required.shouldContainExactlyInAnyOrder(setOf("name", "age", "tags", "attributes"))

        // Properties: check types
        val props = userNode.properties.associateBy { it.name }
        props.getValue("name").type.shouldBeInstanceOf<TypeRef.Inline> { inline ->
            inline.node.shouldBeInstanceOf<PrimitiveNode> { prim ->
                prim.kind shouldBe PrimitiveKind.STRING
            }
            inline.nullable shouldBe false
        }

        props.getValue("age").type.shouldBeInstanceOf<TypeRef.Inline> { inline ->
            inline.node.shouldBeInstanceOf<PrimitiveNode> { prim ->
                // Int maps to INT
                prim.kind shouldBe PrimitiveKind.INT
            }
            inline.nullable shouldBe true
        }

        // email has default, still type is primitive string
        props.getValue("email").type.shouldBeInstanceOf<TypeRef.Inline> { inline ->
            inline.node.shouldBeInstanceOf<PrimitiveNode> { prim ->
                prim.kind shouldBe PrimitiveKind.STRING
            }
        }

        // tags is List<String>
        props.getValue("tags").type.shouldBeInstanceOf<TypeRef.Inline> { inline ->
            inline.node.shouldBeInstanceOf<ListNode> { list ->
                list.element.shouldBeInstanceOf<TypeRef.Inline> { el ->
                    el.node.shouldBeInstanceOf<PrimitiveNode> { prim ->
                        prim.kind shouldBe PrimitiveKind.STRING
                    }
                }
            }
        }

        // attributes is Map<String, Int>?
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
        val introspector = SerializationIntrospector(json)
        val graph = introspector.introspect(WithEnum.serializer())

        val rootRef = graph.root.shouldBeInstanceOf<TypeRef.Ref>()
        val withEnumNode = graph.nodes[rootRef.id].shouldNotBeNull().shouldBeInstanceOf<ObjectNode>()
        val colorProp = withEnumNode.properties.first { it.name == "color" }

        val colorRef = colorProp.type.shouldBeInstanceOf<TypeRef.Ref>()
        val enumNode = graph.nodes[colorRef.id].shouldNotBeNull().shouldBeInstanceOf<EnumNode>()
        enumNode.entries.shouldContainExactlyInAnyOrder(listOf("RED", "GREEN", "BLUE"))
    }

    @Test
    fun `introspects sealed polymorphic adds polymorphic node and subtype objects`() {
        val introspector = SerializationIntrospector(json)
        val graph = introspector.introspect(Shape.serializer())

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