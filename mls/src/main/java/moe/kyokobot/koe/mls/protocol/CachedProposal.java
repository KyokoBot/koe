package moe.kyokobot.koe.mls.protocol;

import moe.kyokobot.koe.mls.codec.Proposal;
import moe.kyokobot.koe.mls.TreeKEM.LeafIndex;

public class CachedProposal
{
    byte[] proposalRef;
    Proposal proposal;
    LeafIndex sender;

    public CachedProposal(byte[] proposalRef, Proposal proposal, LeafIndex sender)
    {
        this.proposalRef = proposalRef;
        this.proposal = proposal;
        this.sender = sender;
    }
}
