package moe.kyokobot.koe.mls.codec;

import java.io.IOException;

public class FramedContent
    implements MLSInputStream.Readable, MLSOutputStream.Writable
{
    byte[] group_id;
    long epoch;
    Sender sender;
    byte[] authenticated_data;
    byte[] application_data;

    final ContentType contentType;

    Proposal proposal;
    Commit commit;

    public Proposal getProposal()
    {
        return proposal;
    }

    public Commit getCommit()
    {
        return commit;
    }

    public Sender getSender()
    {
        return sender;
    }

    public byte[] getGroupID()
    {
        return group_id;
    }

    public long getEpoch()
    {
        return epoch;
    }

    public ContentType getContentType()
    {
        return contentType;
    }

    public byte[] getAuthenticated_data()
    {
        return authenticated_data;
    }

    public byte[] getContentBytes()
        throws IOException
    {
        switch (contentType)
        {

        case APPLICATION:
            return application_data;
        case PROPOSAL:
            return MLSOutputStream.encode(proposal);
        case COMMIT:
            return MLSOutputStream.encode(commit);
        default:
            return null;
        }
    }

    @SuppressWarnings("unused")
    public FramedContent(MLSInputStream stream)
        throws IOException
    {
        group_id = stream.readOpaque();
        epoch = (long)stream.read(long.class);
        sender = (Sender)stream.read(Sender.class);
        authenticated_data = stream.readOpaque();
        contentType = ContentType.values()[(byte)stream.read(byte.class)];
        switch (contentType)
        {
        case APPLICATION:
            application_data = stream.readOpaque();
            break;
        case PROPOSAL:
            proposal = (Proposal)stream.read(Proposal.class);
            break;
        case COMMIT:
            commit = (Commit)stream.read(Commit.class);
            break;
        }
    }

    public FramedContent(byte[] group_id, long epoch, Sender sender, byte[] authenticated_data, byte[] application_data, ContentType content_type, Proposal proposal, Commit commit)
    {
        this.group_id = group_id;
        this.epoch = epoch;
        this.sender = sender;
        this.authenticated_data = authenticated_data;
        this.application_data = application_data;
        this.contentType = content_type;
        this.proposal = proposal;
        this.commit = commit;
    }

    public static FramedContent rawContent(byte[] group_id, long epoch, Sender sender, byte[] authenticated_data, ContentType content_type, byte[] contentBytes)
        throws IOException
    {
        switch (content_type)
        {
        case APPLICATION:
            return application(group_id, epoch, sender, authenticated_data, contentBytes);
        case PROPOSAL:
            return proposal(group_id, epoch, sender, authenticated_data, contentBytes);
        case COMMIT:
            return commit(group_id, epoch, sender, authenticated_data, contentBytes);
        }
        return null;
    }

    public static FramedContent application(byte[] group_id, long epoch, Sender sender, byte[] authenticated_data, byte[] application_data)
    {
        return new FramedContent(group_id, epoch, sender, authenticated_data, application_data, ContentType.APPLICATION, null, null);
    }

    public static FramedContent proposal(byte[] group_id, long epoch, Sender sender, byte[] authenticated_data, byte[] proposal)
        throws IOException
    {
        return new FramedContent(group_id, epoch, sender, authenticated_data, null, ContentType.PROPOSAL, (Proposal)MLSInputStream.decode(proposal, Proposal.class), null);
    }

    public static FramedContent commit(byte[] group_id, long epoch, Sender sender, byte[] authenticated_data, byte[] commit)
        throws IOException
    {
        return new FramedContent(group_id, epoch, sender, authenticated_data, null, ContentType.COMMIT, null, (Commit)MLSInputStream.decode(commit, Commit.class));
    }

    @Override
    public void writeTo(MLSOutputStream stream)
        throws IOException
    {
        stream.writeOpaque(group_id);
        stream.write(epoch);
        stream.write(sender);
        stream.writeOpaque(authenticated_data);
        stream.write(contentType);

        switch (contentType)
        {
        case RESERVED:
            break;
        case APPLICATION:
            stream.writeOpaque(application_data);
            break;
        case PROPOSAL:
            stream.write(proposal);
            break;
        case COMMIT:
            stream.write(commit);
            break;
        }
    }
}
