package moe.kyokobot.koe.codec.netty;

import moe.kyokobot.koe.MediaConnection;
import moe.kyokobot.koe.codec.AbstractFramePoller;
import moe.kyokobot.koe.codec.H264Codec;
import moe.kyokobot.koe.media.IntReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

public class NettyH264FramePoller extends AbstractFramePoller {
    private static final Logger logger = LoggerFactory.getLogger(NettyH264FramePoller.class);
    /**
     * Delay between frame polling attempts.
     */
    private static final int FRAME_RATE = 1000 / 30;

    public NettyH264FramePoller(MediaConnection connection) {
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

        boolean pollNext = false;
        try {
            do {
                var handler = connection.getConnectionHandler();
                var sender = connection.getAudioSender();
                var codec = H264Codec.INSTANCE;

                if (sender != null && handler != null && sender.canSendFrame(codec)) {
                    var buf = allocator.buffer();
                    int start = buf.writerIndex();
                    pollNext = sender.retrieve(codec, buf, timestamp);
                    int len = buf.writerIndex() - start;
                    if (len != 0) {
                        handler.sendFrame(H264Codec.PAYLOAD_TYPE, timestamp.get(), buf, len, true);
                    }
                    buf.release();
                }
            } while (pollNext);
        } catch (Exception e) {
            logger.error("Sending frame failed", e);
        }

        long frameDelay = FRAME_RATE - (System.currentTimeMillis() - lastFrame);

        if (frameDelay > 0) {
            eventLoop.schedule(this::loop, frameDelay, TimeUnit.MILLISECONDS);
        } else {
            loop();
        }
    }

    private void loop() {
        if (System.currentTimeMillis() < lastFrame + 60) {
            lastFrame += FRAME_RATE;
        } else {
            lastFrame = System.currentTimeMillis();
        }

        pollFrame();
    }
}
