package moe.kyokobot.koe.codec;

import moe.kyokobot.koe.VoiceConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class OpusCodec extends AbstractCodec {
    private static final Logger logger = LoggerFactory.getLogger(OpusCodec.class);

    public static final byte PAYLOAD_TYPE = (byte) 120;
    public static final OpusCodec INSTANCE = new OpusCodec();

    public OpusCodec() {
        super("opus", PAYLOAD_TYPE, 100, CodecType.AUDIO);
    }

    @Override
    public FramePoller createFramePoller(VoiceConnection connection) {
        return new OpusFramePoller(connection);
    }

    public static class OpusFramePoller extends AbstractFramePoller {
        public OpusFramePoller(VoiceConnection connection) {
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
                if (connection.getSender() != null && connection.getConnectionHandler() != null
                        && connection.getSender().canSendFrame()) {
                    var buf = allocator.buffer();
                    int start = buf.writerIndex();
                    connection.getSender().retrieve(OpusCodec.INSTANCE, buf);
                    int len = buf.writerIndex() - start;
                    connection.getConnectionHandler().sendFrame(OpusCodec.PAYLOAD_TYPE, timestamp.getAndAdd(960),
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
}
