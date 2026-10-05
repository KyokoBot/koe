package moe.kyokobot.koe.internal.crypto;

import io.netty.buffer.ByteBuf;

import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;

public class XSalsa20Poly1305SuffixEncryptionMode implements EncryptionMode {
    private final XSalsa20Poly1305 cipher = new XSalsa20Poly1305();
    private final byte[] extendedNonce = new byte[24];
    private final byte[] tag = new byte[TAG_BYTES_LENGTH];
    private final Consumer<byte[]> nonceGenerator;
    private byte[] payload = new byte[1276];

    public XSalsa20Poly1305SuffixEncryptionMode() {
        this(nonce -> ThreadLocalRandom.current().nextBytes(nonce));
    }

    XSalsa20Poly1305SuffixEncryptionMode(Consumer<byte[]> nonceGenerator) {
        this.nonceGenerator = nonceGenerator;
    }

    @Override
    @SuppressWarnings("Duplicates")
    public boolean box(ByteBuf plain, int len, ByteBuf output, byte[] secretKey) {
        if (payload.length < len) {
            // Only bigger video frames get here, the buffer is reused afterwards.
            payload = new byte[len];
        }

        nonceGenerator.accept(extendedNonce);
        plain.getBytes(plain.readerIndex(), payload, 0, len);

        cipher.seal(secretKey, extendedNonce, payload, len, tag);

        plain.skipBytes(len);
        output.writeBytes(tag);
        output.writeBytes(payload, 0, len);
        output.writeBytes(extendedNonce);
        return true;
    }

    @Override
    public String getName() {
        return "xsalsa20_poly1305_suffix";
    }
}
