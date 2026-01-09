package com.bitmick.marshall;

import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.util.Log;

import com.bitmick.marshall.interfaces.lowlevel_i;
import com.digitalmediavending.hardware.nayax_sdk_utils.usbserial.driver.UsbSerialPort;

import java.io.IOException;
import java.util.Arrays;

/**
 * USB Serial Bridge implementing lowlevel_i interface for Marshall SDK.
 *
 * This implementation matches DMVI's lowlevel_serial_ftdi.java exactly:
 * - 16384 byte RX buffer with circular head/tail pointers
 * - 1000ms read timeout, 0ms write timeout
 * - Packet framing with 2-byte little-endian length prefix
 * - Thread priority 10 (MAX_PRIORITY)
 * - Port must be opened BEFORE passing to init()
 */
public class UsbSerialBridge implements lowlevel_i {
    private static final String TAG = "UsbSerialBridge";

    // Buffer sizes matching DMVI lowlevel_serial_ftdi.java
    private static final int RX_BUFFER_SIZE = 16384;    // DMVI line 134
    private static final int TEMP_BUFFER_SIZE = 1024;   // DMVI line 94

    // Timeouts matching DMVI
    private static final int READ_TIMEOUT_MS = 1000;    // DMVI line 117
    private static final int WRITE_TIMEOUT_MS = 0;      // DMVI line 77 (non-blocking)

    // Marshall protocol packet size limits (DMVI line 148)
    private static final int MIN_PACKET_SIZE = 9;
    private static final int MAX_PACKET_SIZE = 512;

    private final UsbManager usbManager;
    private final UsbDevice usbDevice;

    private UsbSerialPort serialPort;
    private UsbDeviceConnection connection;
    private link_events_t linkEvents;
    private Thread readThread;
    private volatile boolean running = false;

    // FIX 2: Callback for raw data to bridge Async USB with pull-based serial_port_i
    public interface DataCallback {
        void onDataReceived(byte[] data);
    }
    private DataCallback dataCallback;

    public void setDataCallback(DataCallback callback) {
        this.dataCallback = callback;
    }

    // Baud rate stored in init(), applied in start()
    private int baudRate = 115200;

    // Circular buffer for packet framing (DMVI pattern)
    private byte[] rxBuffer;
    private byte[] tempBuffer;
    private int head = 0;
    private int tail = 0;

    public UsbSerialBridge(UsbManager usbManager, UsbDevice usbDevice) {
        this.usbManager = usbManager;
        this.usbDevice = usbDevice;
    }

    /**
     * Initialize the bridge with an already-open serial port.
     * DMVI pattern: port is opened externally and passed in.
     * setParameters() is NOT called here - it's called in start().
     */
    @Override
    public void init(Object port, Object baudRateParam) {
        Log.d(TAG, "Initializing USB serial bridge (DMVI pattern)");

        // DMVI line 28: receive already-open UsbSerialPort
        if (port instanceof UsbSerialPort) {
            this.serialPort = (UsbSerialPort) port;
            Log.d(TAG, "Received already-open UsbSerialPort");
        } else {
            Log.w(TAG, "Port not provided, will need to be set before start()");
        }

        // Store baud rate for start() - DMVI line 29
        this.baudRate = (baudRateParam instanceof Integer) ? (Integer) baudRateParam : 115200;

        // Initialize buffers - DMVI pattern
        rxBuffer = new byte[RX_BUFFER_SIZE];
        tempBuffer = new byte[TEMP_BUFFER_SIZE];
        head = 0;
        tail = 0;

        Log.i(TAG, "USB serial bridge initialized, baud=" + baudRate + " (will be set on start)");
    }

    /**
     * Set the serial port directly (for cases where port is opened separately)
     */
    public void setSerialPort(UsbSerialPort port, UsbDeviceConnection conn) {
        this.serialPort = port;
        this.connection = conn;

        // Initialize buffers if not already done (when init() wasn't called)
        if (rxBuffer == null) {
            rxBuffer = new byte[RX_BUFFER_SIZE];
            tempBuffer = new byte[TEMP_BUFFER_SIZE];
            head = 0;
            tail = 0;
        }

        Log.d(TAG, "Serial port set directly");
    }

    @Override
    public void register_link_events(link_events_t events) {
        this.linkEvents = events;
    }

    /**
     * Start the bridge - sets serial parameters and starts read thread.
     * DMVI pattern: setParameters() is called HERE, not in init().
     */
    @Override
    public void start() {
        if (serialPort == null) {
            Log.e(TAG, "Cannot start - serial port not initialized");
            return;
        }

        // DMVI line 43: setParameters() called in start(), not init()
        try {
            serialPort.setParameters(
                baudRate,
                8,                            // Data bits
                UsbSerialPort.STOPBITS_1,     // Stop bits
                UsbSerialPort.PARITY_NONE     // Parity
            );
            Log.d(TAG, "Serial parameters set: " + baudRate + " bps, 8N1");
        } catch (IOException e) {
            Log.e(TAG, "Failed to set serial parameters", e);
            return;
        }

        // Reset buffer state
        resetBuffer();

        // Start read thread with MAX_PRIORITY - DMVI line 55
        running = true;
        readThread = new Thread(this::readLoop, "UsbSerialBridge-Read");
        readThread.setPriority(Thread.MAX_PRIORITY);  // Priority 10
        readThread.start();

        Log.i(TAG, "USB serial bridge started (priority=" + Thread.MAX_PRIORITY + ")");
    }

    @Override
    public void stop() {
        Log.d(TAG, "Stopping USB serial bridge");
        running = false;

        if (readThread != null) {
            readThread.interrupt();
            try {
                readThread.join(1000);
            } catch (InterruptedException e) {
                // Ignore
            }
            readThread = null;
        }

        try {
            if (serialPort != null) {
                serialPort.close();
                serialPort = null;
            }
            if (connection != null) {
                connection.close();
                connection = null;
            }
        } catch (IOException e) {
            Log.e(TAG, "Error closing serial port", e);
        }

        Log.i(TAG, "USB serial bridge stopped");
    }

    @Override
    public void reset() {
        resetBuffer();
        Log.d(TAG, "Buffer reset");
    }

    /**
     * Transmit data with 0ms timeout (non-blocking).
     * DMVI line 77: write(bArr, 0) - timeout is 0
     */
    @Override
    public boolean transmit(byte[] data, int length) {
        if (serialPort == null) {
            Log.e(TAG, "Cannot transmit - serial port not open");
            return false;
        }

        try {
            byte[] toSend = Arrays.copyOf(data, length);
            serialPort.write(toSend, WRITE_TIMEOUT_MS);  // 0ms = non-blocking
            logData("TX", toSend, length);
            return true;
        } catch (IOException e) {
            Log.e(TAG, "Failed to transmit: " + e.getMessage());
            return false;
        }
    }

    @Override
    public void onLinkTimerTick(long timestamp) {
        // No action needed - read loop handles everything
    }

    /**
     * Read loop with packet framing - matches DMVI's ClientAltHandlingRunnable.run()
     * Key differences from old implementation:
     * - Uses 16384 byte circular buffer
     * - 1000ms read timeout
     * - Parses 2-byte length prefix before dispatching
     */
    private void readLoop() {
        Log.d(TAG, "Read loop started with packet framing (DMVI pattern)");
        int readCount = 0;
        int zeroReadCount = 0;

        while (running && serialPort != null) {
            try {
                // Read into temp buffer - DMVI lines 111-127
                int bytesRead;
                synchronized (serialPort) {
                    int maxRead = Math.min(TEMP_BUFFER_SIZE, RX_BUFFER_SIZE - head);
                    if (maxRead <= 0) {
                        resetBuffer();
                        continue;
                    }
                    bytesRead = serialPort.read(tempBuffer, READ_TIMEOUT_MS);
                }

                readCount++;

                // Handle read result - DMVI lines 138-140
                if (bytesRead < 0) {
                    Log.e(TAG, "Serial read error (bytesRead=" + bytesRead + "), stopping");
                    running = false;
                    break;
                }

                if (bytesRead == 0) {
                    zeroReadCount++;
                    // Log every 10 timeouts to show we're alive
                    if (zeroReadCount % 10 == 0) {
                        Log.d(TAG, "Read loop alive: " + readCount + " reads, " + zeroReadCount + " timeouts, head=" + head + ", tail=" + tail);
                    }
                    // No data - sleep if buffer empty (DMVI line 163)
                    if (head == tail) {
                        try {
                            Thread.sleep(1);
                        } catch (InterruptedException e) {
                            break;
                        }
                    }
                    continue;
                }

                // We got data!
                Log.i(TAG, ">>> RECEIVED " + bytesRead + " bytes from serial port!");

                // FIX 2: Dispatch raw data to callback if registered
                if (dataCallback != null) {
                    byte[] rawData = new byte[bytesRead];
                    System.arraycopy(tempBuffer, 0, rawData, 0, bytesRead);
                    dataCallback.onDataReceived(rawData);
                }

                // Copy to rx buffer - DMVI line 118
                System.arraycopy(tempBuffer, 0, rxBuffer, head, bytesRead);
                head += bytesRead;

                // Log raw RX for debugging
                logData("RX", tempBuffer, bytesRead);

                // Process complete packets - DMVI lines 145-161
                processPackets();

            } catch (IOException e) {
                if (running) {
                    Log.w(TAG, "Read error: " + e.getMessage());
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException ie) {
                        break;
                    }
                }
            }
        }

        Log.d(TAG, "Read loop terminated");
    }

    /**
     * Process complete packets using DMVI's exact framing logic.
     * Marshall packets have a 2-byte length prefix (little-endian).
     *
     * DMVI lines 145-161:
     * - Read length from first 2 bytes (little-endian) + 2
     * - Validate: 9 <= length <= 512
     * - If invalid: reset buffer
     * - If complete packet available: dispatch to linkEvents.onReceive()
     * - Reset buffer when near end and caught up
     */
    private void processPackets() {
        if (linkEvents == null) return;

        while (head != tail) {
            int available = head - tail;

            // Need at least 2 bytes for length prefix
            if (available < 2) break;

            // Read packet length from first 2 bytes (little-endian) + 2
            // DMVI line 146: ByteArrayUtils.byteArrToShort(bArr, this.tail) + 2
            int packetLen = ((rxBuffer[tail] & 0xFF) | ((rxBuffer[tail + 1] & 0xFF) << 8)) + 2;

            // Validate packet length - DMVI line 148
            if (packetLen < MIN_PACKET_SIZE || packetLen > MAX_PACKET_SIZE) {
                Log.w(TAG, "Invalid packet length: " + packetLen + ", resetting buffer");
                resetBuffer();
                return;
            }

            // Check if we have the complete packet
            if (available < packetLen) {
                // Wait for more data
                break;
            }

            // Dispatch complete packet to Marshall SDK - DMVI line 151
            Log.d(TAG, "Dispatching packet: " + packetLen + " bytes");
            boolean accepted = linkEvents.onReceive(rxBuffer, tail, packetLen);

            if (!accepted) {
                Log.w(TAG, "Packet rejected by SDK, resetting buffer");
                resetBuffer();
                return;
            }

            // Advance tail past this packet - DMVI line 154
            tail += packetLen;

            // Reset buffer when near end and caught up - DMVI lines 156-157
            if (RX_BUFFER_SIZE - tail <= 412 && tail == head) {
                resetBuffer();
                return;
            }
        }
    }

    /**
     * Reset the circular buffer - DMVI lines 101-104
     */
    private void resetBuffer() {
        synchronized (this) {
            head = 0;
            tail = 0;
        }
    }

    /**
     * Log data in hex format for debugging
     */
    private void logData(String prefix, byte[] data, int length) {
        StringBuilder sb = new StringBuilder(prefix + "[" + length + "]: ");
        int limit = Math.min(length, 64);
        for (int i = 0; i < limit; i++) {
            sb.append(String.format("%02X ", data[i]));
        }
        if (length > 64) {
            sb.append("...");
        }
        Log.d(TAG, sb.toString());
    }

    public boolean isConnected() {
        return serialPort != null && running;
    }

    /**
     * Get diagnostics for debugging
     */
    public String getDiagnostics() {
        return String.format(
            "UsbSerialBridge: running=%b, port=%s, head=%d, tail=%d, buffered=%d",
            running,
            serialPort != null ? "open" : "null",
            head,
            tail,
            head - tail
        );
    }
}
