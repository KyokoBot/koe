package moe.kyokobot.koe.mls.TreeKEM;

import java.io.IOException;

import moe.kyokobot.koe.mls.codec.MLSInputStream;
import moe.kyokobot.koe.mls.codec.MLSOutputStream;

public class LeafNodeHashInput
    implements MLSInputStream.Readable, MLSOutputStream.Writable
{
    LeafIndex leafIndex;
    LeafNode leafNode;

    public LeafNodeHashInput(LeafIndex leafIndex, LeafNode leafNode)
    {
        this.leafIndex = leafIndex;
        this.leafNode = leafNode;
    }

    @SuppressWarnings("unused")
    public LeafNodeHashInput(MLSInputStream stream)
        throws IOException
    {
        leafIndex = (LeafIndex)stream.read(LeafIndex.class);
        leafNode = (LeafNode)stream.readOptional(LeafIndex.class);
    }

    @Override
    public void writeTo(MLSOutputStream stream)
        throws IOException
    {
        stream.write(leafIndex);
        stream.writeOptional(leafNode);
    }
}
