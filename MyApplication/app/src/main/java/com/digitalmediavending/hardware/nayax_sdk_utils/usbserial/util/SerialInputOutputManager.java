package com.digitalmediavending.hardware.nayax_sdk_utils.usbserial.util;

import android.util.Log;
import com.digitalmediavending.hardware.nayax_sdk_utils.usbserial.driver.UsbSerialPort;
import java.io.IOException;
import java.nio.ByteBuffer;
/* loaded from: classes.dex */
public class SerialInputOutputManager implements Runnable {
    private static final int BUFSIZ = 4096;
    private static final boolean DEBUG = true;
    private static final int READ_WAIT_MILLIS = 200;
    private static final String TAG = "SerialInputOutputManager";
    private final UsbSerialPort mDriver;
    private Listener mListener;
    private final ByteBuffer mReadBuffer;
    private State mState;
    private final ByteBuffer mWriteBuffer;

    /* loaded from: classes.dex */
    public interface Listener {
        void onNewData(byte[] data);

        void onRunError(Exception e);
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public enum State {
        STOPPED,
        RUNNING,
        STOPPING
    }

    public SerialInputOutputManager(UsbSerialPort driver) {
        this(driver, null);
    }

    public SerialInputOutputManager(UsbSerialPort driver, Listener listener) {
        this.mReadBuffer = ByteBuffer.allocate(4096);
        this.mWriteBuffer = ByteBuffer.allocate(4096);
        this.mState = State.STOPPED;
        this.mDriver = driver;
        this.mListener = listener;
    }

    public synchronized void setListener(Listener listener) {
        this.mListener = listener;
    }

    public synchronized Listener getListener() {
        return this.mListener;
    }

    public void writeAsync(byte[] data) {
        synchronized (this.mWriteBuffer) {
            this.mWriteBuffer.put(data);
        }
    }

    public synchronized void stop() {
        if (getState() == State.RUNNING) {
            Log.i(TAG, "Stop requested");
            this.mState = State.STOPPING;
        }
    }

    private synchronized State getState() {
        return this.mState;
    }

    @Override // java.lang.Runnable
    public void run() {
        synchronized (this) {
            if (getState() != State.STOPPED) {
                throw new IllegalStateException("Already running.");
            }
            this.mState = State.RUNNING;
        }
        Log.i(TAG, "Running ..");
        while (getState() == State.RUNNING) {
            try {
                try {
                    step();
                } catch (Exception e) {
                    String str = TAG;
                    Log.w(str, "Run ending due to exception: " + e.getMessage(), e);
                    Listener listener = getListener();
                    if (listener != null) {
                        listener.onRunError(e);
                    }
                    synchronized (this) {
                        this.mState = State.STOPPED;
                        Log.i(str, "Stopped.");
                        return;
                    }
                }
            } catch (Throwable th) {
                synchronized (this) {
                    this.mState = State.STOPPED;
                    Log.i(TAG, "Stopped.");
                    throw th;
                }
            }
        }
        String str2 = TAG;
        Log.i(str2, "Stopping mState=" + getState());
        synchronized (this) {
            this.mState = State.STOPPED;
            Log.i(str2, "Stopped.");
        }
    }

    private void step() throws IOException {
        int position;
        int read = this.mDriver.read(this.mReadBuffer.array(), 200);
        if (read > 0) {
            String str = TAG;
            Log.d(str, "Read data len=" + read);
            Listener listener = getListener();
            if (listener != null) {
                byte[] bArr = new byte[read];
                this.mReadBuffer.get(bArr, 0, read);
                listener.onNewData(bArr);
            }
            this.mReadBuffer.clear();
        }
        byte[] bArr2 = null;
        synchronized (this.mWriteBuffer) {
            position = this.mWriteBuffer.position();
            if (position > 0) {
                bArr2 = new byte[position];
                this.mWriteBuffer.rewind();
                this.mWriteBuffer.get(bArr2, 0, position);
                this.mWriteBuffer.clear();
            }
        }
        if (bArr2 != null) {
            String str2 = TAG;
            Log.d(str2, "Writing data len=" + position);
            this.mDriver.write(bArr2, 200);
        }
    }
}
