package moe.kyokobot.koe.mls;

import java.util.Arrays;

import moe.kyokobot.koe.mls.crypto.Secret;

public class KeyGeneration
{
    public final int generation;
    public final byte[] key;
    public final byte[] nonce;

    public KeyGeneration(int generation, Secret key, Secret nonce)
    {
        this.generation = generation;
        this.key = key.value().clone();
        this.nonce = nonce.value().clone();

        key.consume();
        nonce.consume();
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o)
        {
            return true;
        }
        if (o == null || getClass() != o.getClass())
        {
            return false;
        }
        KeyGeneration that = (KeyGeneration)o;
        return generation == that.generation && Arrays.equals(key, that.key) && Arrays.equals(nonce, that.nonce);
    }

    void consume()
    {
        Arrays.fill(key, (byte)0);
        Arrays.fill(nonce, (byte)0);
    }
}
