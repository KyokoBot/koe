package moe.kyokobot.koe.internal.crypto;

/**
 * Same as xsalsa20_poly1305_lite, except a header extension preamble stays unencrypted as part of the RTP header.
 */
public class XSalsa20Poly1305LiteRTPSizeEncryptionMode extends XSalsa20Poly1305LiteEncryptionMode {

    public XSalsa20Poly1305LiteRTPSizeEncryptionMode() {
        super();
    }

    XSalsa20Poly1305LiteRTPSizeEncryptionMode(int initialSeq) {
        super(initialSeq);
    }

    @Override
    public boolean isRtpSize() {
        return true;
    }

    @Override
    public String getName() {
        return "xsalsa20_poly1305_lite_rtpsize";
    }
}
