package moe.kyokobot.koe.dave;

public interface KeyRatchet {
    byte[] getKey(int keyGeneration);

    void deleteKey(int keyGeneration);
}
