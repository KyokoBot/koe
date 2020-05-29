package moe.kyokobot.koe.internal.util;

import io.netty.buffer.ByteBuf;

public class RTPHeaderWriter {
    private RTPHeaderWriter() {
        //
    }

    public static void writeV2(ByteBuf output, byte payloadType, char seq, int timestamp, int ssrc, boolean extension) {
        output.writeByte(extension ? 0x90 : 0x80);
        output.writeByte(payloadType & 0x7f);
        output.writeChar(seq);
        output.writeInt(timestamp);
        output.writeInt(ssrc);
    }
}
