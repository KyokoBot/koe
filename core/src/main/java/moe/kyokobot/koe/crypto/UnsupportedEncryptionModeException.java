package moe.kyokobot.koe.crypto;

public class UnsupportedEncryptionModeException extends IllegalArgumentException {
    public UnsupportedEncryptionModeException(String message) {
        super(message);
    }
}
