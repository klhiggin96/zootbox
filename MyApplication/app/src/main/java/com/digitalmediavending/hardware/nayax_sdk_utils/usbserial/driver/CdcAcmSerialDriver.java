package com.digitalmediavending.hardware.nayax_sdk_utils.usbserial.driver;

import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbRequest;
import android.os.Build;
import android.util.Log;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
/* loaded from: classes.dex */
public class CdcAcmSerialDriver implements UsbSerialDriver {
    private final String TAG = CdcAcmSerialDriver.class.getSimpleName();
    private final UsbDevice mDevice;
    private final UsbSerialPort mPort;

    public CdcAcmSerialDriver(UsbDevice device) {
        this.mDevice = device;
        this.mPort = new CdcAcmSerialPort(device, 0);
    }

    @Override // com.digitalmediavending.hardware.nayax_sdk_utils.usbserial.driver.UsbSerialDriver
    public UsbDevice getDevice() {
        return this.mDevice;
    }

    @Override // com.digitalmediavending.hardware.nayax_sdk_utils.usbserial.driver.UsbSerialDriver
    public List<UsbSerialPort> getPorts() {
        return Collections.singletonList(this.mPort);
    }

    /* loaded from: classes.dex */
    class CdcAcmSerialPort extends CommonUsbSerialPort {
        private static final int GET_LINE_CODING = 33;
        private static final int SEND_BREAK = 35;
        private static final int SET_CONTROL_LINE_STATE = 34;
        private static final int SET_LINE_CODING = 32;
        private static final int USB_RECIP_INTERFACE = 1;
        private static final int USB_RT_ACM = 33;
        private UsbEndpoint mControlEndpoint;
        private UsbInterface mControlInterface;
        private UsbInterface mDataInterface;
        private boolean mDtr;
        private final boolean mEnableAsyncReads;
        private UsbEndpoint mReadEndpoint;
        private boolean mRts;
        private UsbEndpoint mWriteEndpoint;

        @Override // com.digitalmediavending.hardware.nayax_sdk_utils.usbserial.driver.CommonUsbSerialPort, com.digitalmediavending.hardware.nayax_sdk_utils.usbserial.driver.UsbSerialPort
        public boolean getCD() throws IOException {
            return false;
        }

        @Override // com.digitalmediavending.hardware.nayax_sdk_utils.usbserial.driver.CommonUsbSerialPort, com.digitalmediavending.hardware.nayax_sdk_utils.usbserial.driver.UsbSerialPort
        public boolean getCTS() throws IOException {
            return false;
        }

        @Override // com.digitalmediavending.hardware.nayax_sdk_utils.usbserial.driver.CommonUsbSerialPort, com.digitalmediavending.hardware.nayax_sdk_utils.usbserial.driver.UsbSerialPort
        public boolean getDSR() throws IOException {
            return false;
        }

        @Override // com.digitalmediavending.hardware.nayax_sdk_utils.usbserial.driver.CommonUsbSerialPort, com.digitalmediavending.hardware.nayax_sdk_utils.usbserial.driver.UsbSerialPort
        public boolean getRI() throws IOException {
            return false;
        }

        public CdcAcmSerialPort(UsbDevice device, int portNumber) {
            super(device, portNumber);
            this.mRts = false;
            this.mDtr = false;
            this.mEnableAsyncReads = Build.VERSION.SDK_INT >= 16;
        }

        @Override // com.digitalmediavending.hardware.nayax_sdk_utils.usbserial.driver.UsbSerialPort
        public UsbSerialDriver getDriver() {
            return CdcAcmSerialDriver.this;
        }

        @Override // com.digitalmediavending.hardware.nayax_sdk_utils.usbserial.driver.CommonUsbSerialPort, com.digitalmediavending.hardware.nayax_sdk_utils.usbserial.driver.UsbSerialPort
        public void open(UsbDeviceConnection connection) throws IOException {
            if (this.mConnection != null) {
                throw new IOException("Already open");
            }
            this.mConnection = connection;
            try {
                if (1 == this.mDevice.getInterfaceCount()) {
                    Log.d(CdcAcmSerialDriver.this.TAG, "device might be castrated ACM device, trying single interface logic");
                    openSingleInterface();
                } else {
                    Log.d(CdcAcmSerialDriver.this.TAG, "trying default interface logic");
                    openInterface();
                }
                if (this.mEnableAsyncReads) {
                    Log.d(CdcAcmSerialDriver.this.TAG, "Async reads enabled");
                } else {
                    Log.d(CdcAcmSerialDriver.this.TAG, "Async reads disabled.");
                }
            } catch (Throwable th) {
                this.mConnection = null;
                this.mControlEndpoint = null;
                this.mReadEndpoint = null;
                this.mWriteEndpoint = null;
                throw th;
            }
        }

        private void openSingleInterface() throws IOException {
            int i = 0;
            this.mControlInterface = this.mDevice.getInterface(0);
            String str = CdcAcmSerialDriver.this.TAG;
            Log.d(str, "Control iface=" + this.mControlInterface);
            this.mDataInterface = this.mDevice.getInterface(0);
            String str2 = CdcAcmSerialDriver.this.TAG;
            Log.d(str2, "data iface=" + this.mDataInterface);
            if (!this.mConnection.claimInterface(this.mControlInterface, true)) {
                throw new IOException("Could not claim shared control/data interface.");
            }
            int endpointCount = this.mControlInterface.getEndpointCount();
            if (endpointCount < 3) {
                String str3 = CdcAcmSerialDriver.this.TAG;
                Log.d(str3, "not enough endpoints - need 3. count=" + this.mControlInterface.getEndpointCount());
                throw new IOException("Insufficient number of endpoints(" + this.mControlInterface.getEndpointCount() + ")");
            }
            this.mControlEndpoint = null;
            this.mReadEndpoint = null;
            this.mWriteEndpoint = null;
            while (true) {
                if (i >= endpointCount) {
                    break;
                }
                UsbEndpoint endpoint = this.mControlInterface.getEndpoint(i);
                if (endpoint.getDirection() == 128 && endpoint.getType() == 3) {
                    Log.d(CdcAcmSerialDriver.this.TAG, "Found controlling endpoint");
                    this.mControlEndpoint = endpoint;
                } else if (endpoint.getDirection() == 128 && endpoint.getType() == 2) {
                    Log.d(CdcAcmSerialDriver.this.TAG, "Found reading endpoint");
                    this.mReadEndpoint = endpoint;
                } else if (endpoint.getDirection() == 0 && endpoint.getType() == 2) {
                    Log.d(CdcAcmSerialDriver.this.TAG, "Found writing endpoint");
                    this.mWriteEndpoint = endpoint;
                }
                if (this.mControlEndpoint != null && this.mReadEndpoint != null && this.mWriteEndpoint != null) {
                    Log.d(CdcAcmSerialDriver.this.TAG, "Found all required endpoints");
                    break;
                }
                i++;
            }
            if (this.mControlEndpoint == null || this.mReadEndpoint == null || this.mWriteEndpoint == null) {
                Log.d(CdcAcmSerialDriver.this.TAG, "Could not establish all endpoints");
                throw new IOException("Could not establish all endpoints");
            }
        }

        private void openInterface() throws IOException {
            String str = CdcAcmSerialDriver.this.TAG;
            Log.d(str, "claiming interfaces, count=" + this.mDevice.getInterfaceCount());
            this.mControlInterface = this.mDevice.getInterface(0);
            String str2 = CdcAcmSerialDriver.this.TAG;
            Log.d(str2, "Control iface=" + this.mControlInterface);
            if (!this.mConnection.claimInterface(this.mControlInterface, true)) {
                throw new IOException("Could not claim control interface.");
            }
            this.mControlEndpoint = this.mControlInterface.getEndpoint(0);
            String str3 = CdcAcmSerialDriver.this.TAG;
            Log.d(str3, "Control endpoint direction: " + this.mControlEndpoint.getDirection());
            Log.d(CdcAcmSerialDriver.this.TAG, "Claiming data interface.");
            this.mDataInterface = this.mDevice.getInterface(1);
            String str4 = CdcAcmSerialDriver.this.TAG;
            Log.d(str4, "data iface=" + this.mDataInterface);
            if (!this.mConnection.claimInterface(this.mDataInterface, true)) {
                throw new IOException("Could not claim data interface.");
            }
            // FIX: Properly detect endpoint directions instead of assuming indices
            // IN endpoint (direction 0x80/128) = Read, OUT endpoint (direction 0) = Write
            UsbEndpoint ep0 = this.mDataInterface.getEndpoint(0);
            UsbEndpoint ep1 = this.mDataInterface.getEndpoint(1);

            if (ep0.getDirection() == 128) { // IN = 0x80 = 128
                this.mReadEndpoint = ep0;
                this.mWriteEndpoint = ep1;
            } else {
                this.mReadEndpoint = ep1;
                this.mWriteEndpoint = ep0;
            }

            String str5 = CdcAcmSerialDriver.this.TAG;
            Log.d(str5, "Read endpoint: addr=0x" + Integer.toHexString(this.mReadEndpoint.getAddress()) + ", direction: " + this.mReadEndpoint.getDirection());
            String str6 = CdcAcmSerialDriver.this.TAG;
            Log.d(str6, "Write endpoint: addr=0x" + Integer.toHexString(this.mWriteEndpoint.getAddress()) + ", direction: " + this.mWriteEndpoint.getDirection());

            // FIX 1: Assert DTR and RTS High to signal host readiness to VPOS Touch
            try {
                setDTR(true);
                setRTS(true);
                Log.i(CdcAcmSerialDriver.this.TAG, "DTR and RTS asserted High");
            } catch (IOException e) {
                Log.e(CdcAcmSerialDriver.this.TAG, "Failed to assert control lines: " + e.getMessage());
            }
        }

        private int sendAcmControlMessage(int request, int value, byte[] buf) {
            return this.mConnection.controlTransfer(33, request, value, 0, buf, buf != null ? buf.length : 0, 5000);
        }

        @Override // com.digitalmediavending.hardware.nayax_sdk_utils.usbserial.driver.CommonUsbSerialPort, com.digitalmediavending.hardware.nayax_sdk_utils.usbserial.driver.UsbSerialPort
        public void close() throws IOException {
            if (this.mConnection == null) {
                throw new IOException("Already closed");
            }
            this.mConnection.close();
            this.mConnection = null;
        }

        @Override // com.digitalmediavending.hardware.nayax_sdk_utils.usbserial.driver.CommonUsbSerialPort, com.digitalmediavending.hardware.nayax_sdk_utils.usbserial.driver.UsbSerialPort
        public int read(byte[] dest, int timeoutMillis) throws IOException {
            if (this.mEnableAsyncReads) {
                UsbRequest usbRequest = new UsbRequest();
                try {
                    usbRequest.initialize(this.mConnection, this.mReadEndpoint);
                    ByteBuffer wrap = ByteBuffer.wrap(dest);
                    if (!usbRequest.queue(wrap, dest.length)) {
                        throw new IOException("Error queueing request.");
                    }
                    if (this.mConnection.requestWait() == null) {
                        throw new IOException("Null response");
                    }
                    int position = wrap.position();
                    if (position > 0) {
                        return position;
                    }
                    return 0;
                } finally {
                    usbRequest.close();
                }
            }
            synchronized (this.mReadBufferLock) {
                int bulkTransfer = this.mConnection.bulkTransfer(this.mReadEndpoint, this.mReadBuffer, Math.min(dest.length, this.mReadBuffer.length), timeoutMillis);
                if (bulkTransfer < 0) {
                    return timeoutMillis == Integer.MAX_VALUE ? -1 : 0;
                }
                System.arraycopy(this.mReadBuffer, 0, dest, 0, bulkTransfer);
                return bulkTransfer;
            }
        }

        @Override // com.digitalmediavending.hardware.nayax_sdk_utils.usbserial.driver.CommonUsbSerialPort, com.digitalmediavending.hardware.nayax_sdk_utils.usbserial.driver.UsbSerialPort
        public int write(byte[] src, int timeoutMillis) throws IOException {
            int min;
            byte[] bArr;
            int bulkTransfer;
            int i = 0;
            while (i < src.length) {
                synchronized (this.mWriteBufferLock) {
                    min = Math.min(src.length - i, this.mWriteBuffer.length);
                    if (i == 0) {
                        bArr = src;
                    } else {
                        System.arraycopy(src, i, this.mWriteBuffer, 0, min);
                        bArr = this.mWriteBuffer;
                    }
                    bulkTransfer = this.mConnection.bulkTransfer(this.mWriteEndpoint, bArr, min, timeoutMillis);
                }
                if (bulkTransfer > 0) {
                    String str = CdcAcmSerialDriver.this.TAG;
                    Log.d(str, "Wrote amt=" + bulkTransfer + " attempted=" + min);
                    i += bulkTransfer;
                } else {
                    throw new IOException("Error writing " + min + " bytes at offset " + i + " length=" + src.length);
                }
            }
            return i;
        }

        @Override // com.digitalmediavending.hardware.nayax_sdk_utils.usbserial.driver.CommonUsbSerialPort, com.digitalmediavending.hardware.nayax_sdk_utils.usbserial.driver.UsbSerialPort
        public void setParameters(int baudRate, int dataBits, int stopBits, int parity) {
            byte b;
            byte b2;
            if (stopBits == 1) {
                b = 0;
            } else if (stopBits == 2) {
                b = 2;
            } else if (stopBits != 3) {
                throw new IllegalArgumentException("Bad value for stopBits: " + stopBits);
            } else {
                b = 1;
            }
            if (parity == 0) {
                b2 = 0;
            } else if (parity == 1) {
                b2 = 1;
            } else if (parity == 2) {
                b2 = 2;
            } else if (parity == 3) {
                b2 = 3;
            } else if (parity != 4) {
                throw new IllegalArgumentException("Bad value for parity: " + parity);
            } else {
                b2 = 4;
            }
            byte[] msg = new byte[]{(byte) (baudRate & 255), (byte) ((baudRate >> 8) & 255), (byte) ((baudRate >> 16) & 255), (byte) ((baudRate >> 24) & 255), b, b2, (byte) dataBits};
            int result = sendAcmControlMessage(32, 0, msg);
            Log.i(CdcAcmSerialDriver.this.TAG, "SET_LINE_CODING: baud=" + baudRate + ", dataBits=" + dataBits + ", stopBits=" + stopBits + ", parity=" + parity + ", result=" + result);
        }

        @Override // com.digitalmediavending.hardware.nayax_sdk_utils.usbserial.driver.CommonUsbSerialPort, com.digitalmediavending.hardware.nayax_sdk_utils.usbserial.driver.UsbSerialPort
        public boolean getDTR() throws IOException {
            return this.mDtr;
        }

        @Override // com.digitalmediavending.hardware.nayax_sdk_utils.usbserial.driver.CommonUsbSerialPort, com.digitalmediavending.hardware.nayax_sdk_utils.usbserial.driver.UsbSerialPort
        public void setDTR(boolean value) throws IOException {
            this.mDtr = value;
            setDtrRts();
        }

        @Override // com.digitalmediavending.hardware.nayax_sdk_utils.usbserial.driver.CommonUsbSerialPort, com.digitalmediavending.hardware.nayax_sdk_utils.usbserial.driver.UsbSerialPort
        public boolean getRTS() throws IOException {
            return this.mRts;
        }

        @Override // com.digitalmediavending.hardware.nayax_sdk_utils.usbserial.driver.CommonUsbSerialPort, com.digitalmediavending.hardware.nayax_sdk_utils.usbserial.driver.UsbSerialPort
        public void setRTS(boolean value) throws IOException {
            this.mRts = value;
            setDtrRts();
        }

        private void setDtrRts() {
            sendAcmControlMessage(34, (this.mRts ? 2 : 0) | (this.mDtr ? 1 : 0), null);
        }
    }

    public static Map<Integer, int[]> getSupportedDevices() {
        LinkedHashMap linkedHashMap = new LinkedHashMap();
        linkedHashMap.put(Integer.valueOf((int) UsbId.VENDOR_ARDUINO), new int[]{1, 67, 16, 66, 59, 68, 63, 68, UsbId.ARDUINO_LEONARDO, UsbId.ARDUINO_MICRO});
        linkedHashMap.put(Integer.valueOf((int) UsbId.VENDOR_VAN_OOIJEN_TECH), new int[]{UsbId.VAN_OOIJEN_TECH_TEENSYDUINO_SERIAL});
        linkedHashMap.put(1003, new int[]{UsbId.ATMEL_LUFA_CDC_DEMO_APP});
        linkedHashMap.put(Integer.valueOf((int) UsbId.VENDOR_LEAFLABS), new int[]{4});
        return linkedHashMap;
    }
}
