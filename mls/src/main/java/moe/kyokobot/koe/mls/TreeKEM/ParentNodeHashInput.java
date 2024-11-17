package moe.kyokobot.koe.mls.TreeKEM;

import java.io.IOException;

import moe.kyokobot.koe.mls.codec.MLSInputStream;
import moe.kyokobot.koe.mls.codec.MLSOutputStream;

public class ParentNodeHashInput
    implements MLSInputStream.Readable, MLSOutputStream.Writable
{
    ParentNode parentNode;
    byte[] leftHash;
    byte[] rightHash;

    public ParentNodeHashInput(ParentNode parentNode, byte[] leftHash, byte[] rightHash)
    {
        this.parentNode = parentNode;
        this.leftHash = leftHash;
        this.rightHash = rightHash;
    }

    @SuppressWarnings("unused")
    public ParentNodeHashInput(MLSInputStream stream)
        throws IOException
    {
        parentNode = (ParentNode)stream.readOptional(ParentNode.class);
        leftHash = stream.readOpaque();
        rightHash = stream.readOpaque();
    }

    @Override
    public void writeTo(MLSOutputStream stream)
        throws IOException
    {
        stream.writeOptional(parentNode);
        stream.writeOpaque(leftHash);
        stream.writeOpaque(rightHash);
    }
}


