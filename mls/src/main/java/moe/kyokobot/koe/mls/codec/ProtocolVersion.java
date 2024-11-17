package moe.kyokobot.koe.mls.codec;

import java.io.IOException;

public enum ProtocolVersion
    implements MLSInputStream.Readable, MLSOutputStream.Writable
{
    RESERVED((short)0),
    mls10((short)1);
    final short value;

    ProtocolVersion(short value)
    {
        this.value = value;
    }

    public short getValue() {
        return value;
    }

    @Override
    public void writeTo(MLSOutputStream stream)
        throws IOException
    {
        stream.write(value);
    }
}
