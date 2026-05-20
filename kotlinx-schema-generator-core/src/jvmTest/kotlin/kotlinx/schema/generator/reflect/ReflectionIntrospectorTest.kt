@file:OptIn(ExperimentalUuidApi::class)

package kotlinx.schema.generator.reflect

import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.schema.Description
import kotlinx.schema.SchemaIgnore
import kotlinx.schema.generator.core.ir.AnyNode
import kotlinx.schema.generator.core.ir.EnumNode
import kotlinx.schema.generator.core.ir.ListNode
import kotlinx.schema.generator.core.ir.MapNode
import kotlinx.schema.generator.core.ir.ObjectNode
import kotlinx.schema.generator.core.ir.PolymorphicNode
import kotlinx.schema.generator.core.ir.PrimitiveKind
import kotlinx.schema.generator.core.ir.PrimitiveNode
import kotlinx.schema.generator.core.ir.TypeId
import kotlinx.schema.generator.core.ir.TypeRef
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind as SerialPrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlin.jvm.JvmInline
import kotlin.test.Test
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

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

    @Suppress("unused")
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

    @Suppress("unused")
    sealed class Vehicle {
        sealed class Motorized : Vehicle() {
            data class Car(val doors: Int) : Motorized()
            data class Truck(val payload: Double) : Motorized()
        }

        data class Bicycle(val gears: Int) : Vehicle()
    }

    @Suppress("unused")
    sealed class Event {
        data class Click(val x: Int, val y: Int) : Event()
        data class PageView(val url: String) : Event()

        @SchemaIgnore
        data class Internal(val trace: String) : Event()
    }

    data class WithAny(
        val content: Any,
        val optContent: Any?,
        val metadata: Map<String, Any>,
    )

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
        }

        // Verify subtypes use qualified names (Parent.Child pattern)
        val subtypeIds = polyNode.subtypes.map { it.id.value }.toSet()
        subtypeIds.shouldContainExactlyInAnyOrder(
            setOf(
                "kotlinx.schema.generator.reflect.ReflectionIntrospectorTest.Shape.Circle",
                "kotlinx.schema.generator.reflect.ReflectionIntrospectorTest.Shape.Rectangle",
            ),
        )

        // Verify each subtype node is registered with qualified name
        val circleNode =
            graph.nodes[
                TypeId(
                    "kotlinx.schema.generator.reflect.ReflectionIntrospectorTest.Shape.Circle",
                ),
            ].shouldNotBeNull()
                .shouldBeInstanceOf<ObjectNode>()
        circleNode.description shouldBe "Circle shape"

        val rectangleNode =
            graph.nodes[
                TypeId(
                    "kotlinx.schema.generator.reflect.ReflectionIntrospectorTest.Shape.Rectangle",
                ),
            ].shouldNotBeNull()
                .shouldBeInstanceOf<ObjectNode>()
        rectangleNode.description shouldBe "Rectangle shape"
    }

    @Test
    fun `sealed class excludes @SchemaIgnore subtypes from polymorphic node`() {
        val graph = introspector.introspect(Event::class)

        val rootRef = graph.root.shouldBeInstanceOf<TypeRef.Ref>()
        val polyNode = graph.nodes[rootRef.id].shouldNotBeNull().shouldBeInstanceOf<PolymorphicNode>()

        val subtypeIds = polyNode.subtypes.map { it.id.value }.toSet()
        subtypeIds.shouldContainExactlyInAnyOrder(
            setOf(
                "kotlinx.schema.generator.reflect.ReflectionIntrospectorTest.Event.Click",
                "kotlinx.schema.generator.reflect.ReflectionIntrospectorTest.Event.PageView",
            ),
        )

        // Internal should not appear in nodes
        graph.nodes.keys.none { it.value.contains("Internal") } shouldBe true
    }

    @Test
    fun `introspects kotlin Any as inline AnyNode for non-nullable nullable and map value`() {
        val graph = introspector.introspect(WithAny::class)

        val root = graph.root.shouldBeInstanceOf<TypeRef.Ref>()
        val node = graph.nodes[root.id].shouldBeInstanceOf<ObjectNode>()
        val props = node.properties.associateBy { it.name }

        // non-nullable Any → TypeRef.Inline(AnyNode(), nullable=false)
        props.getValue("content").type.shouldBeInstanceOf<TypeRef.Inline> {
            it.node.shouldBeInstanceOf<AnyNode>()
            it.nullable shouldBe false
        }

        // nullable Any? → TypeRef.Inline(AnyNode(), nullable=true)
        props.getValue("optContent").type.shouldBeInstanceOf<TypeRef.Inline> {
            it.node.shouldBeInstanceOf<AnyNode>()
            it.nullable shouldBe true
        }

        // Map<String, Any> value type → AnyNode
        props.getValue("metadata").type.shouldBeInstanceOf<TypeRef.Inline> { mapRef ->
            mapRef.node.shouldBeInstanceOf<MapNode> { mapNode ->
                mapNode.value.shouldBeInstanceOf<TypeRef.Inline> {
                    it.node.shouldBeInstanceOf<AnyNode>()
                }
            }
        }

        // kotlin.Any does not create a named node in the graph
        graph.nodes.keys.none { it.value == "kotlin.Any" } shouldBe true
    }

    // ---- Inline value classes & serializer-aware primitive resolution -----------------

    @JvmInline
    @Serializable
    value class Meters(val value: Double)

    data class WithInlineValueClass(
        val distance: Meters,
        val optionalDistance: Meters?,
    )

    @Test
    fun `introspects inline value class as its inner primitive type`() {
        val graph = introspector.introspect(WithInlineValueClass::class)

        val rootRef = graph.root.shouldBeInstanceOf<TypeRef.Ref>()
        val objNode = graph.nodes[rootRef.id].shouldNotBeNull().shouldBeInstanceOf<ObjectNode>()
        val props = objNode.properties.associateBy { it.name }

        // Non-nullable inline value class wrapping a Double should resolve to a flat
        // double primitive — no `{value: …}` wrapper, no `Meters` node in the graph.
        props.getValue("distance").type.shouldBeInstanceOf<TypeRef.Inline> { inline ->
            inline.node.shouldBeInstanceOf<PrimitiveNode> { prim ->
                prim.kind shouldBe PrimitiveKind.DOUBLE
            }
            inline.nullable shouldBe false
        }

        // Nullable variant should propagate the nullability through the flattening.
        props.getValue("optionalDistance").type.shouldBeInstanceOf<TypeRef.Inline> { inline ->
            inline.node.shouldBeInstanceOf<PrimitiveNode> { prim ->
                prim.kind shouldBe PrimitiveKind.DOUBLE
            }
            inline.nullable shouldBe true
        }

        // The inline class itself must NOT appear as a separate node in the graph.
        graph.nodes.keys.none { it.value.endsWith("Meters") } shouldBe true
    }

    /**
     * Custom string-shaped serializer for `kotlin.uuid.Uuid`. Together with the typealias
     * below, this mirrors the standard kotlinx-serialization pattern for serializing a
     * third-party class whose declaration you can't modify.
     *
     * (Real downstream code can also rely on the built-in `Uuid.Companion.serializer()`
     * shipped by kotlinx-serialization 1.10+, which is also primitive-string. We define
     * our own here so the test pins the *resolution mechanism* — `serializerOrNull` →
     * descriptor.kind → primitive — independently of which serializer happens to be
     * picked up.)
     */
    object TestUuidStringSerializer : KSerializer<Uuid> {
        override val descriptor: SerialDescriptor =
            PrimitiveSerialDescriptor(serialName = "TestUuid", kind = SerialPrimitiveKind.STRING)

        override fun serialize(
            encoder: Encoder,
            value: Uuid,
        ) = encoder.encodeString(value.toString())

        override fun deserialize(decoder: Decoder): Uuid = Uuid.parse(decoder.decodeString())
    }

    @JvmInline
    @Serializable
    value class TestUserId(
        val value: @Serializable(with = TestUuidStringSerializer::class) Uuid,
    )

    data class WithInlineValueClassWrappingUuid(
        val id: TestUserId,
        val optionalId: TestUserId?,
    )

    @Test
    fun `inline value class wrapping a typealias-string-serializer Uuid resolves to STRING`() {
        // Repro for the bug observed downstream when Koog generated a tool descriptor for
        // a method taking a `DocumentId` parameter (an `@JvmInline value class` wrapping a
        // typealias-annotated `Uuid`). Without the inline-class flattening + serializer-
        // aware primitive resolution, the reflection introspector would produce an
        // ObjectNode like `{value: {leastSignificantBits, mostSignificantBits}}` because
        // it would walk Uuid's class structure via reflection.
        val graph = introspector.introspect(WithInlineValueClassWrappingUuid::class)

        val rootRef = graph.root.shouldBeInstanceOf<TypeRef.Ref>()
        val objNode = graph.nodes[rootRef.id].shouldNotBeNull().shouldBeInstanceOf<ObjectNode>()
        val props = objNode.properties.associateBy { it.name }

        props.getValue("id").type.shouldBeInstanceOf<TypeRef.Inline> { inline ->
            inline.node.shouldBeInstanceOf<PrimitiveNode> { prim ->
                prim.kind shouldBe PrimitiveKind.STRING
            }
            inline.nullable shouldBe false
        }

        props.getValue("optionalId").type.shouldBeInstanceOf<TypeRef.Inline> { inline ->
            inline.node.shouldBeInstanceOf<PrimitiveNode> { prim ->
                prim.kind shouldBe PrimitiveKind.STRING
            }
            inline.nullable shouldBe true
        }

        // Neither the inline class nor the underlying Uuid should appear as named nodes.
        graph.nodes.keys.none { it.value.endsWith("TestUserId") } shouldBe true
        graph.nodes.keys.none { it.value == "kotlin.uuid.Uuid" } shouldBe true
    }

    data class WithBuiltinUuid(
        val id: Uuid,
        val optionalId: Uuid?,
    )

    @Test
    fun `bare kotlin uuid Uuid resolves to STRING via built-in serializer`() {
        // kotlinx-serialization 1.10+ ships a built-in Uuid serializer that's
        // PrimitiveKind.STRING. The reflection introspector should pick it up via
        // `serializerOrNull(typeOf<Uuid>())` without needing any user annotation.
        val graph = introspector.introspect(WithBuiltinUuid::class)

        val rootRef = graph.root.shouldBeInstanceOf<TypeRef.Ref>()
        val objNode = graph.nodes[rootRef.id].shouldNotBeNull().shouldBeInstanceOf<ObjectNode>()
        val props = objNode.properties.associateBy { it.name }

        props.getValue("id").type.shouldBeInstanceOf<TypeRef.Inline> { inline ->
            inline.node.shouldBeInstanceOf<PrimitiveNode> { prim -> prim.kind shouldBe PrimitiveKind.STRING }
            inline.nullable shouldBe false
        }
        props.getValue("optionalId").type.shouldBeInstanceOf<TypeRef.Inline> { inline ->
            inline.node.shouldBeInstanceOf<PrimitiveNode> { prim -> prim.kind shouldBe PrimitiveKind.STRING }
            inline.nullable shouldBe true
        }

        graph.nodes.keys.none { it.value == "kotlin.uuid.Uuid" } shouldBe true
    }
}
