package com.digitalmediavending.hardware.nayax_sdk_utils.usbserial.driver;

import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.util.Log;
import java.io.IOException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * FTDI Serial Driver for Nayax VPOS Touch
 *
 * The Nayax VPOS uses an internal FTDI chip (VID 0403, PID 6015).
 * This driver handles the FTDI-specific protocol including:
 * - 2-byte status header filtering on reads
 * - FTDI baud rate divisor calculation
 * - FTDI control transfers for reset and configuration
 */
public class FtdiSerialDriver implements UsbSerialDriver {
    private final UsbDevice mDevice;
    private final UsbSerialPort mPort;

    private enum DeviceType {
        TYPE_BM,
        TYPE_AM,
        TYPE_2232C,
        TYPE_R,
        TYPE_2232H,
        TYPE_4232H
    }

    public FtdiSerialDriver(UsbDevice device) {
        this.mDevice = device;
        this.mPort = new FtdiSerialPort(device, 0);
    }

    @Override
    public UsbDevice getDevice() {
        return this.mDevice;
    }

    @Override
    public List<UsbSerialPort> getPorts() {
        return Collections.singletonList(this.mPort);
    }

    private class FtdiSerialPort extends CommonUsbSerialPort {
        private static final int FTDI_DEVICE_OUT_REQTYPE = 64;
        private static final int SIO_RESET_REQUEST = 0;
        private static final int SIO_SET_BAUD_RATE_REQUEST = 3;
        private static final int SIO_SET_DATA_REQUEST = 4;
        private static final int USB_WRITE_TIMEOUT_MILLIS = 5000;

        private final String TAG = FtdiSerialDriver.class.getSimpleName();
        private DeviceType mType;

        public FtdiSerialPort(UsbDevice device, int portNumber) {
            super(device, portNumber);
        }

        @Override
        public UsbSerialDriver getDriver() {
            return FtdiSerialDriver.this;
        }

        @Override
        public boolean getCD() throws IOException { return false; }
        @Override
        public boolean getCTS() throws IOException { return false; }
        @Override
        public boolean getDSR() throws IOException { return false; }
        @Override
        public boolean getDTR() throws IOException { return false; }
        @Override
        public boolean getRI() throws IOException { return false; }
        @Override
        public boolean getRTS() throws IOException { return false; }
        @Override
        public void setDTR(boolean value) throws IOException { }
        @Override
        public void setRTS(boolean value) throws IOException { }

        /**
         * Filter out FTDI 2-byte status headers from each packet.
         * FTDI chips prepend 2 status bytes to every USB packet.
         */
        private int filterStatusBytes(byte[] src, byte[] dest, int totalBytesRead, int maxPacketSize) {
            int packetsCount = (totalBytesRead / maxPacketSize) + ((totalBytesRead % maxPacketSize) == 0 ? 0 : 1);
            int destOffset = 0;

            for (int packet = 0; packet < packetsCount; packet++) {
                int srcOffset = packet * maxPacketSize + 2; // Skip 2-byte header
                int bytesInPacket;
                if (packet == packetsCount - 1) {
                    // Last packet
                    bytesInPacket = (totalBytesRead % maxPacketSize);
                    if (bytesInPacket == 0) bytesInPacket = maxPacketSize;
                    bytesInPacket -= 2;
                } else {
                    bytesInPacket = maxPacketSize - 2;
                }

                if (bytesInPacket > 0) {
                    System.arraycopy(src, srcOffset, dest, destOffset, bytesInPacket);
                    destOffset += bytesInPacket;
                }
            }
            return destOffset;
        }

        public void reset() throws IOException {
            int result = this.mConnection.controlTransfer(FTDI_DEVICE_OUT_REQTYPE, SIO_RESET_REQUEST, 0, 0, null, 0, USB_WRITE_TIMEOUT_MILLIS);
            if (result != 0) {
                throw new IOException("Reset failed: result=" + result);
            }
            this.mType = DeviceType.TYPE_R;
            Log.d(TAG, "FTDI reset successful, type=TYPE_R");
        }

        @Override
        public void open(UsbDeviceConnection connection) throws IOException {
            if (this.mConnection != null) {
                throw new IOException("Already open");
            }
            this.mConnection = connection;

            // Claim all interfaces
            for (int i = 0; i < this.mDevice.getInterfaceCount(); i++) {
                try {
                    if (connection.claimInterface(this.mDevice.getInterface(i), true)) {
                        Log.d(TAG, "claimInterface " + i + " SUCCESS");
                    } else {
                        throw new IOException("Error claiming interface " + i);
                    }
                } catch (Throwable th) {
                    close();
                    this.mConnection = null;
                    throw th;
                }
            }
            reset();
        }

        @Override
        public void close() throws IOException {
            if (this.mConnection == null) {
                throw new IOException("Already closed");
            }
            try {
                this.mConnection.close();
            } finally {
                this.mConnection = null;
            }
        }

        @Override
        public int read(byte[] dest, int timeoutMillis) throws IOException {
            UsbEndpoint endpoint = this.mDevice.getInterface(0).getEndpoint(0);
            int maxPacketSize = endpoint.getMaxPacketSize();

            synchronized (this.mReadBufferLock) {
                int bytesRead = this.mConnection.bulkTransfer(endpoint, this.mReadBuffer,
                    Math.min(dest.length + 2, this.mReadBuffer.length), timeoutMillis);

                if (bytesRead < 2) {
                    // FTDI always returns at least 2 status bytes
                    return 0;
                }

                return filterStatusBytes(this.mReadBuffer, dest, bytesRead, maxPacketSize);
            }
        }

        @Override
        public int write(byte[] src, int timeoutMillis) throws IOException {
            UsbEndpoint endpoint = this.mDevice.getInterface(0).getEndpoint(1);
            int written = 0;

            while (written < src.length) {
                int toWrite;
                byte[] buffer;

                synchronized (this.mWriteBufferLock) {
                    toWrite = Math.min(src.length - written, this.mWriteBuffer.length);
                    if (written == 0) {
                        buffer = src;
                    } else {
                        System.arraycopy(src, written, this.mWriteBuffer, 0, toWrite);
                        buffer = this.mWriteBuffer;
                    }

                    int result = this.mConnection.bulkTransfer(endpoint, buffer, toWrite, timeoutMillis);
                    if (result <= 0) {
                        throw new IOException("Error writing " + toWrite + " bytes at offset " + written);
                    }
                    written += result;
                }
            }
            return written;
        }

        private int setBaudRate(int baudRate) throws IOException {
            long[] divisors = convertBaudrate(baudRate);
            long actualBaud = divisors[0];
            long index = divisors[1];
            long value = divisors[2];

            int result = this.mConnection.controlTransfer(FTDI_DEVICE_OUT_REQTYPE, SIO_SET_BAUD_RATE_REQUEST,
                (int) value, (int) index, null, 0, USB_WRITE_TIMEOUT_MILLIS);

            if (result != 0) {
                throw new IOException("Setting baudrate failed: result=" + result);
            }

            Log.d(TAG, "Baud rate set: requested=" + baudRate + ", actual=" + actualBaud);
            return (int) actualBaud;
        }

        @Override
        public void setParameters(int baudRate, int dataBits, int stopBits, int parity) throws IOException {
            setBaudRate(baudRate);

            int config = dataBits;

            // Parity
            switch (parity) {
                case 0: break; // None
                case 1: config |= 0x100; break; // Odd
                case 2: config |= 0x200; break; // Even
                case 3: config |= 0x300; break; // Mark
                case 4: config |= 0x400; break; // Space
                default: throw new IllegalArgumentException("Unknown parity: " + parity);
            }

            // Stop bits
            switch (stopBits) {
                case 1: break; // 1 stop bit
                case 2: config |= 0x1000; break; // 2 stop bits
                case 3: config |= 0x800; break; // 1.5 stop bits
                default: throw new IllegalArgumentException("Unknown stopBits: " + stopBits);
            }

            int result = this.mConnection.controlTransfer(FTDI_DEVICE_OUT_REQTYPE, SIO_SET_DATA_REQUEST,
                config, 0, null, 0, USB_WRITE_TIMEOUT_MILLIS);

            if (result != 0) {
                throw new IOException("Setting parameters failed: result=" + result);
            }

            Log.d(TAG, "Parameters set: " + baudRate + " baud, " + dataBits + " data bits");
        }

        /**
         * Convert baud rate to FTDI divisor values.
         * Returns [actualBaud, index, value]
         */
        private long[] convertBaudrate(int baudrate) {
            int divisor = 24000000 / baudrate;
            int[] fracCode = {0, 3, 2, 4, 1, 5, 6, 7};

            int bestDivisor = 0;
            int bestBaud = 0;
            int bestError = Integer.MAX_VALUE;

            for (int delta = 0; delta < 2; delta++) {
                int tryDivisor = divisor + delta;

                // Clamp divisor
                if (tryDivisor <= 8) tryDivisor = 8;
                else if (mType != DeviceType.TYPE_AM && tryDivisor < 12) tryDivisor = 12;
                else if (tryDivisor < 16) tryDivisor = 16;
                else if (mType != DeviceType.TYPE_AM && tryDivisor > 131071) tryDivisor = 131071;

                int actualBaud = (24000000 + tryDivisor / 2) / tryDivisor;
                int error = Math.abs(actualBaud - baudrate);

                if (error < bestError) {
                    bestError = error;
                    bestDivisor = tryDivisor;
                    bestBaud = actualBaud;
                    if (error == 0) break;
                }
            }

            long encoded = (bestDivisor >> 3) | (fracCode[bestDivisor & 7] << 14);
            if (encoded == 1) encoded = 0;
            else if (encoded == 0x4001) encoded = 1;

            long index;
            if (mType == DeviceType.TYPE_2232C || mType == DeviceType.TYPE_2232H || mType == DeviceType.TYPE_4232H) {
                index = ((encoded >> 8) & 0xFF00);
            } else {
                index = (encoded >> 16) & 0xFFFF;
            }

            return new long[]{bestBaud, index, encoded & 0xFFFF};
        }

        @Override
        public boolean purgeHwBuffers(boolean purgeRead, boolean purgeWrite) throws IOException {
            if (purgeRead) {
                int result = this.mConnection.controlTransfer(FTDI_DEVICE_OUT_REQTYPE, SIO_RESET_REQUEST, 1, 0, null, 0, USB_WRITE_TIMEOUT_MILLIS);
                if (result != 0) {
                    throw new IOException("Purge RX failed: result=" + result);
                }
            }
            if (purgeWrite) {
                int result = this.mConnection.controlTransfer(FTDI_DEVICE_OUT_REQTYPE, SIO_RESET_REQUEST, 2, 0, null, 0, USB_WRITE_TIMEOUT_MILLIS);
                if (result != 0) {
                    throw new IOException("Purge TX failed: result=" + result);
                }
            }
            return true;
        }
    }

    public static Map<Integer, int[]> getSupportedDevices() {
        LinkedHashMap<Integer, int[]> devices = new LinkedHashMap<>();
        devices.put(UsbId.VENDOR_FTDI, new int[]{UsbId.FTDI_FT232R, UsbId.FTDI_FT231X, 0x6015}); // Added 6015 for Nayax
        return devices;
    }
}
