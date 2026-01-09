package com.bitmick.utils;
/* loaded from: classes.dex */
public class StringUtils {
    public static final String TAG = "StringUtils";
    private static final char[] hex_array = "0123456789ABCDEF".toCharArray();

    public static String byteArray2String(byte[] bArr) {
        int i = 0;
        while (true) {
            if (i >= bArr.length) {
                i = -1;
                break;
            } else if (bArr[i] == 0) {
                break;
            } else {
                i++;
            }
        }
        if (i > 0) {
            return new String(bArr, 0, i);
        }
        return null;
    }

    public static int atoi(String str, int i) {
        double d;
        if (str.charAt(i) == '-') {
            d = -1.0d;
            i++;
        } else {
            d = 0.0d;
        }
        boolean z = false;
        do {
            char charAt = str.charAt(i);
            if (Character.isDigit(charAt)) {
                if (!z) {
                    d = (d * 10.0d) + (charAt - '0');
                }
            } else if (z || charAt != '.') {
                break;
            } else {
                z = true;
            }
            i++;
        } while (i < str.length());
        return (int) (d * 1);
    }

    public static String int2Str(int i) {
        return String.valueOf(i);
    }

    public static String float2Str(float f) {
        return String.valueOf(f);
    }

    public static String padRight(String str, int i) {
        return String.format("%1$-" + i + "s", str);
    }

    public static String padLeft(String str, int i) {
        return String.format("%1$" + i + "s", str);
    }

    public static String paddingStringRight(String str, int i) {
        return String.format("%1$-" + i + "s", str);
    }

    public static int string2Int(String str) {
        try {
            return Integer.parseInt(str);
        } catch (Exception unused) {
            return 0;
        }
    }

    public static String buf2hex_str(byte[] bArr) {
        char[] cArr = new char[bArr.length * 2];
        int i = 0;
        for (byte b : bArr) {
            int i2 = b & 255;
            int i3 = i + 1;
            char[] cArr2 = hex_array;
            cArr[i] = cArr2[(byte) (i2 >>> 4)];
            i = i3 + 1;
            cArr[i3] = cArr2[(byte) (i2 & 15)];
        }
        return new String(cArr);
    }

    public static byte[] hex_str2buf(String str) {
        int length = str.length() / 2;
        byte[] bArr = new byte[length];
        for (int i = 0; i < length; i++) {
            int i2 = i * 2;
            bArr[i] = (byte) Integer.parseInt(str.substring(i2, i2 + 2), 16);
        }
        return bArr;
    }
}
