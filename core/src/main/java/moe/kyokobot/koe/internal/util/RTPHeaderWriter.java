package moe.kyokobot.koe.internal.util;

import io.netty.buffer.ByteBuf;

public class RTPHeaderWriter {
    private RTPHeaderWriter() {
        //
    }

    public static void writeV2(ByteBuf output, byte payloadType, char seq, int timestamp, int ssrc, boolean extension, boolean padding) {
        byte h = (byte) 0x80; // v2
        if (extension) h |= 0x10;
        if (padding) h |= 0x20;
        output.writeByte(h);
        output.writeByte(payloadType);
        output.writeChar(seq);
        output.writeInt(timestamp);
        output.writeInt(ssrc);
    }
}
