package moe.kyokobot.koe.internal.crypto;

import io.netty.buffer.ByteBuf;

public class XSalsa20Poly1305EncryptionMode implements EncryptionMode {
    private final XSalsa20Poly1305 cipher = new XSalsa20Poly1305();
    private final byte[] extendedNonce = new byte[24];
    private final byte[] tag = new byte[TAG_BYTES_LENGTH];
    private byte[] payload = new byte[1276];

    @Override
    @SuppressWarnings("Duplicates")
    public boolean box(ByteBuf plain, int len, ByteBuf output, byte[] secretKey) {
        if (payload.length < len) {
            // Only bigger video frames get here, the buffer is reused afterwards.
            payload = new byte[len];
        }

        // The nonce is the RTP header, the rest of it stays zero.
        output.getBytes(0, extendedNonce, 0, 12);
        plain.getBytes(plain.readerIndex(), payload, 0, len);

        cipher.seal(secretKey, extendedNonce, payload, len, tag);

        plain.skipBytes(len);
        output.writeBytes(tag);
        output.writeBytes(payload, 0, len);
        return true;
    }

    @Override
    public String getName() {
        return "xsalsa20_poly1305";
    }
}
