package com.bitmick.utils;

/**
 * Log wrapper that delegates to Android's Log class.
 * Modified from DMVI's desktop Java implementation for Android compatibility.
 */
public final class Log {
    public static final int LOG_LEVEL_DEBUG = 4;
    public static final int LOG_LEVEL_ERROR = 1;
    public static final int LOG_LEVEL_EVERYTHING = 8;
    public static final int LOG_LEVEL_WARNING = 2;

    private static int m_level = 255;
    private static String m_filter = null;

    public static void close() {
    }

    public static void initialize(Object unused) {
        // No-op for Android - uses Android's Log directly
    }

    public static void filter(String str) {
        m_filter = str;
    }

    public static void level(int i) {
        m_level = i;
    }

    public static void d(String tag, String msg) {
        if ((LOG_LEVEL_DEBUG & m_level) != 0) {
            if (m_filter == null || tag.contains(m_filter)) {
                android.util.Log.d(tag.isEmpty() ? "Marshall" : tag, msg);
            }
        }
    }

    public static void l(String tag, String msg) {
        if ((LOG_LEVEL_EVERYTHING & m_level) != 0) {
            if (m_filter == null || tag.contains(m_filter)) {
                android.util.Log.v(tag.isEmpty() ? "Marshall" : tag, msg);
            }
        }
    }

    public static void w(String tag, String msg) {
        if ((LOG_LEVEL_WARNING & m_level) != 0) {
            if (m_filter == null || tag.contains(m_filter)) {
                android.util.Log.w(tag.isEmpty() ? "Marshall" : tag, msg);
            }
        }
    }

    public static void e(String tag, String msg) {
        if ((LOG_LEVEL_ERROR & m_level) != 0) {
            if (m_filter == null || tag.contains(m_filter)) {
                android.util.Log.e(tag.isEmpty() ? "Marshall" : tag, msg);
            }
        }
    }
}
