package moe.kyokobot.koe.testbot;

import io.netty.buffer.PooledByteBufAllocator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NettyLeakDetect {
    private static final Logger logger = LoggerFactory.getLogger(NettyLeakDetect.class);
    private final PooledByteBufAllocator allocator;

    public NettyLeakDetect() {
        allocator = new PooledByteBufAllocator(true, 1, 1, 8192, 11, 0, 0, false);
    }

    public PooledByteBufAllocator getAllocator() {
        return allocator;
    }

    public void printAllocStats() {
        long directActive = 0;
        long directAlloc = 0;
        long directDealloc = 0;
        long heapActive = 0;
        long heapAlloc = 0;
        long heapDealloc = 0;
        for (var arena : allocator.metric().directArenas()) {
            directActive += arena.numActiveAllocations();
            directAlloc += arena.numAllocations();
            directDealloc += arena.numDeallocations();
        }

        for (var arena : allocator.metric().heapArenas()) {
            heapActive += arena.numActiveAllocations();
            heapAlloc += arena.numAllocations();
            heapDealloc += arena.numDeallocations();
        }
        logger.info("{}", allocator.dumpStats());
        logger.info("Direct Active:  {}", directActive);
        logger.info("Direct Alloc:   {}", directAlloc);
        logger.info("Direct Dealloc: {}", directDealloc);
        logger.info("Heap Active:    {}", heapActive);
        logger.info("Heap Alloc:     {}", heapAlloc);
        logger.info("Heap Dealloc:   {}", heapDealloc);
    }
}
