package com.bitmick.utils;
/* loaded from: classes.dex */
public final class ByteArrayUtils {
    public static final String TAG = "ByteArrayUtils";

    public static int byteArrToInteger(byte[] bArr, int i) {
        return (bArr[i + 0] & 255) + ((bArr[i + 1] & 255) << 8) + ((bArr[i + 2] & 255) << 16) + ((bArr[i + 3] & 255) << 24);
    }

    public static long byteArrToLong(byte[] bArr, int i) {
        return (bArr[i + 0] & 255) + ((bArr[i + 1] & 255) << 8) + ((bArr[i + 2] & 255) << 16) + ((bArr[i + 3] & 255) << 24) + ((bArr[i + 4] & 255) << 32) + ((bArr[i + 5] & 255) << 40) + ((bArr[i + 6] & 255) << 48) + ((bArr[i + 7] & 255) << 56);
    }

    public static short byteArrToShort(byte[] bArr, int i) {
        return (short) (((short) (bArr[i + 0] & 255)) + ((bArr[i + 1] & 255) << 8));
    }

    public static byte byteArrToByte(byte[] bArr, int i) {
        return bArr[i];
    }

    public static int byteToByteArray(byte[] bArr, int i, byte b) {
        bArr[i + 0] = b;
        return 1;
    }

    public static int shortToByteArray(byte[] bArr, int i, short s) {
        bArr[i + 0] = (byte) s;
        bArr[i + 1] = (byte) (s >>> 8);
        return 2;
    }

    public static int intToByteArray(byte[] bArr, int i, int i2) {
        bArr[i + 0] = (byte) (i2 >> 0);
        bArr[i + 1] = (byte) (i2 >> 8);
        bArr[i + 2] = (byte) (i2 >> 16);
        bArr[i + 3] = (byte) (i2 >> 24);
        return 4;
    }

    public static int longToByteArray(byte[] bArr, int i, long j) {
        bArr[i + 0] = (byte) (j >> 0);
        bArr[i + 1] = (byte) (j >> 8);
        bArr[i + 2] = (byte) (j >> 16);
        bArr[i + 3] = (byte) (j >> 24);
        bArr[i + 4] = (byte) (j >> 32);
        bArr[i + 5] = (byte) (j >> 40);
        bArr[i + 6] = (byte) (j >> 48);
        bArr[i + 7] = (byte) (j >> 56);
        return 8;
    }

    public static int stringToByteArray(byte[] bArr, int i, String str, int i2) {
        try {
            byte[] bytes = str.getBytes("ASCII");
            if (bytes.length < i2) {
                int length = bytes.length;
            }
            int i3 = 0;
            while (i3 < bytes.length) {
                bArr[i] = bytes[i3];
                i3++;
                i++;
            }
            return bytes.length;
        } catch (Exception unused) {
            return -1;
        }
    }

    public static String byteArrToString(byte[] bArr, int i, int i2) {
        for (int i3 = i; i3 < i + i2; i3++) {
            if (bArr[i3] == 0 || bArr[i3] == -1) {
                i2 = i3;
                break;
            }
        }
        return new String(bArr, i, i2 - i);
    }

    public static int subByteArrToByteArray(byte[] bArr, int i, byte[] bArr2) {
        try {
            System.arraycopy(bArr2, 0, bArr, i, bArr2.length);
        } catch (Exception unused) {
        }
        return bArr2.length;
    }

    public static int byteArrToSubByteArray(byte[] bArr, int i, byte[] bArr2) {
        try {
            System.arraycopy(bArr, i, bArr2, 0, bArr2.length);
        } catch (Exception unused) {
        }
        return bArr2.length;
    }
}
