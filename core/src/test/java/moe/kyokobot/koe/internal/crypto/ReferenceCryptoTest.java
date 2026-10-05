package moe.kyokobot.koe.internal.crypto;

import org.junit.jupiter.api.Test;

import static moe.kyokobot.koe.TestUtils.hex;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;

class ReferenceCryptoTest {
    @Test
    void hChaCha20MatchesDraftTestVector() {
        // draft-irtf-cfrg-xchacha-03, section 2.2.1
        var key = new byte[32];
        for (int i = 0; i < key.length; i++) key[i] = (byte) i;
        var nonce = hex("000000090000004a0000000031415927");

        assertArrayEquals(hex("82413b4227b27bfed30e42508a877d73a0f9e4d58a74a853c12ec41326d3ecdc"),
                ReferenceCrypto.hChaCha20(key, nonce));
    }
}
