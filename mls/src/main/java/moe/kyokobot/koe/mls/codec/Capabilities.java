package moe.kyokobot.koe.mls.codec;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import moe.kyokobot.koe.mls.crypto.MlsCipherSuite;

public class Capabilities
        implements MLSInputStream.Readable, MLSOutputStream.Writable {
    static private final Short[] DEFAULT_SUPPORTED_VERSIONS = {ProtocolVersion.mls10.value};
    static private final short[] DEFAULT_SUPPORTED_CIPHERSUITES = MlsCipherSuite.ALL_SUPPORTED_SUITES;
    static private final Short[] DEFAULT_SUPPORTED_CREDENTIALS = {CredentialType.basic.value, CredentialType.x509.value};
    List<Short> versions;
    List<Short> cipherSuites;
    List<Short> extensions;
    List<Short> proposals;
    List<Short> credentials;

    public Capabilities() {
        versions = Arrays.asList(DEFAULT_SUPPORTED_VERSIONS);
        cipherSuites = new ArrayList<>();
        for (short suite : DEFAULT_SUPPORTED_CIPHERSUITES) {
            cipherSuites.add(suite);
        }
        extensions = new ArrayList<>();
        proposals = new ArrayList<>();
        credentials = Arrays.asList(DEFAULT_SUPPORTED_CREDENTIALS);
    }

    public List<Short> getVersions() {
        return versions;
    }

    public List<Short> getCipherSuites() {
        return cipherSuites;
    }

    public List<Short> getExtensions() {
        return extensions;
    }

    public List<Short> getProposals() {
        return proposals;
    }

    public List<Short> getCredentials() {
        return credentials;
    }

    @SuppressWarnings("unused")
    Capabilities(MLSInputStream stream)
            throws IOException {
        versions = new ArrayList<>();
        cipherSuites = new ArrayList<>();
        extensions = new ArrayList<>();
        proposals = new ArrayList<>();
        credentials = new ArrayList<>();
        stream.readList(versions, short.class);
        stream.readList(cipherSuites, short.class);
        stream.readList(extensions, short.class);
        stream.readList(proposals, short.class);
        stream.readList(credentials, short.class);
    }

    @Override
    public void writeTo(MLSOutputStream stream)
            throws IOException {
        stream.writeList(versions);
        stream.writeList(cipherSuites);
        stream.writeList(extensions);
        stream.writeList(proposals);
        stream.writeList(credentials);
    }
}
