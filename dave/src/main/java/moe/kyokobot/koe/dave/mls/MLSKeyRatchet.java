package moe.kyokobot.koe.dave.mls;

import moe.kyokobot.koe.dave.KeyRatchet;
import moe.kyokobot.koe.mls.crypto.MlsCipherSuite;

public class MLSKeyRatchet implements KeyRatchet {
    public MLSKeyRatchet(MlsCipherSuite suite, byte[] baseSecret) {

    }

    @Override
    public byte[] getKey(int keyGeneration) {
        return new byte[0];
    }

    @Override
    public void deleteKey(int keyGeneration) {

    }
}
