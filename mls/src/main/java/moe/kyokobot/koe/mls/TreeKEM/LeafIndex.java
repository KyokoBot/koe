package moe.kyokobot.koe.mls.TreeKEM;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.Vector;

import moe.kyokobot.koe.mls.TreeSize;
import moe.kyokobot.koe.mls.codec.MLSInputStream;
import moe.kyokobot.koe.mls.codec.MLSOutputStream;

public class LeafIndex
    implements MLSInputStream.Readable, MLSOutputStream.Writable
{
    protected int value;
    public int value()
    {
        return value;
    }

    public LeafIndex(int valueIn)
    {
        value = valueIn;
    }

    public LeafIndex(NodeIndex valueIn)
    {
        value = (int)(valueIn.value() >>> 1);
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o)
        {
            return true;
        }

        if (o == null || getClass() != o.getClass())
        {
            return false;
        }

        LeafIndex leafIndex = (LeafIndex)o;
        return value == leafIndex.value;
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(value);
    }

    public NodeIndex commonAncestor(LeafIndex other)
    {
        if (this.equals(other))
        {
            return new NodeIndex(this);
        }

        long k = 0;
        long xv = (new NodeIndex(this)).value();
        long yv = (new NodeIndex(other)).value();
        while (xv != yv)
        {
            xv >>= 1;
            yv >>= 1;
            k += 1;
        }

        long prefix = xv << k;
        long stop = (1L << (k - 1));
        return new NodeIndex(prefix + stop - 1);
    }

    public List<NodeIndex> directPath(TreeSize size)
    {
        List<NodeIndex> d = new Vector<NodeIndex>();

        NodeIndex n = new NodeIndex(this);
        NodeIndex r = NodeIndex.root(size);
        if (n.equals(r))
        {
            return d;
        }

        NodeIndex p = n.parent();
        while (!p.equals(r))
        {
            d.add(p);
            p = p.parent();
        }

        // Include the root unless this is a one-member tree
        if (!n.equals(r))
        {
            d.add(p);
        }

        return d;
    }

    @SuppressWarnings("unused")
    public LeafIndex(MLSInputStream stream)
        throws IOException
    {
        value = (int)stream.read(int.class);
    }

    @Override
    public void writeTo(MLSOutputStream stream)
        throws IOException
    {
        stream.write(value);
    }
}
