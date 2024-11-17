package moe.kyokobot.koe.mls.TreeKEM;

import java.io.IOException;

import moe.kyokobot.koe.mls.codec.MLSInputStream;
import moe.kyokobot.koe.mls.codec.MLSOutputStream;
import moe.kyokobot.koe.mls.codec.NodeType;

public class OptionalNode
    implements MLSInputStream.Readable, MLSOutputStream.Writable
{
    public static OptionalNode blankNode()
    {
        return new OptionalNode();
    }

    private OptionalNode()
    {
        this.node = null;
    }

    Node node;

    public boolean isBlank()
    {
        return node == null;
    }

    public boolean isLeaf()
    {
        return !isBlank() && node.nodeType == NodeType.leaf;
    }

    public boolean isParent()
    {
        return !isBlank() && node.nodeType == NodeType.parent;
    }

    public LeafNode getLeafNode()
    {
        return node.leafNode;
    }

    public ParentNode getParentNode()
    {
        return node.parentNode;
    }

    @SuppressWarnings("unused")
    public OptionalNode(MLSInputStream stream)
        throws IOException
    {
        node = (Node)stream.readOptional(Node.class);
    }

    @Override
    public void writeTo(MLSOutputStream stream)
        throws IOException
    {
        stream.writeOptional(node);
    }
}
