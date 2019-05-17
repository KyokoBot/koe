package moe.kyokobot.koe.crypto;

public class UnsupportedEncryptionModeException extends IllegalArgumentException {
    private final String mode;

    public UnsupportedEncryptionModeException(String mode) {
        this.mode = mode;
    }

    @Override
    public String getMessage() {
        return "Encryption mode " + mode + " is not supported!";
    }

    @Override
    public String getLocalizedMessage() {
        return getMessage();
    }
}
