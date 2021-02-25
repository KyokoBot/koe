package moe.kyokobot.koe.codec.netty;

import io.netty.buffer.ByteBuf;
import moe.kyokobot.koe.MediaConnection;
import moe.kyokobot.koe.codec.AbstractFramePoller;
import moe.kyokobot.koe.codec.OpusCodec;
import moe.kyokobot.koe.handler.ConnectionHandler;
import moe.kyokobot.koe.media.IntReference;
import moe.kyokobot.koe.media.MediaFrameProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

public class NettyOpusFramePoller extends AbstractFramePoller {
    private static final Logger logger = LoggerFactory.getLogger(NettyOpusFramePoller.class);

    public NettyOpusFramePoller(MediaConnection connection) {
        super(connection);
    }

    /**
     * Last frame time in ms.
     */
    private long lastFrame = 0;

    /**
     * Current frame timestamp.
     */
    private final IntReference timestamp = new IntReference();

    @Override
    public void start() {
        if (this.polling) {
            throw new IllegalStateException("Polling already started!");
        }

        this.polling = true;
        this.lastFrame = System.currentTimeMillis();
        eventLoop.execute(this::pollFrame);
    }

    @Override
    public void stop() {
        this.polling = false;
    }

    private void pollFrame() {
        if (!this.polling) {
            return;
        }

        try {
            ConnectionHandler<?> handler = connection.getConnectionHandler();
            MediaFrameProvider sender = connection.getAudioSender();
            OpusCodec codec = OpusCodec.INSTANCE;

            // ugly but it's the hottest path in Koe and Java is a shit language.
            if (sender != null && handler != null && sender.canSendFrame(codec)) {
                ByteBuf buf = allocator.buffer();
                int start = buf.writerIndex();
                // opus codec doesn't need framing, we don't handle multiple packet cases.
                sender.retrieve(codec, buf, timestamp);
                int len = buf.writerIndex() - start;
                if (len != 0) {
                    handler.sendFrame(OpusCodec.PAYLOAD_TYPE, timestamp.get(), buf, len, false);
                }
                buf.release();
            }
        } catch (Exception e) { // get rid of somehow?
            logger.error("Sending frame failed", e);
        }

        long frameDelay = 20 - (System.currentTimeMillis() - lastFrame);

        if (frameDelay > 0) {
            eventLoop.schedule(this::loop, frameDelay, TimeUnit.MILLISECONDS);
        } else {
            loop();
        }
    }

    private void loop() {
        if (System.currentTimeMillis() < lastFrame + 60) {
            lastFrame += 20;
        } else {
            lastFrame = System.currentTimeMillis();
        }

        pollFrame();
    }
}
