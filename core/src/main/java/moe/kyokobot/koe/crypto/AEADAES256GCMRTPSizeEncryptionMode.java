package moe.kyokobot.koe.crypto;

import com.google.crypto.tink.aead.internal.InsecureNonceAesGcmJce;
import io.netty.buffer.ByteBuf;

import java.security.GeneralSecurityException;

public class AEADAES256GCMRTPSizeEncryptionMode implements EncryptionMode {

    private static final int NONCE_BYTES_LENGTH = 12;

    private final byte[] extendedNonce = new byte[NONCE_BYTES_LENGTH];
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

        byte[] c;
        try {
            var aesGcmJce = new InsecureNonceAesGcmJce(secretKey);

            c = aesGcmJce.encrypt(extendedNonce, m, associatedData);
        } catch (GeneralSecurityException e) {
            return false;
        }

        output.writeBytes(c);
        output.writeIntLE(s);
        return true;
    }

    @Override
    public String getName() {
        return "aead_aes256_gcm_rtpsize";
    }
}
