package moe.kyokobot.koe.internal.util;

import com.sun.management.HotSpotDiagnosticMXBean;
import org.junit.jupiter.api.Test;

import java.lang.management.ManagementFactory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class AesAccelerationTest {
    @Test
    void hotSpotFlagIsReadWithoutLinkingToHotSpot() {
        HotSpotDiagnosticMXBean bean;
        try {
            bean = ManagementFactory.getPlatformMXBean(HotSpotDiagnosticMXBean.class);
        } catch (RuntimeException | LinkageError e) {
            bean = null;
        }
        assumeTrue(bean != null, "not running on HotSpot");

        var expected = Boolean.parseBoolean(bean.getVMOption("UseAES").getValue());
        assertEquals(expected, AesAcceleration.readHotSpotFlag());
    }

    @Test
    void onlyHotSpotReportingAesCountsAsAccelerated() {
        assertEquals(Boolean.TRUE.equals(AesAcceleration.readHotSpotFlag()), AesAcceleration.isLikelyAccelerated());
    }
}
