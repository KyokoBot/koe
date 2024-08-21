package moe.kyokobot.koe.crypto;

import com.google.crypto.tink.aead.internal.InsecureNonceXChaCha20Poly1305;
import io.netty.buffer.ByteBuf;

import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;

public class AEADXChaCha20Poly1305RTPSizeEncryptionMode implements EncryptionMode {

    private static final int NONCE_BYTES_LENGTH = 24;

    private final byte[] extendedNonce = new byte[NONCE_BYTES_LENGTH];
    private final ByteBuffer c = ByteBuffer.allocate(1276 + TAG_BYTES_LENGTH + NONCE_BYTES_LENGTH);
    private final byte[] associatedData = new byte[12];
    private int seq = Math.abs(random.nextInt()) % 418 + 1;

    @Override
    @SuppressWarnings("Duplicates")
    public boolean box(ByteBuf packet, int len, ByteBuf output, byte[] secretKey) {
        var m = new byte[len];
        packet.readBytes(m, 0, len);

        var s = this.seq++;
        extendedNonce[0] = (byte) (s & 0xff);
        extendedNonce[1] = (byte) ((s >> 8) & 0xff);
        extendedNonce[2] = (byte) ((s >> 16) & 0xff);
        extendedNonce[3] = (byte) ((s >> 24) & 0xff);

        // rtp header was already written to output
        output.readBytes(associatedData);
        output.resetReaderIndex();

        c.clear();
        c.limit(len + TAG_BYTES_LENGTH);
        try {
            var xChaCha20Poly1305 = new InsecureNonceXChaCha20Poly1305(secretKey);

            xChaCha20Poly1305.encrypt(c, extendedNonce, m, associatedData);
        } catch (GeneralSecurityException e) {
            return false;
        }

        output.writeBytes(c.flip());
        output.writeIntLE(s);
        return true;
    }

    @Override
    public String getName() {
        return "aead_xchacha20_poly1305_rtpsize";
    }
}
