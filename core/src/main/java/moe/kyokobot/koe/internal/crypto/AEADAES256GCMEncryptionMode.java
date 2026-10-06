package moe.kyokobot.koe.internal.crypto;

import io.netty.buffer.ByteBuf;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.GeneralSecurityException;

public class AEADAES256GCMEncryptionMode implements EncryptionMode {

    static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int NONCE_BYTES_LENGTH = 12;

    private final byte[] extendedNonce = new byte[NONCE_BYTES_LENGTH];
    private byte[] associatedData = new byte[16];
    private final Cipher cipher;
    private byte[] lastKey;
    private SecretKeySpec keySpec;
    private int seq;

    public AEADAES256GCMEncryptionMode() {
        this(Math.abs(SECURE_RANDOM.nextInt()) % 418 + 1);
    }

    AEADAES256GCMEncryptionMode(int initialSeq) {
        this.seq = initialSeq;
        try {
            this.cipher = Cipher.getInstance(TRANSFORMATION);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(TRANSFORMATION + " is not available", e);
        }
    }

    @Override
    @SuppressWarnings("Duplicates")
    public boolean box(ByteBuf plain, int len, ByteBuf output, byte[] secretKey) {
        var s = this.seq;
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

        int written;
        try {
            // The key stays the same for the whole session.
            if (secretKey != lastKey) {
                keySpec = new SecretKeySpec(secretKey, "AES");
                lastKey = secretKey;
            }

            cipher.init(Cipher.ENCRYPT_MODE, keySpec, new GCMParameterSpec(TAG_BYTES_LENGTH * 8, extendedNonce));
            cipher.updateAAD(associatedData, 0, headerLength);
            output.ensureWritable(len + TAG_BYTES_LENGTH + 4);
            written = cipher.doFinal(plain.nioBuffer(plain.readerIndex(), len),
                    output.nioBuffer(output.writerIndex(), len + TAG_BYTES_LENGTH));
        } catch (GeneralSecurityException e) {
            return false;
        }

        plain.skipBytes(len);
        this.seq++;
        output.writerIndex(output.writerIndex() + written);
        output.writeIntLE(s);
        return true;
    }

    @Override
    public boolean isRtpSize() {
        return false;
    }

    @Override
    public String getName() {
        return "aead_aes256_gcm";
    }
}
