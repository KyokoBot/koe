package moe.kyokobot.koe.dave.mls;

import moe.kyokobot.koe.mls.codec.Capabilities;
import moe.kyokobot.koe.mls.codec.CredentialType;
import moe.kyokobot.koe.mls.codec.Extension;
import moe.kyokobot.koe.mls.codec.ExternalSender;
import moe.kyokobot.koe.mls.crypto.MlsCipherSuite;

import java.io.IOException;
import java.util.List;

public class Parameters {
    public static short ciphersuiteIDForProtocolVersion(@SuppressWarnings("unused") int protocolVersion) {
        return MlsCipherSuite.MLS_128_DHKEMP256_AES128GCM_SHA256_P256;
    }

    public static MlsCipherSuite ciphersuiteForProtocolVersion(int protocolVersion) {
        try {
            return MlsCipherSuite.getSuite(ciphersuiteIDForProtocolVersion(protocolVersion));
        } catch (Exception e) {
            // should never happen
            throw new RuntimeException(e);
        }
    }

    public static short ciphersuiteIDForSignatureVersion(@SuppressWarnings("unused") int signatureVersion) {
        return MlsCipherSuite.MLS_128_DHKEMP256_AES128GCM_SHA256_P256;
    }

    public static MlsCipherSuite ciphersuiteForSignatureVersion(int signatureVersion) {
        try {
            return MlsCipherSuite.getSuite(ciphersuiteIDForSignatureVersion(signatureVersion));
        } catch (Exception e) {
            // should never happen
            throw new RuntimeException(e);
        }
    }

    public static Capabilities leafNodeCapabilitiesForProtocolVersion(int protocolVersion) {
        var capabilities = new Capabilities();

        var cipherSuites = capabilities.getCipherSuites();
        var credentials = capabilities.getCredentials();

        cipherSuites.clear();
        cipherSuites.add(ciphersuiteIDForProtocolVersion(protocolVersion));

        credentials.clear();
        credentials.add(CredentialType.basic.getValue());

        return capabilities;
    }

    public static List<Extension> leafNodeExtensionsForProtocolVersion(@SuppressWarnings("unused") int protocolVersion) {
        return List.of();
    }

    public static List<Extension> groupExtensionsForProtocolVersion(
            @SuppressWarnings("unused") int protocolVersion,
            ExternalSender externalSender) throws IOException {
        return List.of(Extension.externalSender(List.of(externalSender)));
    }
}
