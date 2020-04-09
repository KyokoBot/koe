package moe.kyokobot.koe.codec.netty;

import moe.kyokobot.koe.VoiceConnection;
import moe.kyokobot.koe.codec.AbstractFramePoller;
import moe.kyokobot.koe.codec.OpusCodec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class NettyOpusFramePoller extends AbstractFramePoller {
    private static final Logger logger = LoggerFactory.getLogger(NettyOpusFramePoller.class);

    public NettyOpusFramePoller(VoiceConnection connection) {
        super(connection);
        this.timestamp = new AtomicInteger();
    }

    private long lastFrame;
    private AtomicInteger timestamp;

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
            var handler = connection.getConnectionHandler();
            var sender = connection.getSender();

            if (sender != null && handler != null && sender.canSendFrame()) {
                var buf = allocator.buffer();
                int start = buf.writerIndex();
                sender.retrieve(OpusCodec.INSTANCE, buf);
                int len = buf.writerIndex() - start;
                handler.sendFrame(OpusCodec.PAYLOAD_TYPE, timestamp.getAndAdd(960),
                        buf, len);
                buf.release();
            }
        } catch (Exception e) {
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
