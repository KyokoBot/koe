package moe.kyokobot.koe.internal.crypto;

import io.netty.buffer.ByteBuf;

public class AEADXChaCha20Poly1305RTPSizeEncryptionMode implements EncryptionMode {

    private static final int NONCE_BYTES_LENGTH = 24;

    private final XChaCha20Poly1305 cipher = new XChaCha20Poly1305();
    private final byte[] extendedNonce = new byte[NONCE_BYTES_LENGTH];
    private byte[] associatedData = new byte[16];
    private final byte[] tag = new byte[TAG_BYTES_LENGTH];
    private byte[] payload = new byte[1276];
    private int seq;

    public AEADXChaCha20Poly1305RTPSizeEncryptionMode() {
        this(Math.abs(SECURE_RANDOM.nextInt()) % 418 + 1);
    }

    AEADXChaCha20Poly1305RTPSizeEncryptionMode(int initialSeq) {
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

        // rtp header was already written to output (read without moving reader index)
        int headerLength = output.writerIndex();
        if (associatedData.length < headerLength) {
            associatedData = new byte[headerLength];
        }
        output.getBytes(0, associatedData, 0, headerLength);
        plain.getBytes(plain.readerIndex(), payload, 0, len);

        cipher.seal(secretKey, extendedNonce, associatedData, headerLength, payload, len, tag);

        plain.skipBytes(len);
        this.seq++;
        output.writeBytes(payload, 0, len);
        output.writeBytes(tag);
        output.writeIntLE(s);
        return true;
    }

    @Override
    public boolean isRtpSize() {
        return true;
    }

    @Override
    public String getName() {
        return "aead_xchacha20_poly1305_rtpsize";
    }
}
