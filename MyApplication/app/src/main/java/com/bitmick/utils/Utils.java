package com.bitmick.utils;
/* loaded from: classes.dex */
public final class Utils {
    private static final String TAG = "Utils";

    public static void clearPacket(byte[] bArr, int i, int i2) {
        for (int i3 = 0; i3 < i2; i3++) {
            bArr[i + i3] = 0;
        }
    }

    public static void dumpPacket(String str, byte[] bArr, int i, int i2) {
        StringBuilder sb = new StringBuilder();
        sb.append(str);
        sb.append(":\n\t");
        for (int i3 = 0; i3 < i2; i3++) {
            if (i3 % 16 == 0 && i3 > 0) {
                sb.append("\n\t");
            }
            sb.append(String.format("%02x:", Byte.valueOf(bArr[i + i3])));
        }
        Log.d("", sb.toString());
    }

    public static long currentTimeMillis() {
        return System.currentTimeMillis();
    }

    public static void threadSleep(int i) {
        try {
            Thread.sleep(i);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
