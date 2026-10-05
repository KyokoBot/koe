package moe.kyokobot.koe.internal.crypto;

import io.netty.buffer.ByteBuf;

import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;

public class XSalsa20Poly1305SuffixEncryptionMode implements EncryptionMode {
    private final byte[] extendedNonce = new byte[24];
    private final byte[] m = new byte[1276 + ZERO_BYTES_LENGTH];
    private final byte[] c = new byte[1276 + ZERO_BYTES_LENGTH];
    private final TweetNaclFastInstanced nacl = new TweetNaclFastInstanced();
    private final Consumer<byte[]> nonceGenerator;

    public XSalsa20Poly1305SuffixEncryptionMode() {
        this(nonce -> ThreadLocalRandom.current().nextBytes(nonce));
    }

    XSalsa20Poly1305SuffixEncryptionMode(Consumer<byte[]> nonceGenerator) {
        this.nonceGenerator = nonceGenerator;
    }

    @Override
    @SuppressWarnings("Duplicates")
    public boolean box(ByteBuf plain, int len, ByteBuf output, byte[] secretKey) {
        for (int i = 0; i < c.length; i++) {
            m[i] = 0;
            c[i] = 0;
        }

        for (int i = 0; i < len; i++) {
            m[i + 32] = plain.readByte();
        }

        nonceGenerator.accept(extendedNonce);

        if (0 == nacl.cryptoSecretboxXSalsa20Poly1305(c, m, len + 32, extendedNonce, secretKey)) {
            for (int i = 0; i < (len + 16); i++) {
                output.writeByte(c[i + 16]);
            }
            output.writeBytes(extendedNonce);
            return true;
        } else {
            return false;
        }
    }

    @Override
    public String getName() {
        return "xsalsa20_poly1305_suffix";
    }
}
