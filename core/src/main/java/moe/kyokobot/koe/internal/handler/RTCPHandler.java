package moe.kyokobot.koe.internal.handler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.DatagramPacket;

public class RTCPHandler extends SimpleChannelInboundHandler<DatagramPacket> {
    // https://tools.ietf.org/html/rfc3550#section-6
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, DatagramPacket packet) {
    }
}
