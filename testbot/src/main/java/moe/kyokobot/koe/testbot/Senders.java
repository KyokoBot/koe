package moe.kyokobot.koe.testbot;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.track.playback.MutableAudioFrame;
import io.netty.buffer.ByteBuf;
import moe.kyokobot.koe.MediaConnection;
import moe.kyokobot.koe.codec.Codec;
import moe.kyokobot.koe.codec.H264Codec;
import moe.kyokobot.koe.media.MediaFrameProvider;
import moe.kyokobot.koe.media.OpusAudioFrameProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static com.sedmelluq.discord.lavaplayer.format.StandardAudioDataFormats.DISCORD_OPUS;

public class Senders {
    private static final Logger logger = LoggerFactory.getLogger(Senders.class);
    
    public static class AudioSender extends OpusAudioFrameProvider {
        private final AudioPlayer player;
        private final MutableAudioFrame frame;
        private final ByteBuffer frameBuffer;

        AudioSender(AudioPlayer player, MediaConnection connection) {
            super(connection);
            this.player = player;
            this.frame = new MutableAudioFrame();
            this.frameBuffer = ByteBuffer.allocate(DISCORD_OPUS.maximumChunkSize());
            frame.setBuffer(frameBuffer);
            frame.setFormat(DISCORD_OPUS);
        }

        @Override
        public boolean canProvide() {
            return player.provide(frame);
        }

        @Override
        public void retrieveOpusFrame(ByteBuf targetBuffer) {
            targetBuffer.writeBytes(frameBuffer.array(), 0, frame.getDataLength());
        }
    }

    public static class VideoSender implements MediaFrameProvider {
        private final MediaConnection connection;
        private int frameInterval;
        private H264Reader reader;

        VideoSender(MediaConnection connection) {
            this.connection = connection;
            this.frameInterval = 33;
            try {
                this.reader = new H264Reader(new BufferedInputStream(new FileInputStream("output.h264")));
            } catch (Exception e) {
                throw new IllegalStateException("lol");
            }
        }

        @Override
        public void dispose() {

        }

        @Override
        public int getFrameInterval() {
            return frameInterval;
        }

        @Override
        public void setFrameInterval(int interval) {
            this.frameInterval = interval;
        }

        @Override
        public boolean canSendFrame(Codec codec) {
            return true;
        }

        @Override
        public boolean retrieve(Codec codec, ByteBuf buf, AtomicInteger timestamp, AtomicBoolean marker) {
            if (codec.getPayloadType() != H264Codec.PAYLOAD_TYPE) {
                return false;
            }

            int idx = buf.writerIndex();
            try {
                int nalUnit = this.reader.writeNextNALUnitData(buf);
                if (nalUnit == -1) {
                    buf.writerIndex(idx);

                    try {
                        this.reader = new H264Reader(new BufferedInputStream(new FileInputStream("output.h264")));
                    } catch (Exception e) {
                        //
                    }

                    return false;
                }

                int len = buf.writerIndex() - idx;

                if (nalUnit == 24 && len > 1) {
                    int nalUnitH = buf.getByte(idx + 1);
                    if ((nalUnitH & 0x40) != 0) {
                        if (marker != null) marker.set(false);
                    } else {
                        nalUnit = nalUnitH & 0x1f;
                    }
                }

                //logger.info("nal unit write: {} {}", nalUnit, len);

                switch (nalUnit) {
                    case 6:
                    case 7:
                    case 8:
                        if (marker != null) marker.set(false);
                        break;
                    case 9:
                        if (marker != null) marker.set(false);
                        timestamp.addAndGet(90000 * frameInterval / 1000);
                        return false;
                    default:
                        if (marker != null) marker.set(true);
                        break;
                }

                return true;
            } catch (IOException e) {
                buf.writerIndex(idx);
                return false;
            }
        }
    }
}
