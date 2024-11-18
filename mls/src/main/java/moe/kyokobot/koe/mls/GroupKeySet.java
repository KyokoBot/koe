package moe.kyokobot.koe.mls;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidParameterException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import moe.kyokobot.koe.mls.codec.ContentType;
import moe.kyokobot.koe.mls.crypto.MlsCipherSuite;
import moe.kyokobot.koe.mls.TreeKEM.LeafIndex;
import moe.kyokobot.koe.mls.TreeKEM.NodeIndex;
import moe.kyokobot.koe.mls.crypto.Secret;

public class GroupKeySet
{
    final MlsCipherSuite suite;
    final int secretSize;
    // We store a commitment to the encryption secret that was used to create this structure, so that we can compare
    // for  purposes of equivalence checking without violating forward secrecy.
    final Secret encryptionSecretCommit;

    public SecretTree secretTree;
    Map<LeafIndex, HashRatchet> handshakeRatchets;
    Map<LeafIndex, HashRatchet> applicationRatchets;


    public GroupKeySet(MlsCipherSuite suite, TreeSize treeSize, Secret encryptionSecret) throws IOException {
        this.suite = suite;
        this.secretSize = suite.getKDF().getHashLength();
        this.encryptionSecretCommit = encryptionSecret.deriveSecret(suite, "commitment");
        this.secretTree = new SecretTree(treeSize, encryptionSecret);
        this.handshakeRatchets = new HashMap<>();
        this.applicationRatchets = new HashMap<>();
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o)
        {
            return true;
        }
        if (o == null || getClass() != o.getClass())
        {
            return false;
        }
        GroupKeySet that = (GroupKeySet)o;
        return secretSize == that.secretSize && suite.equals(that.suite) && encryptionSecretCommit.equals(that.encryptionSecretCommit);
    }

    void initRatchets(LeafIndex sender)
        throws IOException, IllegalAccessException
    {
        Secret leafSecret = secretTree.get(sender);

        Secret handshakeRatchetSecret = leafSecret.expandWithLabel(suite, "handshake", new byte[]{}, secretSize);
        Secret applicationRatchetSecret = leafSecret.expandWithLabel(suite, "application", new byte[]{}, secretSize);

        HashRatchet handshakeRatchet = new HashRatchet(suite, handshakeRatchetSecret);
        HashRatchet applicationRatchet = new HashRatchet(suite, applicationRatchetSecret);

        handshakeRatchets.put(sender, handshakeRatchet);
        applicationRatchets.put(sender, applicationRatchet);
    }

    public KeyGeneration get(ContentType contentType, LeafIndex sender, int generation, byte[] reuseGuard)
        throws IOException, IllegalAccessException
    {
        HashRatchet chain = getChain(contentType, sender);
        if (chain == null) {
            return null;
        }

        KeyGeneration keys = chain.get(generation);
        ApplyReuseGuard(reuseGuard, keys.nonce);
        return keys;
    }

    public KeyGeneration get(ContentType contentType, LeafIndex sender, byte[] reuseGuard)
        throws IOException, IllegalAccessException
    {
        HashRatchet chain = getChain(contentType, sender);
        if (chain == null) {
            return null;
        }

        KeyGeneration keys = chain.next();
        ApplyReuseGuard(reuseGuard, keys.nonce);
        return keys;
    }

    private void ApplyReuseGuard(byte[] guard, byte[] nonce)
    {
        for (int i = 0; i < guard.length; i++)
        {
            nonce[i] ^= guard[i];
        }
    }

    public void erase(ContentType contentType, LeafIndex sender, int generation)
        throws IOException, IllegalAccessException
    {
        HashRatchet chain = getChain(contentType, sender);
        if (chain != null) {
            chain.erase(generation);
        }
    }

    public HashRatchet handshakeRatchet(LeafIndex sender)
        throws IOException, IllegalAccessException
    {
        if (!handshakeRatchets.containsKey(sender))
        {
            initRatchets(sender);
        }
        return handshakeRatchets.get(sender);
    }

    public HashRatchet applicationRatchet(LeafIndex sender)
        throws IOException, IllegalAccessException
    {
        if (!applicationRatchets.containsKey(sender))
        {
            initRatchets(sender);
        }
        return applicationRatchets.get(sender);
    }

    public boolean hasLeaf(LeafIndex sender)
    {
        return secretTree.hasLeaf(sender);
    }

    private HashRatchet getChain(ContentType contentType, LeafIndex sender)
            throws IOException, IllegalAccessException {
        HashRatchet chain;

        switch (contentType)
        {
            case APPLICATION:
                chain = applicationRatchet(sender);
                break;
            case PROPOSAL:
            case COMMIT:
                chain = handshakeRatchet(sender);
                break;
            default:
                return null;
        }

        return chain;
    }

    public class SecretTree
    {
        final TreeSize treeSize;
        public Map<NodeIndex, Secret> secrets;

        public SecretTree(TreeSize treeSizeIn, Secret encryptionSecret)
        {
            treeSize = treeSizeIn;
            secrets = new HashMap<NodeIndex, Secret>();
            secrets.put(NodeIndex.root(treeSize), encryptionSecret);
        }

        protected boolean hasLeaf(LeafIndex sender)
        {
            return sender.value() < treeSize.leafCount();
        }

        public Secret get(LeafIndex leaf)
            throws IOException, IllegalAccessException
        {

            final byte[] leftLabel = "left".getBytes(StandardCharsets.UTF_8);
            final byte[] rightLabel = "right".getBytes(StandardCharsets.UTF_8);

            NodeIndex rootNode = NodeIndex.root(treeSize);
            NodeIndex leafNode = new NodeIndex(leaf);

            // Find an ancestor that is populated
            List<NodeIndex> dirpath = leaf.directPath(treeSize);
            dirpath.add(0, leafNode);
            dirpath.add(rootNode);
            int curr = 0;
            for (; curr < dirpath.size(); curr++)
            {
                if (secrets.containsKey(dirpath.get(curr)))
                {
                    break;
                }
            }

            if (curr > dirpath.size())
            {
                throw new InvalidParameterException("No secret found to derive leaf key");
            }

            // Derive down
            for (; curr > 0; curr--)
            {
                NodeIndex currNode = dirpath.get(curr);
                NodeIndex left = currNode.left();
                NodeIndex right = currNode.right();

                Secret secret = secrets.get(currNode);
                secrets.put(left, secret.expandWithLabel(suite, "tree", leftLabel, secretSize));
                secrets.put(right, secret.expandWithLabel(suite, "tree", rightLabel, secretSize));
            }

            // Get the leaf secret
            Secret leafSecret = secrets.get(leafNode);

            // Forget the secrets along the direct path
            for (NodeIndex i : dirpath)
            {
                if (i.equals(leafNode))
                {
                    continue;
                }

                if (secrets.containsKey(i))
                {
                    secrets.get(i).consume();
                    secrets.remove(i);
                }
            }

            return leafSecret;
        }
    }

}
