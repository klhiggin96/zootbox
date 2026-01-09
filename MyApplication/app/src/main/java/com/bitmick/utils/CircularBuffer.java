package com.bitmick.utils;

/**
 * Thread-safe Circular Buffer (FIFO) for bridging async USB reads with sync SDK polling.
 */
public class CircularBuffer {
    private final byte[] buffer;
    private int head = 0;
    private int tail = 0;
    private int size = 0;
    private final int capacity;

    public CircularBuffer(int capacity) {
        this.capacity = capacity;
        this.buffer = new byte[capacity];
    }

    /**
     * Write data to the buffer.
     */
    public synchronized void write(byte[] data) {
        for (byte b : data) {
            if (size < capacity) {
                buffer[head] = b;
                head = (head + 1) % capacity;
                size++;
            }
            // If full, oldest data is overwritten or we can drop it.
            // For serial data, we should ideally drop if we can't keep up.
        }
    }

    /**
     * Read a single byte from the buffer.
     */
    public synchronized byte read() {
        if (size == 0) return 0;
        byte b = buffer[tail];
        tail = (tail + 1) % capacity;
        size--;
        return b;
    }

    /**
     * Read multiple bytes from the buffer.
     */
    public synchronized int read(byte[] out, int length) {
        int count = Math.min(length, size);
        for (int i = 0; i < count; i++) {
            out[i] = read();
        }
        return count;
    }

    /**
     * Returns the number of bytes currently in the buffer.
     */
    public synchronized int size() {
        return size;
    }

    /**
     * Resets the buffer pointers.
     */
    public synchronized void reset() {
        head = 0;
        tail = 0;
        size = 0;
    }
}

