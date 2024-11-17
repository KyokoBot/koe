package moe.kyokobot.koe.dave.util;

public class MLSUtil {
    /**
     * Turns a long into a byte array containing a big endian u64.
     * @param value The long to convert.
     * @return The byte array.
     */
    public static byte[] bigEndianBytesFrom(long value) {
        byte[] bytes = new byte[8];
        for (int i = 7; i >= 0; i--) {
            bytes[i] = (byte) (value & 0xFF);
            value >>= 8;
        }
        return bytes;
    }

    /**
     * Turns a byte array containing a big endian u64 into a long.
     * @param bytes The byte array to convert.
     * @return The long.
     */
    public static long fromBigEndianBytes(byte[] bytes) {
        long value = 0;
        for (int i = 0; i < 8; i++) {
            value <<= 8;
            value |= bytes[i] & 0xFF;
        }
        return value;
    }

    /**
     * Turns a String containing an unsigned long into a byte array containing a little endian u64.
     * @param value The string to convert.
     * @return The byte array.
     */
    public static byte[] littleEndianBytesFromString(String value) {
        long longValue = Long.parseUnsignedLong(value);
        byte[] bytes = new byte[8];
        for (int i = 0; i < 8; i++) {
            bytes[i] = (byte) (longValue & 0xFF);
            longValue >>= 8;
        }
        return bytes;
    }
}
