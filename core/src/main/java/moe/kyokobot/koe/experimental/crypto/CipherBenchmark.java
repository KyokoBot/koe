package moe.kyokobot.koe.experimental.crypto;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import moe.kyokobot.koe.internal.crypto.EncryptionMode;
import moe.kyokobot.koe.internal.json.JsonArray;
import moe.kyokobot.koe.internal.json.JsonObject;
import moe.kyokobot.koe.internal.json.JsonParser;
import moe.kyokobot.koe.internal.json.JsonParserException;
import moe.kyokobot.koe.internal.util.AesAcceleration;
import moe.kyokobot.koe.internal.util.RTPHeaderWriter;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

/**
 * A rough benchmark of the encryption modes on the current JVM, good enough to tell which ones are fast here.
 */
public final class CipherBenchmark {
    public static final Duration DEFAULT_WARMUP = Duration.ofMillis(50);
    public static final Duration DEFAULT_ROUND = Duration.ofMillis(10);
    public static final int DEFAULT_ROUNDS = 5;

    // A 20ms Opus frame at Discord's default bitrate of 64 kbps
    private static final int PAYLOAD_SIZE = 160;
    private static final int BATCH = 32;

    private CipherBenchmark() {
        //
    }

    /**
     * Measures every supported encryption mode with the default durations, about 100ms per mode.
     *
     * @return the results
     * @see #run(Duration, Duration, int)
     */
    @NotNull
    public static Report run() {
        return run(DEFAULT_WARMUP, DEFAULT_ROUND, DEFAULT_ROUNDS);
    }

    /**
     * Measures every supported encryption mode. Blocks the calling thread for {@code warmup + round * rounds} per
     * mode, so call it once at startup, never on an event loop thread. Longer rounds give more stable results.
     *
     * @param warmup how long to run every mode before measuring, so the JIT compiler has optimized it
     * @param round  how long to measure every mode in a round, modes take turns between rounds
     * @param rounds number of rounds, the median of them is the result
     * @return the results
     */
    @NotNull
    public static Report run(@NotNull Duration warmup, @NotNull Duration round, int rounds) {
        var modes = new ArrayList<>(EncryptionMode.supportedModes());
        modes.remove("plain");
        return run(warmup, round, rounds, modes);
    }

    /**
     * Measures the given encryption modes, see {@link #run(Duration, Duration, int)}.
     *
     * @param modes names of the encryption modes to measure
     * @return the results
     * @throws IllegalArgumentException if a mode isn't supported
     */
    @NotNull
    public static Report run(@NotNull Duration warmup, @NotNull Duration round, int rounds,
                             @NotNull Collection<String> modes) {
        if (warmup.isNegative() || round.isNegative() || round.isZero() || rounds < 1) {
            throw new IllegalArgumentException("warmup can't be negative, round has to be positive and there has to be at least one round");
        }
        if (modes.isEmpty() || !EncryptionMode.supportedModes().containsAll(modes)) {
            throw new IllegalArgumentException("Unsupported encryption modes: " + modes);
        }

        var states = new ArrayList<ModeState>();
        try {
            for (var name : new LinkedHashSet<>(modes)) {
                states.add(new ModeState(name));
            }

            for (var state : states) {
                state.measure(warmup.toNanos());
            }

            var samples = new double[states.size()][rounds];
            for (int r = 0; r < rounds; r++) {
                // Rotate the order every round, so no mode always runs right after another
                for (int i = 0; i < states.size(); i++) {
                    int index = (i + r) % states.size();
                    samples[index][r] = states.get(index).measure(round.toNanos());
                }
            }

            var measurements = new ArrayList<Measurement>(states.size());
            for (int i = 0; i < states.size(); i++) {
                Arrays.sort(samples[i]);
                measurements.add(new Measurement(states.get(i).name, samples[i][rounds / 2]));
            }
            return new Report(Instant.now(), currentEnvironment(), PAYLOAD_SIZE, measurements);
        } finally {
            states.forEach(ModeState::release);
        }
    }

    /**
     * @return what the results depend on besides Koe itself: the JVM, the OS, the CPU and whether HotSpot uses AES
     * instructions
     */
    @NotNull
    public static Map<String, String> currentEnvironment() {
        var environment = new LinkedHashMap<String, String>();
        for (var property : List.of("java.vm.vendor", "java.vm.name", "java.vm.version", "os.name", "os.arch")) {
            environment.put(property, System.getProperty(property, ""));
        }
        environment.put("cpus", String.valueOf(Runtime.getRuntime().availableProcessors()));
        var useAes = AesAcceleration.readHotSpotFlag();
        environment.put("aes", useAes != null ? useAes.toString() : "unknown");
        return Collections.unmodifiableMap(environment);
    }

    /**
     * Results of a benchmark run, which can be stored as JSON to avoid running it on every start.
     */
    public static final class Report {
        /**
         * Bumped on significant changes to the encryption modes, which make older results misleading, and on
         * incompatible changes to the JSON format. {@link #fromJson(String)} rejects reports with another version.
         */
        public static final int VERSION = 1;

        private final Instant createdAt;
        private final Map<String, String> environment;
        private final int payloadSize;
        private final List<Measurement> measurements;

        Report(Instant createdAt, Map<String, String> environment, int payloadSize, List<Measurement> measurements) {
            this.createdAt = createdAt;
            this.environment = Collections.unmodifiableMap(new LinkedHashMap<>(environment));
            this.payloadSize = payloadSize;
            var sorted = new ArrayList<>(measurements);
            sorted.sort(Comparator.comparingDouble(Measurement::getNanosPerPacket));
            this.measurements = Collections.unmodifiableList(sorted);
        }

        @NotNull
        public Instant getCreatedAt() {
            return createdAt;
        }

        /**
         * @see CipherBenchmark#currentEnvironment()
         */
        @NotNull
        public Map<String, String> getEnvironment() {
            return environment;
        }

        /**
         * @return the size of the payload encrypted in every packet, in bytes
         */
        public int getPayloadSize() {
            return payloadSize;
        }

        /**
         * @return measurements from the fastest to the slowest mode
         */
        @NotNull
        public List<Measurement> getMeasurements() {
            return measurements;
        }

        /**
         * @return whether the results were measured on the same JVM, OS and CPU, otherwise they may not apply anymore
         * and the benchmark should run again
         */
        public boolean isFromCurrentEnvironment() {
            return environment.equals(currentEnvironment());
        }

        /**
         * @return a policy preferring modes from the fastest to the slowest
         */
        @NotNull
        public CipherPreferencePolicy toPolicy() {
            return CipherPreferencePolicy.preferring(measurements.stream()
                    .map(Measurement::getMode)
                    .collect(Collectors.toList()));
        }

        @NotNull
        public String toJson() {
            var environmentJson = new JsonObject();
            environmentJson.putAll(environment);

            var measurementsJson = new JsonArray();
            for (var measurement : measurements) {
                measurementsJson.add(new JsonObject()
                        .add("mode", measurement.mode)
                        .add("nanosPerPacket", measurement.nanosPerPacket));
            }

            return new JsonObject()
                    .add("version", VERSION)
                    .add("createdAt", createdAt.toString())
                    .add("environment", environmentJson)
                    .add("payloadSize", payloadSize)
                    .add("measurements", measurementsJson)
                    .toString();
        }

        /**
         * @param json a report from {@link #toJson()}
         * @return the parsed report
         * @throws IllegalArgumentException if the JSON is malformed or the report has another {@link #VERSION}
         */
        @NotNull
        public static Report fromJson(@NotNull String json) {
            try {
                var object = JsonParser.object().from(json);
                int version = object.getInt("version", -1);
                if (version != VERSION) {
                    throw new IllegalArgumentException("Cipher benchmark report has version " + version
                            + ", expected " + VERSION);
                }

                var environment = new LinkedHashMap<String, String>();
                var environmentJson = require(object.getObject("environment"), "environment");
                for (var entry : environmentJson.entrySet()) {
                    environment.put(entry.getKey(), String.valueOf(entry.getValue()));
                }

                var measurements = new ArrayList<Measurement>();
                for (var element : require(object.getArray("measurements"), "measurements")) {
                    if (!(element instanceof JsonObject)) {
                        throw new IllegalArgumentException("Malformed measurement: " + element);
                    }
                    var measurement = (JsonObject) element;
                    measurements.add(new Measurement(
                            require(measurement.getString("mode"), "mode"),
                            measurement.getDouble("nanosPerPacket", Double.NaN)));
                }

                return new Report(
                        Instant.parse(require(object.getString("createdAt"), "createdAt")),
                        environment,
                        object.getInt("payloadSize", PAYLOAD_SIZE),
                        measurements);
            } catch (JsonParserException | DateTimeParseException e) {
                throw new IllegalArgumentException("Malformed cipher benchmark report", e);
            }
        }

        private static <T> T require(T value, String name) {
            if (value == null) {
                throw new IllegalArgumentException("Cipher benchmark report is missing " + name);
            }
            return value;
        }

        @Override
        public String toString() {
            return measurements.toString();
        }
    }

    public static final class Measurement {
        private final String mode;
        private final double nanosPerPacket;

        Measurement(String mode, double nanosPerPacket) {
            if (!(nanosPerPacket > 0)) {
                throw new IllegalArgumentException("Invalid time for " + mode + ": " + nanosPerPacket);
            }
            this.mode = mode;
            this.nanosPerPacket = nanosPerPacket;
        }

        /**
         * @return the encryption mode name, as used by Discord
         */
        @NotNull
        public String getMode() {
            return mode;
        }

        /**
         * @return median time to encrypt a packet, in nanoseconds
         */
        public double getNanosPerPacket() {
            return nanosPerPacket;
        }

        @Override
        public String toString() {
            return String.format("%s: %.0f ns/packet", mode, nanosPerPacket);
        }
    }

    private static final class ModeState {
        private final String name;
        private final EncryptionMode mode;
        private final byte[] key = new byte[32];
        private final byte[] payload = new byte[PAYLOAD_SIZE];
        private final ByteBuf plain = Unpooled.directBuffer(PAYLOAD_SIZE);
        private final ByteBuf output = Unpooled.directBuffer(2048);
        private char seq;

        ModeState(String name) {
            this.name = name;
            this.mode = EncryptionMode.get(name);
            ThreadLocalRandom.current().nextBytes(key);
            ThreadLocalRandom.current().nextBytes(payload);
        }

        /**
         * @return average nanoseconds per packet over the given time
         */
        double measure(long durationNanos) {
            long start = System.nanoTime();
            long elapsed;
            long packets = 0;
            do {
                for (int i = 0; i < BATCH; i++) {
                    plain.clear().writeBytes(payload);
                    output.clear();
                    RTPHeaderWriter.writeV2(output, (byte) 120, seq++, 0, 0, false);
                    mode.box(plain, PAYLOAD_SIZE, output, key);
                }
                packets += BATCH;
                elapsed = System.nanoTime() - start;
            } while (elapsed < durationNanos);
            return (double) elapsed / packets;
        }

        void release() {
            plain.release();
            output.release();
        }
    }
}
