package com.digitalmediavending.hardware.nayax_sdk_utils.usbserial.driver;

import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import java.io.IOException;
/* loaded from: classes.dex */
abstract class CommonUsbSerialPort implements UsbSerialPort {
    public static final int DEFAULT_READ_BUFFER_SIZE = 16384;
    public static final int DEFAULT_WRITE_BUFFER_SIZE = 16384;
    protected final UsbDevice mDevice;
    protected final int mPortNumber;
    protected UsbDeviceConnection mConnection = null;
    protected final Object mReadBufferLock = new Object();
    protected final Object mWriteBufferLock = new Object();
    protected byte[] mReadBuffer = new byte[16384];
    protected byte[] mWriteBuffer = new byte[16384];

    @Override // com.digitalmediavending.hardware.nayax_sdk_utils.usbserial.driver.UsbSerialPort
    public abstract void close() throws IOException;

    @Override // com.digitalmediavending.hardware.nayax_sdk_utils.usbserial.driver.UsbSerialPort
    public abstract boolean getCD() throws IOException;

    @Override // com.digitalmediavending.hardware.nayax_sdk_utils.usbserial.driver.UsbSerialPort
    public abstract boolean getCTS() throws IOException;

    @Override // com.digitalmediavending.hardware.nayax_sdk_utils.usbserial.driver.UsbSerialPort
    public abstract boolean getDSR() throws IOException;

    @Override // com.digitalmediavending.hardware.nayax_sdk_utils.usbserial.driver.UsbSerialPort
    public abstract boolean getDTR() throws IOException;

    @Override // com.digitalmediavending.hardware.nayax_sdk_utils.usbserial.driver.UsbSerialPort
    public abstract boolean getRI() throws IOException;

    @Override // com.digitalmediavending.hardware.nayax_sdk_utils.usbserial.driver.UsbSerialPort
    public abstract boolean getRTS() throws IOException;

    @Override // com.digitalmediavending.hardware.nayax_sdk_utils.usbserial.driver.UsbSerialPort
    public abstract void open(UsbDeviceConnection connection) throws IOException;

    @Override // com.digitalmediavending.hardware.nayax_sdk_utils.usbserial.driver.UsbSerialPort
    public boolean purgeHwBuffers(boolean flushReadBuffers, boolean flushWriteBuffers) throws IOException {
        return (flushReadBuffers || flushWriteBuffers) ? false : true;
    }

    @Override // com.digitalmediavending.hardware.nayax_sdk_utils.usbserial.driver.UsbSerialPort
    public abstract int read(final byte[] dest, final int timeoutMillis) throws IOException;

    @Override // com.digitalmediavending.hardware.nayax_sdk_utils.usbserial.driver.UsbSerialPort
    public abstract void setDTR(boolean value) throws IOException;

    @Override // com.digitalmediavending.hardware.nayax_sdk_utils.usbserial.driver.UsbSerialPort
    public abstract void setParameters(int baudRate, int dataBits, int stopBits, int parity) throws IOException;

    @Override // com.digitalmediavending.hardware.nayax_sdk_utils.usbserial.driver.UsbSerialPort
    public abstract void setRTS(boolean value) throws IOException;

    @Override // com.digitalmediavending.hardware.nayax_sdk_utils.usbserial.driver.UsbSerialPort
    public abstract int write(final byte[] src, final int timeoutMillis) throws IOException;

    public CommonUsbSerialPort(UsbDevice device, int portNumber) {
        this.mDevice = device;
        this.mPortNumber = portNumber;
    }

    public String toString() {
        return String.format("<%s device_name=%s device_id=%s port_number=%s>", getClass().getSimpleName(), this.mDevice.getDeviceName(), Integer.valueOf(this.mDevice.getDeviceId()), Integer.valueOf(this.mPortNumber));
    }

    public final UsbDevice getDevice() {
        return this.mDevice;
    }

    @Override // com.digitalmediavending.hardware.nayax_sdk_utils.usbserial.driver.UsbSerialPort
    public int getPortNumber() {
        return this.mPortNumber;
    }

    @Override // com.digitalmediavending.hardware.nayax_sdk_utils.usbserial.driver.UsbSerialPort
    public String getSerial() {
        return this.mConnection.getSerial();
    }

    public final void setReadBufferSize(int bufferSize) {
        synchronized (this.mReadBufferLock) {
            if (bufferSize == this.mReadBuffer.length) {
                return;
            }
            this.mReadBuffer = new byte[bufferSize];
        }
    }

    public final void setWriteBufferSize(int bufferSize) {
        synchronized (this.mWriteBufferLock) {
            if (bufferSize == this.mWriteBuffer.length) {
                return;
            }
            this.mWriteBuffer = new byte[bufferSize];
        }
    }
}
