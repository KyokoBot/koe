package moe.kyokobot.koe.experimental.crypto;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class CipherBenchmarkTest {
    private static final String AES_GCM = "aead_aes256_gcm_rtpsize";
    private static final String XCHACHA20_POLY1305 = "aead_xchacha20_poly1305_rtpsize";
    private static final Set<String> ENCRYPTED_MODES = Set.of(AES_GCM, XCHACHA20_POLY1305,
            "xsalsa20_poly1305", "xsalsa20_poly1305_lite", "xsalsa20_poly1305_suffix");

    @Test
    void ranksEveryEncryptedModeFromFastestToSlowest() {
        var report = CipherBenchmark.run(Duration.ofMillis(1), Duration.ofMillis(1), 3);
        var measurements = report.getMeasurements();

        assertEquals(ENCRYPTED_MODES, measurements.stream().map(CipherBenchmark.Measurement::getMode).collect(Collectors.toSet()));
        for (int i = 1; i < measurements.size(); i++) {
            assertTrue(measurements.get(i - 1).getNanosPerPacket() <= measurements.get(i).getNanosPerPacket(), report.toString());
        }
        assertEquals(CipherBenchmark.currentEnvironment(), report.getEnvironment());
        assertTrue(report.isFromCurrentEnvironment());
    }

    @Test
    void measuresOnlyTheGivenModes() {
        var report = CipherBenchmark.run(Duration.ZERO, Duration.ofMillis(1), 1, List.of(AES_GCM, XCHACHA20_POLY1305));

        assertEquals(Set.of(AES_GCM, XCHACHA20_POLY1305),
                report.getMeasurements().stream().map(CipherBenchmark.Measurement::getMode).collect(Collectors.toSet()));
        assertThrows(IllegalArgumentException.class,
                () -> CipherBenchmark.run(Duration.ZERO, Duration.ofMillis(1), 1, List.of("made_up_mode")));
        assertThrows(IllegalArgumentException.class,
                () -> CipherBenchmark.run(Duration.ZERO, Duration.ofMillis(1), 1, List.of()));
    }

    @Test
    void rejectsInvalidDurations() {
        assertThrows(IllegalArgumentException.class, () -> CipherBenchmark.run(Duration.ofMillis(-1), Duration.ofMillis(1), 1));
        assertThrows(IllegalArgumentException.class, () -> CipherBenchmark.run(Duration.ZERO, Duration.ZERO, 1));
        assertThrows(IllegalArgumentException.class, () -> CipherBenchmark.run(Duration.ZERO, Duration.ofMillis(1), 0));
    }

    @Test
    void reportSurvivesJson() {
        var report = report(CipherBenchmark.currentEnvironment());

        var parsed = CipherBenchmark.Report.fromJson(report.toJson());

        assertEquals(report.getCreatedAt(), parsed.getCreatedAt());
        assertEquals(report.getEnvironment(), parsed.getEnvironment());
        assertEquals(report.getPayloadSize(), parsed.getPayloadSize());
        assertEquals(report.toString(), parsed.toString());
        assertEquals(report.getMeasurements().get(0).getNanosPerPacket(), parsed.getMeasurements().get(0).getNanosPerPacket());
        assertTrue(parsed.isFromCurrentEnvironment());
    }

    @Test
    void reportFromAnotherEnvironmentIsStale() {
        var environment = new HashMap<>(CipherBenchmark.currentEnvironment());
        environment.put("java.vm.version", "1.0");

        assertFalse(CipherBenchmark.Report.fromJson(report(environment).toJson()).isFromCurrentEnvironment());
    }

    @Test
    void rejectsOtherVersionsAndMalformedJson() {
        var json = report(CipherBenchmark.currentEnvironment()).toJson();

        assertThrows(IllegalArgumentException.class, () -> CipherBenchmark.Report.fromJson(
                json.replace("\"version\":" + CipherBenchmark.Report.VERSION, "\"version\":" + (CipherBenchmark.Report.VERSION + 1))));
        assertThrows(IllegalArgumentException.class, () -> CipherBenchmark.Report.fromJson("{"));
        assertThrows(IllegalArgumentException.class, () -> CipherBenchmark.Report.fromJson(
                "{\"version\":" + CipherBenchmark.Report.VERSION + "}"));
    }

    @Test
    void policyPrefersTheFastestMode() {
        var policy = report(CipherBenchmark.currentEnvironment()).toPolicy();

        assertEquals(XCHACHA20_POLY1305, policy.select(List.of(AES_GCM, XCHACHA20_POLY1305), ENCRYPTED_MODES));
        assertEquals(AES_GCM, policy.select(List.of(AES_GCM), ENCRYPTED_MODES));
    }

    private static CipherBenchmark.Report report(java.util.Map<String, String> environment) {
        return new CipherBenchmark.Report(Instant.parse("2026-10-05T12:00:00Z"), environment, 160, List.of(
                new CipherBenchmark.Measurement(AES_GCM, 3369.5),
                new CipherBenchmark.Measurement(XCHACHA20_POLY1305, 1338.9)));
    }
}
