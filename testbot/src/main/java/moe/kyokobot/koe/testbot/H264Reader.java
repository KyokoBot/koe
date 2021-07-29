package moe.kyokobot.koe.testbot;

import io.netty.buffer.ByteBuf;

import java.io.BufferedInputStream;
import java.io.IOException;

public class H264Reader {
    private final BufferedInputStream stream;

    public H264Reader(BufferedInputStream stream) {
        this.stream = stream;
    }

    public int writeNextNALUnitData(ByteBuf target) throws IOException {
        byte[] pkt = new byte[4];
        int r = stream.read(pkt);
        byte nalUnit;

        if (r == -1) {
            return -1;
        } else {
            if (r >= 4 && pkt[0] == 0 && pkt[1] == 0 && pkt[2] == 0 && pkt[3] == 1) {
                r = stream.read();
                if (r == -1) return -1;

                nalUnit = (byte) r;
            } else if (pkt[0] == 0 && pkt[1] == 0 && pkt[2] == 1) {
                if (r == 4) {
                    nalUnit = pkt[3];
                } else {
                    return -1;
                }
            } else {
                return -1;
            }
        }

//        boolean forbiddenZeroBit = (nalUnit & 0x80) != 0;
//        byte nalRefIdc = (byte) ((nalUnit >> 5) & 0x03);
        int nalUnitType = ((nalUnit) & 0x1f);
        target.writeByte(nalUnit);

        for (; ; ) {
            stream.mark(4);
            r = stream.read();
            if (r == -1) break;

            if (r == 0) {
                int r2 = stream.read();
                int r3 = stream.read();
                int r4 = stream.read();

                if (r2 == 0 && (r3 == 1 || (r3 == 0 && r4 == 1))) {
                    stream.reset();
                    break;
                } else if (r2 == -1) {
                    target.writeByte(r);
                    break;
                } else if (r3 == -1) {
                    target.writeByte(r);
                    target.writeByte(r2);
                    break;
                } else if (r4 == -1) {
                    target.writeByte(r);
                    target.writeByte(r2);
                    target.writeByte(r3);
                    break;
                } else {
                    stream.reset();
                    target.writeByte(stream.read());
                }
            } else {
                target.writeByte(r);
            }
        }

        return nalUnitType;
    }

    public enum NALUnitType {
        Unspecified, // 0
        SliceLayerNonIDR,
        SliceDataPartALayer,
        SliceDataPartBLayer,
        SliceDataPartCLayer,
        SliceLayerIDR, // 5
        SEI,
        SeqParamSet,
        PicParamSet,
        AccessUnitDelimiter,
        EndSeq, // 10
        EndStream,
        FillerData,
        SeqParamSetExtension,
        PrefixNALUnit,
        SubsetSeqParamSet, // 15
        Reserved1,
        Reserved2,
        Reserved3,
        SliceLayerAux,
        SliceLayerExt, // 20
        SliceLayerExtDepth,
    }
}
