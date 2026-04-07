package kotlinx.schema.integration.type

import kotlinx.schema.Description
import kotlinx.schema.Schema

@Description("A node in a tree structure")
@Schema(withSchemaObject = true)
sealed class TreeNode {
    @Description("Node identifier")
    abstract val id: String

    @Description("A leaf node containing a value")
    @Schema
    data class Leaf(
        @Description("Node identifier")
        override val id: String,
        @Description("Leaf value")
        val value: String,
    ) : TreeNode()

    @Description("A branch node with recursive children")
    @Schema
    data class Branch(
        @Description("Node identifier")
        override val id: String,
        @Description("Optional left child")
        val left: TreeNode?,
        @Description("Right child")
        val right: TreeNode,
    ) : TreeNode()
}

@Description("A tree with a root node")
@Schema(withSchemaObject = true)
data class Tree(
    @Description("The root node")
    val root: TreeNode,
)
