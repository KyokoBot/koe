package moe.kyokobot.koe.mls.codec;

import java.io.IOException;

public class EncryptedGroupSecrets
    implements MLSInputStream.Readable, MLSOutputStream.Writable
{

    byte[] new_member; // KeyPackageRaf
    HPKECiphertext encrypted_group_secrets;

    public EncryptedGroupSecrets(byte[] new_member, HPKECiphertext encrypted_group_secrets)
    {
        this.new_member = new_member;
        this.encrypted_group_secrets = encrypted_group_secrets;
    }

    @SuppressWarnings("unused")
    EncryptedGroupSecrets(MLSInputStream stream)
        throws IOException
    {
        new_member = stream.readOpaque();
        encrypted_group_secrets = (HPKECiphertext)stream.read(HPKECiphertext.class);
    }

    @Override
    public void writeTo(MLSOutputStream stream)
        throws IOException
    {
        stream.writeOpaque(new_member);
        stream.write(encrypted_group_secrets);
    }
}
