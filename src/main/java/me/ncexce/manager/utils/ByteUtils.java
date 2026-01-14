package me.ncexce.manager.utils;

public class ByteUtils {
    private static final char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();

    public static String bytesToHex(byte[] bytes) {
        if (bytes == null) return null;
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF; // 取低8位，防止符号位扩展
            hexChars[j * 2] = HEX_ARRAY[v >>> 4]; // 高4位
            hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F]; // 低4位
        }
        return new String(hexChars);
    }
}