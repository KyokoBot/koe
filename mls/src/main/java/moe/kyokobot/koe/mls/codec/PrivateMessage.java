package moe.kyokobot.koe.mls.codec;

import java.io.IOException;

import moe.kyokobot.koe.mls.crypto.MlsCipherSuite;
import org.bouncycastle.crypto.InvalidCipherTextException;
import moe.kyokobot.koe.mls.GroupKeySet;
import moe.kyokobot.koe.mls.KeyGeneration;
import moe.kyokobot.koe.mls.KeyScheduleEpoch;
import moe.kyokobot.koe.mls.TreeKEM.LeafIndex;
import org.bouncycastle.util.Arrays;

public class PrivateMessage
    implements MLSInputStream.Readable, MLSOutputStream.Writable
{
    byte[] group_id;
    long epoch;
    ContentType content_type;
    byte[] authenticated_data;
    byte[] encrypted_sender_data;
    byte[] ciphertext;

    public PrivateMessage(byte[] group_id, long epoch, ContentType content_type, byte[] authenticated_data, byte[] encrypted_sender_data, byte[] ciphertext)
    {
        this.group_id = group_id;
        this.epoch = epoch;
        this.content_type = content_type;
        this.authenticated_data = authenticated_data;
        this.encrypted_sender_data = encrypted_sender_data;
        this.ciphertext = ciphertext;
    }

    @SuppressWarnings("unused")
    PrivateMessage(MLSInputStream stream)
        throws IOException
    {
        group_id = stream.readOpaque();
        epoch = (long)stream.read(long.class);
        content_type = ContentType.values()[(byte)stream.read(byte.class)];
        authenticated_data = stream.readOpaque();
        encrypted_sender_data = stream.readOpaque();
        ciphertext = stream.readOpaque();
    }

    static public PrivateMessage protect(AuthenticatedContent auth, MlsCipherSuite suite, GroupKeySet keys, byte[] senderDataSecretBytes, int paddingSize)
        throws IOException, IllegalAccessException, InvalidCipherTextException
    {
        // Get KeyGeneration from the secret tree
        LeafIndex index = auth.content.sender.sender;
        ContentType contentType = auth.content.contentType;
        byte[] reuseGuard = new byte[4];
        KeyGeneration keyGen = keys.get(contentType, index, reuseGuard);

        // Encrypt the content
        byte[] contentPt = serializeContentPt(auth.content, auth.auth, paddingSize);

        PrivateContentAAD contentAAD = new PrivateContentAAD(
            auth.content.group_id,
            auth.content.epoch,
            auth.content.contentType,
            auth.content.authenticated_data
        );

        byte[] contentCt = suite.getAEAD().seal(
            keyGen.key,
            keyGen.nonce,
            MLSOutputStream.encode(contentAAD),
            contentPt
        );

        // Encrypt the sender data
        LeafIndex senderIndex = auth.content.sender.sender;
        SenderData senderDataPt = new SenderData(
            senderIndex,
            keyGen.generation,
            reuseGuard
        );
        SenderDataAAD senderDataAAD = new SenderDataAAD(
            auth.content.group_id,
            auth.content.epoch,
            auth.content.contentType
        );

        KeyGeneration senderDataKeys = KeyScheduleEpoch.senderDataKeys(suite, senderDataSecretBytes.clone(), contentCt);
        byte[] senderDataCt = suite.getAEAD().seal(
            senderDataKeys.key,
            senderDataKeys.nonce,
            MLSOutputStream.encode(senderDataAAD),
            MLSOutputStream.encode(senderDataPt)
        );

        return new PrivateMessage(
            auth.content.group_id,
            auth.content.epoch,
            auth.content.contentType,
            auth.content.authenticated_data,
            senderDataCt.clone(),
            contentCt.clone()
        );
    }

    public AuthenticatedContent unprotect(MlsCipherSuite suite, GroupKeySet keys, byte[] senderDataSecretBytes)
            throws Exception
    {
        // Decrypt and parse the sender data

        KeyGeneration senderKeys = KeyScheduleEpoch.senderDataKeys(suite, senderDataSecretBytes.clone(), ciphertext.clone());

        SenderDataAAD senderDataAAD = new SenderDataAAD(group_id, epoch, content_type);

        byte[] senderDataPt = suite.getAEAD().open(
            senderKeys.key,
            senderKeys.nonce,
            MLSOutputStream.encode(senderDataAAD),
            encrypted_sender_data
        );

        SenderData senderData = (SenderData)MLSInputStream.decode(senderDataPt, SenderData.class);

        //Cannot Process Message from self!
        if (!keys.hasLeaf(senderData.sender))
        {
            return null;
        }

        // Decrypt the content
        KeyGeneration contentKeys = keys.get(content_type, senderData.sender, senderData.generation, senderData.reuseGuard);

        PrivateContentAAD contentAAD = new PrivateContentAAD(group_id, epoch, content_type, authenticated_data);
        byte[] contentPtBytes = suite.getAEAD().open(
            contentKeys.key,
            contentKeys.nonce,
            MLSOutputStream.encode(contentAAD),
            ciphertext
        );

        keys.erase(content_type, senderData.sender, senderData.generation);

        // Parse Content
        FramedContent content = new FramedContent(
            group_id,
            epoch,
            Sender.forMember(senderData.sender),
            authenticated_data,
            null,
            content_type,
            null,
            null
        );

        FramedContentAuthData auth = new FramedContentAuthData(
            content_type,
            null,
            null
        );
        deserializeContentPt(contentPtBytes, content, auth);

        return new AuthenticatedContent(
            WireFormat.mls_private_message,
            content,
            auth
        );
    }

    @Override
    public void writeTo(MLSOutputStream stream)
        throws IOException
    {
        stream.writeOpaque(group_id);
        stream.write(epoch);
        stream.write(content_type);
        stream.writeOpaque(authenticated_data);
        stream.writeOpaque(encrypted_sender_data);
        stream.writeOpaque(ciphertext);
    }

    private void deserializeContentPt(byte[] contentPt, FramedContent content, FramedContentAuthData auth)
        throws IOException
    {
        MLSInputStream stream = new MLSInputStream(contentPt);
        switch (content_type)
        {
        case APPLICATION:
            content.application_data = stream.readOpaque();
            break;
        case PROPOSAL:
            content.proposal = (Proposal)stream.read(Proposal.class);
            break;
        case COMMIT:
            content.commit = (Commit)stream.read(Commit.class);
            break;
        }
        auth.signature = stream.readOpaque();
        switch (content_type)
        {
        case APPLICATION:
        case PROPOSAL:
            break;
        case COMMIT:
            auth.confirmation_tag = stream.readOpaque();
            break;
        }
        //TODO: read padding?
    }

    static private byte[] serializeContentPt(FramedContent content, FramedContentAuthData auth, int paddingSize)
        throws IOException
    {
        MLSOutputStream stream = new MLSOutputStream();
        switch (content.contentType)
        {
        case APPLICATION:
            stream.writeOpaque(content.application_data);
            break;
        case PROPOSAL:
            stream.write(content.proposal);
            break;
        case COMMIT:
            stream.write(content.commit);
            break;
        }
        stream.writeOpaque(auth.signature);
        switch (content.contentType)
        {
        case APPLICATION:
        case PROPOSAL:
            break;
        case COMMIT:
            stream.writeOpaque(auth.confirmation_tag);
            break;
        }
        return Arrays.concatenate(stream.toByteArray(), new byte[paddingSize]);
    }
}
