package moe.kyokobot.koe.internal.util;

import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;

import java.util.concurrent.CompletableFuture;

public class NettyFutureWrapper<V> implements GenericFutureListener<Future<V>> {
    private final CompletableFuture<V> javaFuture;

    public NettyFutureWrapper(CompletableFuture<V> javaFuture) {
        this.javaFuture = javaFuture;
    }

    @Override
    public void operationComplete(Future<V> future) throws Exception {
        if (future.isSuccess()) {
            javaFuture.complete(future.get());
        } else {
            javaFuture.completeExceptionally(future.cause());
        }
    }
}
