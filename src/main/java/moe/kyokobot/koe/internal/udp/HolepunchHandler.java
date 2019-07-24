package moe.kyokobot.koe.internal.udp;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.DatagramPacket;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

public class HolepunchHandler extends SimpleChannelInboundHandler<DatagramPacket> {
    private final CompletableFuture<SocketAddress> future;
    private final AtomicInteger tries = new AtomicInteger();
    private final short ssrc;
    private volatile DatagramPacket packet;

    public HolepunchHandler(CompletableFuture<SocketAddress> future, short ssrc) {
        this.future = future;
        this.ssrc = ssrc;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        System.out.println("channel active, do holepunch?");
        holepunch(ctx);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, DatagramPacket packet) {
        System.out.println("channel read");
        if (!future.isDone()) {
            var buf = packet.content();
            buf.skipBytes(4);

            var stringBuilder = new StringBuilder();
            byte b;
            while ((b = buf.readByte()) != 0) {
                stringBuilder.append((char) b);
            }

            var ip = stringBuilder.toString();
            var port = buf.getUnsignedShort(68);

            ctx.pipeline().remove(this);
            packet.release();
            future.complete(new InetSocketAddress(ip, port));
        }
    }

    public void holepunch(ChannelHandlerContext ctx) {
        System.out.println("holepunch");
        if (packet == null) {
            var buf = Unpooled.buffer(70);
            buf.writeShort(ssrc);
            packet = new DatagramPacket(buf, (InetSocketAddress) ctx.channel().remoteAddress());
        }

        packet.retain();
        ctx.writeAndFlush(packet);
        System.out.println("refs: " + packet.refCnt());
    }
}
