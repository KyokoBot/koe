package moe.kyokobot.koe.internal.crypto;

import io.netty.buffer.ByteBuf;

public class XSalsa20Poly1305LiteEncryptionMode implements EncryptionMode {
    private final XSalsa20Poly1305 cipher = new XSalsa20Poly1305();
    private final byte[] extendedNonce = new byte[24];
    private final byte[] tag = new byte[TAG_BYTES_LENGTH];
    private byte[] payload = new byte[1276];
    private int seq;

    public XSalsa20Poly1305LiteEncryptionMode() {
        this(0x80000000);
    }

    XSalsa20Poly1305LiteEncryptionMode(int initialSeq) {
        this.seq = initialSeq;
    }

    @Override
    @SuppressWarnings("Duplicates")
    public boolean box(ByteBuf plain, int len, ByteBuf output, byte[] secretKey) {
        if (payload.length < len) {
            // Only bigger video frames get here, the buffer is reused afterwards.
            payload = new byte[len];
        }

        int s = this.seq;
        extendedNonce[0] = (byte) (s & 0xff);
        extendedNonce[1] = (byte) ((s >> 8) & 0xff);
        extendedNonce[2] = (byte) ((s >> 16) & 0xff);
        extendedNonce[3] = (byte) ((s >> 24) & 0xff);

        plain.getBytes(plain.readerIndex(), payload, 0, len);

        cipher.seal(secretKey, extendedNonce, payload, len, tag);

        plain.skipBytes(len);
        this.seq++;
        output.writeBytes(tag);
        output.writeBytes(payload, 0, len);
        output.writeIntLE(s);
        return true;
    }

    @Override
    public String getName() {
        return "xsalsa20_poly1305_lite";
    }
}
