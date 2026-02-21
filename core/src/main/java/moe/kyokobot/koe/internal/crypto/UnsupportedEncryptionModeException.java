package moe.kyokobot.koe.internal.crypto;

public class UnsupportedEncryptionModeException extends IllegalArgumentException {
    public UnsupportedEncryptionModeException(String message) {
        super(message);
    }
}
