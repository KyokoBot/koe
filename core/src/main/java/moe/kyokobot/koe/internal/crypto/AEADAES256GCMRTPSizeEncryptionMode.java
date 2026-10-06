package moe.kyokobot.koe.internal.crypto;

public class AEADAES256GCMRTPSizeEncryptionMode extends AEADAES256GCMEncryptionMode {

    public AEADAES256GCMRTPSizeEncryptionMode() {
        super();
    }

    AEADAES256GCMRTPSizeEncryptionMode(int initialSeq) {
        super(initialSeq);
    }

    @Override
    public boolean isRtpSize() {
        return true;
    }

    @Override
    public String getName() {
        return "aead_aes256_gcm_rtpsize";
    }
}
