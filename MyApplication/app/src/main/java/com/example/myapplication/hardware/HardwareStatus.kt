package com.example.myapplication.hardware

/**
 * Represents the hardware initialization status for the loading screen.
 * Used by MainActivity to show appropriate status messages during startup.
 */
sealed class HardwareStatus {
    /** Initial state during 10-second USB permission database wait */
    object WaitingForUsbDatabase : HardwareStatus()

    /** HardwareService started, connecting to Nayax payment reader */
    object ConnectingNayax : HardwareStatus()

    /** Connecting to ID scanner */
    object ConnectingScanner : HardwareStatus()

    /** All hardware connected and ready */
    object Ready : HardwareStatus()

    /** Error occurred during hardware initialization */
    data class Error(val message: String) : HardwareStatus()
}
