package moe.kyokobot.koe.mls.TreeKEM;

import java.io.IOException;

import moe.kyokobot.koe.mls.codec.MLSInputStream;
import moe.kyokobot.koe.mls.codec.MLSOutputStream;

public enum LeafNodeSource
    implements MLSInputStream.Readable, MLSOutputStream.Writable
{
    RESERVED((byte)0),
    KEY_PACKAGE((byte)1),
    UPDATE((byte)2),
    COMMIT((byte)3);

    final byte value;

    LeafNodeSource(byte value)
    {
        this.value = value;
    }

    @Override
    public void writeTo(MLSOutputStream stream)
        throws IOException
    {
        stream.write(value);
    }
}
