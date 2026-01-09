package com.bitmick.marshall.drivers;

/**
 * Interface mirroring the Marshall C SDK drv_usart requirements.
 * Used for pull-based serial communication.
 */
public interface serial_port_i {
    /**
     * Returns the count of bytes currently waiting in the buffer.
     * Corresponds to drv_usart_get_rx_pending.
     */
    int available();

    /**
     * Reads and removes a single byte from the buffer.
     * Corresponds to drv_usart_rx_byte.
     */
    byte read_byte();

    /**
     * Reads multiple bytes into a buffer.
     * Corresponds to drv_usart_rx_chunk.
     * @return Number of bytes actually read.
     */
    int read(byte[] buffer, int length);

    /**
     * Sends bytes to the serial device.
     * Corresponds to drv_usart_tx_data.
     */
    void write(byte[] data);

    /**
     * Closes the serial port.
     */
    void close();

    /**
     * Flushes the serial port buffers.
     */
    void flush();
}

