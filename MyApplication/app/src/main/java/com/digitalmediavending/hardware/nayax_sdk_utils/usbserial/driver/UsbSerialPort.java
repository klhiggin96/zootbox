package com.digitalmediavending.hardware.nayax_sdk_utils.usbserial.driver;

import android.hardware.usb.UsbDeviceConnection;
import java.io.IOException;
/* loaded from: classes.dex */
public interface UsbSerialPort {
    public static final int DATABITS_5 = 5;
    public static final int DATABITS_6 = 6;
    public static final int DATABITS_7 = 7;
    public static final int DATABITS_8 = 8;
    public static final int FLOWCONTROL_NONE = 0;
    public static final int FLOWCONTROL_RTSCTS_IN = 1;
    public static final int FLOWCONTROL_RTSCTS_OUT = 2;
    public static final int FLOWCONTROL_XONXOFF_IN = 4;
    public static final int FLOWCONTROL_XONXOFF_OUT = 8;
    public static final int PARITY_EVEN = 2;
    public static final int PARITY_MARK = 3;
    public static final int PARITY_NONE = 0;
    public static final int PARITY_ODD = 1;
    public static final int PARITY_SPACE = 4;
    public static final int STOPBITS_1 = 1;
    public static final int STOPBITS_1_5 = 3;
    public static final int STOPBITS_2 = 2;

    void close() throws IOException;

    boolean getCD() throws IOException;

    boolean getCTS() throws IOException;

    boolean getDSR() throws IOException;

    boolean getDTR() throws IOException;

    UsbSerialDriver getDriver();

    int getPortNumber();

    boolean getRI() throws IOException;

    boolean getRTS() throws IOException;

    String getSerial();

    void open(UsbDeviceConnection connection) throws IOException;

    boolean purgeHwBuffers(boolean flushRX, boolean flushTX) throws IOException;

    int read(final byte[] dest, final int timeoutMillis) throws IOException;

    void setDTR(boolean value) throws IOException;

    void setParameters(int baudRate, int dataBits, int stopBits, int parity) throws IOException;

    void setRTS(boolean value) throws IOException;

    int write(final byte[] src, final int timeoutMillis) throws IOException;
}
