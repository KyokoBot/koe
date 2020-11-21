package moe.kyokobot.koe.internal.handler;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.DatagramPacket;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.net.SocketTimeoutException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class HolepunchHandler extends SimpleChannelInboundHandler<DatagramPacket> {
    private static final Logger logger = LoggerFactory.getLogger(HolepunchHandler.class);

    private final CompletableFuture<InetSocketAddress> future;
    private final int ssrc;
    private int tries = 0;
    private DatagramPacket packet;

    public HolepunchHandler(CompletableFuture<InetSocketAddress> future, int ssrc) {
        this.future = future;
        this.ssrc = ssrc;
    }

    @Override
    public void channelActive(@NotNull ChannelHandlerContext ctx) {
        holepunch(ctx);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, DatagramPacket packet) {
        var buf = packet.content();

        if (!future.isDone()) {
            if (buf.readableBytes() != 74) return;

            buf.skipBytes(8);

            var stringBuilder = new StringBuilder();
            byte b;
            while ((b = buf.readByte()) != 0) {
                stringBuilder.append((char) b);
            }

            var ip = stringBuilder.toString();
            var port = buf.getUnsignedShort(72);

            ctx.pipeline().remove(this);
            future.complete(new InetSocketAddress(ip, port));
        }
    }

    public void holepunch(ChannelHandlerContext ctx) {
        if (future.isDone()) {
            return;
        } else if (tries++ > 10) {
            logger.debug("Discovery failed.");
            future.completeExceptionally(new SocketTimeoutException("Failed to discover external UDP address."));
            return;
        }

        logger.debug("Holepunch [attempt {}/10, local ip: {}]", tries, ctx.channel().localAddress());

        if (packet == null) {
            var buf = Unpooled.buffer(74);
            buf.writeShort(1);
            buf.writeShort(0x46);
            buf.writeInt(ssrc);
            buf.writerIndex(74);
            packet = new DatagramPacket(buf, (InetSocketAddress) ctx.channel().remoteAddress());
        }

        packet.retain();
        ctx.writeAndFlush(packet);

        ctx.executor().schedule(() -> holepunch(ctx), 1, TimeUnit.SECONDS);
    }
}
