package moe.kyokobot.koe.internal.util;

public final class AesAcceleration {
    private static volatile Boolean accelerated;

    private AesAcceleration() {
        //
    }

    public static boolean isLikelyAccelerated() {
        var result = accelerated;
        if (result == null) {
            result = detect();
            accelerated = result;
        }
        return result;
    }

    /**
     * Only the JVM itself can tell whether AES-GCM uses AES instructions, the CPU supporting them doesn't mean the JVM
     * does. Anything other than HotSpot reporting it is treated as an unusual environment, where XChaCha20-Poly1305 is
     * the safer choice.
     */
    private static boolean detect() {
        var flag = readHotSpotFlag();
        return Boolean.TRUE.equals(flag);
    }

    /**
     * Reads the UseAES flag through reflection, so JVMs without the HotSpot management API (OpenJ9, native
     * images, runtimes without the jdk.management module) don't fail to link this class.
     *
     * @return the flag, or null if it can't be read
     */
    public static Boolean readHotSpotFlag() {
        try {
            var beanClass = Class.forName("com.sun.management.HotSpotDiagnosticMXBean");
            var bean = Class.forName("java.lang.management.ManagementFactory")
                    .getMethod("getPlatformMXBean", Class.class)
                    .invoke(null, beanClass);
            if (bean == null) {
                return null;
            }

            var option = beanClass.getMethod("getVMOption", String.class).invoke(bean, "UseAES");
            var value = Class.forName("com.sun.management.VMOption").getMethod("getValue").invoke(option);
            return Boolean.parseBoolean(String.valueOf(value));
        } catch (ReflectiveOperationException | LinkageError | RuntimeException e) {
            return null;
        }
    }
}
