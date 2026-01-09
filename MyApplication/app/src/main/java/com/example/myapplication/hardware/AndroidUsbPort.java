package com.example.myapplication.hardware;

import android.util.Log;
import com.bitmick.marshall.UsbSerialBridge;
import com.bitmick.marshall.drivers.serial_port_i;
import com.bitmick.utils.CircularBuffer;

/**
 * Android implementation of serial_port_i.
 * Acts as a middleman between UsbSerialBridge (Async) and Marshall SDK (Sync/Pull).
 */
public class AndroidUsbPort implements serial_port_i {
    private static final String TAG = "AndroidUsbPort";
    private final UsbSerialBridge mDriver;
    private final CircularBuffer mRxBuffer;

    public AndroidUsbPort(UsbSerialBridge driver) {
        this.mDriver = driver;
        this.mRxBuffer = new CircularBuffer(16384); // Match DMVI buffer size

        // FIX 3: Register callback to bridge Async USB reads into our Circular Buffer
        this.mDriver.setDataCallback(data -> {
            mRxBuffer.write(data);
        });
    }

    @Override
    public int available() {
        return mRxBuffer.size();
    }

    @Override
    public byte read_byte() {
        return mRxBuffer.read();
    }

    @Override
    public int read(byte[] buffer, int length) {
        return mRxBuffer.read(buffer, length);
    }

    @Override
    public void write(byte[] data) {
        if (mDriver != null) {
            mDriver.transmit(data, data.length);
        }
    }

    @Override
    public void close() {
        if (mDriver != null) {
            mDriver.stop();
        }
    }

    @Override
    public void flush() {
        mRxBuffer.reset();
    }
}

