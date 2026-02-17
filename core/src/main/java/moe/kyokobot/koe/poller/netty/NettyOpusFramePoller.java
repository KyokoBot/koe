package moe.kyokobot.koe.poller.netty;

import io.netty.buffer.ByteBuf;
import moe.kyokobot.koe.MediaConnection;
import moe.kyokobot.koe.codec.CodecInstance;
import moe.kyokobot.koe.handler.ConnectionHandler;
import moe.kyokobot.koe.poller.AbstractOpusFramePoller;
import org.jetbrains.annotations.NotNull;

public class NettyOpusFramePoller extends AbstractOpusFramePoller {
    public NettyOpusFramePoller(@NotNull CodecInstance codec, @NotNull MediaConnection connection) {
        super(connection, codec);
    }

    @Override
    protected boolean canSendFrame() {
        return connection.getConnectionHandler() != null;
    }

    @Override
    protected void sendFramePayload(ByteBuf buf, int len, int timestamp) {
        var handler = connection.getConnectionHandler();
        if (handler == null) {
            return;
        }
        handler.sendFrame(codec.getPayloadType(), timestamp, buf, len, false);
    }
}
