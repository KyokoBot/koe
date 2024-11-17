package moe.kyokobot.koe.mls.TreeKEM;

import java.util.List;

public class Utils
{

    static protected void removeLeaves(List<NodeIndex> res, List<LeafIndex> except)
    {
        for (LeafIndex leaf : except)
        {
            res.remove(new NodeIndex(leaf));
        }
    }

}
