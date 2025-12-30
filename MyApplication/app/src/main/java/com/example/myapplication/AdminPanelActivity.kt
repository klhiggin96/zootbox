package com.example.myapplication

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.database.InventoryRepository
import com.example.myapplication.database.models.Coil
import java.security.MessageDigest

/**
 * Admin Panel Activity
 *
 * PIN-protected admin interface for:
 * - Viewing inventory status
 * - Filling all coils to 10 units
 * - Manual entry per row
 * - Viewing transaction statistics
 */
class AdminPanelActivity : AppCompatActivity() {

    private lateinit var repository: InventoryRepository
    private lateinit var prefs: SharedPreferences

    // PIN Entry UI
    private lateinit var pinEntryLayout: LinearLayout
    private lateinit var pinDots: List<View>
    private lateinit var pinErrorText: TextView
    private lateinit var pinKeypad: GridLayout

    // Inventory Overview UI
    private lateinit var inventoryLayout: LinearLayout
    private lateinit var recyclerView: RecyclerView
    private lateinit var totalInventoryText: TextView
    private lateinit var statsText: TextView
    private lateinit var fillAllButton: Button
    private lateinit var manualEntryButton: Button
    private lateinit var viewStatsButton: Button
    private lateinit var exitButton: Button

    // State
    private var enteredPin = ""
    private var failedAttempts = 0
    private var lockoutUntil = 0L
    private val adapter = InventoryAdapter()

    companion object {
        private const val TAG = "AdminPanelActivity"
        private const val PREF_NAME = "admin_prefs"
        private const val PREF_PIN_HASH = "admin_pin_hash"
        private const val PREF_FAILED_ATTEMPTS = "failed_attempts"
        private const val PREF_LOCKOUT_UNTIL = "lockout_until"
        private const val DEFAULT_PIN = "8888"
        private const val MAX_ATTEMPTS = 3
        private const val LOCKOUT_DURATION_MS = 30000L // 30 seconds
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_panel)

        repository = InventoryRepository.getInstance(this)
        prefs = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

        // Initialize default PIN if not set
        if (!prefs.contains(PREF_PIN_HASH)) {
            savePin(DEFAULT_PIN)
        }

        // Load lockout state
        failedAttempts = prefs.getInt(PREF_FAILED_ATTEMPTS, 0)
        lockoutUntil = prefs.getLong(PREF_LOCKOUT_UNTIL, 0)

        initViews()
        showPinEntry()
    }

    private fun initViews() {
        // PIN Entry Layout
        pinEntryLayout = findViewById(R.id.pin_entry_layout)
        pinDots = listOf(
            findViewById(R.id.pin_dot_1),
            findViewById(R.id.pin_dot_2),
            findViewById(R.id.pin_dot_3),
            findViewById(R.id.pin_dot_4)
        )
        pinErrorText = findViewById(R.id.pin_error_text)
        pinKeypad = findViewById(R.id.pin_keypad)

        // Inventory Overview Layout
        inventoryLayout = findViewById(R.id.inventory_layout)
        recyclerView = findViewById(R.id.coils_recycler_view)
        totalInventoryText = findViewById(R.id.total_inventory_text)
        statsText = findViewById(R.id.stats_text)
        fillAllButton = findViewById(R.id.fill_all_button)
        manualEntryButton = findViewById(R.id.manual_entry_button)
        viewStatsButton = findViewById(R.id.view_stats_button)
        exitButton = findViewById(R.id.exit_button)

        // Setup RecyclerView
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        // Setup keypad
        setupKeypad()

        // Setup buttons
        fillAllButton.setOnClickListener { showFillAllDialog() }
        manualEntryButton.setOnClickListener { showManualEntryDialog() }
        viewStatsButton.setOnClickListener { showStatsDialog() }
        exitButton.setOnClickListener { finish() }
    }

    private fun setupKeypad() {
        // Number buttons 0-9
        for (i in 0..9) {
            val button = Button(this).apply {
                text = i.toString()
                textSize = 24f
                setOnClickListener { onNumberPressed(i) }
            }
            pinKeypad.addView(button)
        }

        // Clear button
        val clearButton = Button(this).apply {
            text = "Clear"
            setOnClickListener { clearPin() }
        }
        pinKeypad.addView(clearButton)

        // Enter button
        val enterButton = Button(this).apply {
            text = "Enter"
            setOnClickListener { verifyPin() }
        }
        pinKeypad.addView(enterButton)
    }

    private fun showPinEntry() {
        pinEntryLayout.visibility = View.VISIBLE
        inventoryLayout.visibility = View.GONE
        clearPin()

        // Check if locked out
        if (isLockedOut()) {
            val remainingTime = (lockoutUntil - System.currentTimeMillis()) / 1000
            pinErrorText.text = "Locked out. Try again in $remainingTime seconds"
            pinErrorText.visibility = View.VISIBLE
            pinKeypad.alpha = 0.3f
            pinKeypad.isEnabled = false

            // Schedule unlock
            Handler(Looper.getMainLooper()).postDelayed({
                unlockKeypad()
            }, lockoutUntil - System.currentTimeMillis())
        } else {
            pinKeypad.alpha = 1f
            pinKeypad.isEnabled = true
        }
    }

    private fun showInventoryOverview() {
        pinEntryLayout.visibility = View.GONE
        inventoryLayout.visibility = View.VISIBLE
        refreshInventoryView()
    }

    private fun onNumberPressed(number: Int) {
        if (isLockedOut()) return

        if (enteredPin.length < 4) {
            enteredPin += number.toString()
            updatePinDots()

            // Auto-verify when 4 digits entered
            if (enteredPin.length == 4) {
                Handler(Looper.getMainLooper()).postDelayed({
                    verifyPin()
                }, 300)
            }
        }
    }

    private fun clearPin() {
        enteredPin = ""
        updatePinDots()
        pinErrorText.visibility = View.GONE
    }

    private fun updatePinDots() {
        pinDots.forEachIndexed { index, dot ->
            if (index < enteredPin.length) {
                dot.setBackgroundResource(R.drawable.pin_dot_filled)
            } else {
                dot.setBackgroundResource(R.drawable.pin_dot_empty)
            }
        }
    }

    private fun verifyPin() {
        if (isLockedOut()) return

        if (enteredPin.length != 4) {
            showPinError("Enter 4-digit PIN")
            return
        }

        if (checkPin(enteredPin)) {
            // Success
            failedAttempts = 0
            prefs.edit().putInt(PREF_FAILED_ATTEMPTS, 0).apply()
            showInventoryOverview()
        } else {
            // Failed
            failedAttempts++
            prefs.edit().putInt(PREF_FAILED_ATTEMPTS, failedAttempts).apply()

            if (failedAttempts >= MAX_ATTEMPTS) {
                lockout()
            } else {
                val remaining = MAX_ATTEMPTS - failedAttempts
                showPinError("Wrong PIN. $remaining attempts remaining")
                shakeAnimation()
            }

            clearPin()
        }
    }

    private fun showPinError(message: String) {
        pinErrorText.text = message
        pinErrorText.visibility = View.VISIBLE
    }

    private fun shakeAnimation() {
        val shake = AnimationUtils.loadAnimation(this, R.anim.shake)
        pinEntryLayout.startAnimation(shake)
    }

    private fun lockout() {
        lockoutUntil = System.currentTimeMillis() + LOCKOUT_DURATION_MS
        prefs.edit().putLong(PREF_LOCKOUT_UNTIL, lockoutUntil).apply()

        pinErrorText.text = "Too many failed attempts. Locked for 30 seconds"
        pinErrorText.visibility = View.VISIBLE
        pinKeypad.alpha = 0.3f
        pinKeypad.isEnabled = false

        Handler(Looper.getMainLooper()).postDelayed({
            unlockKeypad()
        }, LOCKOUT_DURATION_MS)
    }

    private fun unlockKeypad() {
        if (System.currentTimeMillis() >= lockoutUntil) {
            failedAttempts = 0
            lockoutUntil = 0
            prefs.edit()
                .putInt(PREF_FAILED_ATTEMPTS, 0)
                .putLong(PREF_LOCKOUT_UNTIL, 0)
                .apply()

            pinKeypad.alpha = 1f
            pinKeypad.isEnabled = true
            pinErrorText.visibility = View.GONE
            clearPin()
        }
    }

    private fun isLockedOut(): Boolean {
        return System.currentTimeMillis() < lockoutUntil
    }

    private fun refreshInventoryView() {
        val coils = repository.getAllCoils()
        adapter.updateCoils(coils)

        // Update total inventory
        val totalInventory = repository.getTotalInventory()
        totalInventoryText.text = "Total Stock: $totalInventory / ${coils.size * 10}"

        // Update stats
        val status = repository.getStockStatus()
        statsText.text = """
            Empty: ${status["empty"]} | Low: ${status["low"]} |
            Normal: ${status["normal"]} | Full: ${status["full"]} |
            Jammed: ${status["jammed"]}
        """.trimIndent()
    }

    private fun showFillAllDialog() {
        val currentTotal = repository.getTotalInventory()

        AlertDialog.Builder(this)
            .setTitle("Refill All Coils")
            .setMessage("This will set all coils to 10 units.\n\nCurrent total: $currentTotal units")
            .setPositiveButton("Confirm") { _, _ ->
                val rowsAffected = repository.resetAllInventory(10)
                Toast.makeText(this, "Filled $rowsAffected coils to 10 units", Toast.LENGTH_SHORT).show()
                refreshInventoryView()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showManualEntryDialog() {
        val coils = repository.getAllCoils()
        val dialogView = layoutInflater.inflate(R.layout.dialog_manual_entry, null)
        val container = dialogView.findViewById<LinearLayout>(R.id.manual_entry_container)

        val pickers = mutableMapOf<String, NumberPicker>()

        coils.forEach { coil ->
            val rowView = layoutInflater.inflate(R.layout.item_manual_entry_row, container, false)
            val label = rowView.findViewById<TextView>(R.id.row_label)
            val current = rowView.findViewById<TextView>(R.id.current_value)
            val picker = rowView.findViewById<NumberPicker>(R.id.number_picker)

            label.text = "Row ${coil.getRowNumber()} (${coil.id})"
            current.text = "Current: ${coil.inventory}/10"

            picker.minValue = 0
            picker.maxValue = 10
            picker.value = coil.inventory
            picker.wrapSelectorWheel = false

            pickers[coil.id] = picker
            container.addView(rowView)
        }

        AlertDialog.Builder(this)
            .setTitle("Manual Inventory Entry")
            .setView(dialogView)
            .setPositiveButton("Save Changes") { _, _ ->
                var updatedCount = 0
                pickers.forEach { (coilId, picker) ->
                    if (repository.updateInventory(coilId, picker.value)) {
                        updatedCount++
                    }
                }
                Toast.makeText(this, "Updated $updatedCount coils", Toast.LENGTH_SHORT).show()
                refreshInventoryView()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showStatsDialog() {
        val stats = repository.getTodayStats()
        val history = repository.getTransactionHistory(10)

        val message = """
            Today's Statistics:

            Total Vends: ${stats.total}
            Successful: ${stats.success}
            Jams: ${stats.jams}
            Failed: ${stats.failed}
            Success Rate: ${"%.1f".format(stats.successRate)}%

            Recent Transactions:
            ${history.take(5).joinToString("\n") {
                "${it.coilId}: ${it.status} (${it.getRelativeTime()})"
            }}
        """.trimIndent()

        AlertDialog.Builder(this)
            .setTitle("Transaction Statistics")
            .setMessage(message)
            .setPositiveButton("OK", null)
            .setNeutralButton("Clear Old Data") { _, _ ->
                showClearDataDialog()
            }
            .show()
    }

    private fun showClearDataDialog() {
        AlertDialog.Builder(this)
            .setTitle("Clear Old Transactions")
            .setMessage("Delete transactions older than 90 days?")
            .setPositiveButton("Delete") { _, _ ->
                val deleted = repository.clearOldTransactions(90)
                Toast.makeText(this, "Deleted $deleted old transactions", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    // PIN Management

    private fun hashPin(pin: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(pin.toByteArray())
        return hash.joinToString("") { "%02x".format(it) }
    }

    private fun savePin(pin: String) {
        val hash = hashPin(pin)
        prefs.edit().putString(PREF_PIN_HASH, hash).apply()
    }

    private fun checkPin(pin: String): Boolean {
        val stored = prefs.getString(PREF_PIN_HASH, hashPin(DEFAULT_PIN))
        return hashPin(pin) == stored
    }

    override fun onDestroy() {
        super.onDestroy()
        // Don't close repository - it's a singleton
    }
}

/**
 * RecyclerView Adapter for inventory list
 */
class InventoryAdapter : RecyclerView.Adapter<InventoryAdapter.ViewHolder>() {

    private var coils = listOf<Coil>()

    fun updateCoils(newCoils: List<Coil>) {
        coils = newCoils
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): ViewHolder {
        val view = android.view.LayoutInflater.from(parent.context)
            .inflate(R.layout.item_inventory_row, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(coils[position])
    }

    override fun getItemCount() = coils.size

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val rowLabel: TextView = itemView.findViewById(R.id.row_label)
        private val stockBar: ProgressBar = itemView.findViewById(R.id.stock_bar)
        private val stockText: TextView = itemView.findViewById(R.id.stock_text)
        private val statusBadge: TextView = itemView.findViewById(R.id.status_badge)

        fun bind(coil: Coil) {
            rowLabel.text = "Row ${coil.getRowNumber()} (${coil.id})"
            stockBar.max = Coil.MAX_INVENTORY
            stockBar.progress = coil.inventory
            stockText.text = "${coil.inventory}/10"
            statusBadge.text = coil.getDisplayStatus()

            // Color coding
            val color = when {
                coil.isJammed() -> android.graphics.Color.RED
                coil.isEmpty() -> android.graphics.Color.GRAY
                coil.isLowStock() -> android.graphics.Color.rgb(255, 165, 0) // Orange
                else -> android.graphics.Color.GREEN
            }
            statusBadge.setTextColor(color)
        }
    }
}
