package moe.kyokobot.koe.mls.protocol;

import moe.kyokobot.koe.mls.codec.Proposal;
import org.bouncycastle.util.Arrays;

public class CachedUpdate
{
    byte[] updateSk;
    Proposal.Update update;

    public CachedUpdate(byte[] updateSk, Proposal.Update update)
    {
        this.updateSk = updateSk;
        this.update = update;
    }

    public void reset()
    {
        this.update = null;
        Arrays.clear(this.updateSk);
    }
}
