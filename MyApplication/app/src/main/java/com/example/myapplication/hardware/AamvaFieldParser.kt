package com.example.myapplication.hardware

import java.text.SimpleDateFormat
import java.util.*

/**
 * AAMVA PDF417 Field Parser
 * Implements field-specific extraction per AAMVA standard with proper format handling
 *
 * Supports AAMVA versions 2+ with fallback to legacy formats
 */
object AamvaFieldParser {
    // AAMVA Record Separator (RS - ASCII 0x1E)
    private const val RECORD_SEPARATOR = '\u001E'

    // Regex patterns for AAMVA fields (exact as per AAMVA specification)
    // DBB: Date of Birth (Modern AAMVA v2+, MMDDYYYY format)
    private val DBB_PATTERN = Regex("""DBB(\d{8})""")

    // DAA: Date of Birth (Legacy AAMVA, YYYYMMDD format)
    private val DAA_PATTERN = Regex("""DAA(\d{8})""")

    // DBA: License Expiration Date (MMDDYYYY format)
    private val DBA_PATTERN = Regex("""DBA(\d{8})""")

    // DAC: First Name (uppercase letters and spaces)
    private val DAC_PATTERN = Regex("""DAC([A-Z\s]+?)(?=\n|$RECORD_SEPARATOR|D[A-Z]{2})""")

    // DCS: Last Name (uppercase letters and spaces)
    private val DCS_PATTERN = Regex("""DCS([A-Z\s]+?)(?=\n|$RECORD_SEPARATOR|D[A-Z]{2})""")

    // Date formatters for AAMVA formats
    private val FORMAT_MMDDYYYY = SimpleDateFormat("MMddyyyy", Locale.US).apply {
        isLenient = false
    }
    private val FORMAT_YYYYMMDD = SimpleDateFormat("yyyyMMdd", Locale.US).apply {
        isLenient = false
    }

    /**
     * Extracts Date of Birth from AAMVA data
     * Priority: DBB (modern AAMVA v2+) > DAA (legacy)
     *
     * @param data Raw barcode data string
     * @return Parsed Date object or null if not found/invalid
     */
    fun extractDateOfBirth(data: String): Date? {
        // Try modern DBB field first (MMDDYYYY format)
        DBB_PATTERN.find(data)?.let { match ->
            val dateStr = match.groupValues[1]
            return parseDate(dateStr, prioritizeMMDDYYYY = true)
        }

        // Fallback to legacy DAA field (YYYYMMDD format)
        DAA_PATTERN.find(data)?.let { match ->
            val dateStr = match.groupValues[1]
            return parseDate(dateStr, prioritizeMMDDYYYY = false)
        }

        return null
    }

    /**
     * Extracts Expiration Date (DBA field)
     *
     * @param data Raw barcode data string
     * @return Parsed Date object or null if not found/invalid
     */
    fun extractExpirationDate(data: String): Date? {
        return DBA_PATTERN.find(data)?.let { match ->
            val dateStr = match.groupValues[1]
            parseDate(dateStr, prioritizeMMDDYYYY = true)
        }
    }

    /**
     * Extracts First Name (DAC field)
     *
     * @param data Raw barcode data string
     * @return First name string (trimmed) or null if not found
     */
    fun extractFirstName(data: String): String? {
        return DAC_PATTERN.find(data)?.groupValues?.get(1)?.trim()
    }

    /**
     * Extracts Last Name (DCS field)
     *
     * @param data Raw barcode data string
     * @return Last name string (trimmed) or null if not found
     */
    fun extractLastName(data: String): String? {
        return DCS_PATTERN.find(data)?.groupValues?.get(1)?.trim()
    }

    /**
     * Parses date string with format auto-detection
     * Supports both MMDDYYYY and YYYYMMDD formats
     *
     * @param dateStr 8-digit date string
     * @param prioritizeMMDDYYYY If true, tries MMDDYYYY first; otherwise YYYYMMDD first
     * @return Parsed Date object or null if invalid
     */
    private fun parseDate(dateStr: String, prioritizeMMDDYYYY: Boolean = true): Date? {
        if (dateStr.length != 8) return null

        val formats = if (prioritizeMMDDYYYY) {
            listOf(FORMAT_MMDDYYYY, FORMAT_YYYYMMDD)
        } else {
            listOf(FORMAT_YYYYMMDD, FORMAT_MMDDYYYY)
        }

        for (format in formats) {
            try {
                val date = format.parse(dateStr)
                if (isValidDate(date)) return date
            } catch (e: Exception) {
                // Try next format
            }
        }

        return null
    }

    /**
     * Validates date is within reasonable range (1900-2100)
     * Prevents parsing errors from corrupted or invalid data
     *
     * @param date Date to validate
     * @return True if date is within valid range
     */
    private fun isValidDate(date: Date?): Boolean {
        if (date == null) return false
        val calendar = Calendar.getInstance().apply { time = date }
        val year = calendar.get(Calendar.YEAR)
        return year in 1900..2100
    }

    /**
     * Validates AAMVA header presence
     * Checks for @ANSI or standalone @ prefix
     *
     * @param data Raw barcode data string
     * @return True if valid AAMVA header detected
     */
    fun hasValidAamvaHeader(data: String): Boolean {
        return data.contains("ANSI", ignoreCase = true) || data.startsWith("@")
    }

    /**
     * Validates that minimum required fields are present
     * At minimum, Date of Birth (DBB or DAA) must be present
     *
     * @param data Raw barcode data string
     * @return True if required fields exist
     */
    fun hasRequiredFields(data: String): Boolean {
        return DBB_PATTERN.containsMatchIn(data) || DAA_PATTERN.containsMatchIn(data)
    }
}
