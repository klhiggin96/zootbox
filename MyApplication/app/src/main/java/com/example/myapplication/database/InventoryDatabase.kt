package com.example.myapplication.database

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log

/**
 * SQLite Database Helper for ZootBox Local Inventory Management
 *
 * This database stores inventory and transaction data locally on the Android device,
 * allowing the vending machine to operate completely offline.
 *
 * Schema:
 * - coils: 10 physical coil positions (A1-J1), each holding 0-10 units
 * - local_transactions: Immutable log of all vend attempts (success/jam/failed)
 */
class InventoryDatabase(context: Context) : SQLiteOpenHelper(
    context,
    DATABASE_NAME,
    null,
    DATABASE_VERSION
) {

    companion object {
        private const val DATABASE_NAME = "zootbox_inventory.db"
        private const val DATABASE_VERSION = 1
        private const val TAG = "InventoryDatabase"

        // Table names
        const val TABLE_COILS = "coils"
        const val TABLE_TRANSACTIONS = "local_transactions"

        // Coils table columns
        const val COLUMN_COIL_ID = "id"
        const val COLUMN_INVENTORY = "inventory"
        const val COLUMN_STATUS = "status"
        const val COLUMN_UPDATED_AT = "updated_at"

        // Transactions table columns
        const val COLUMN_TRANSACTION_ID = "id"
        const val COLUMN_TRANSACTION_COIL_ID = "coil_id"
        const val COLUMN_TRANSACTION_STATUS = "status"
        const val COLUMN_TIMESTAMP = "timestamp"
        const val COLUMN_SYNCED = "synced"

        // Status values
        const val STATUS_AVAILABLE = "available"
        const val STATUS_JAMMED = "jammed"

        // Transaction status values
        const val TRANSACTION_SUCCESS = "success"
        const val TRANSACTION_JAM = "jam"
        const val TRANSACTION_FAILED = "failed"

        // Coil IDs (10 physical motors)
        val COIL_IDS = listOf("A1", "B1", "C1", "D1", "E1", "F1", "G1", "H1", "I1", "J1")

        @Volatile
        private var INSTANCE: InventoryDatabase? = null

        /**
         * Get singleton instance of database
         */
        fun getInstance(context: Context): InventoryDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: InventoryDatabase(context.applicationContext).also {
                    INSTANCE = it
                }
            }
        }
    }

    override fun onCreate(db: SQLiteDatabase) {
        Log.d(TAG, "Creating database schema version $DATABASE_VERSION")

        // Create coils table
        val createCoilsTable = """
            CREATE TABLE IF NOT EXISTS $TABLE_COILS (
                $COLUMN_COIL_ID TEXT PRIMARY KEY,
                $COLUMN_INVENTORY INTEGER NOT NULL DEFAULT 10 CHECK($COLUMN_INVENTORY >= 0 AND $COLUMN_INVENTORY <= 10),
                $COLUMN_STATUS TEXT NOT NULL DEFAULT '$STATUS_AVAILABLE' CHECK($COLUMN_STATUS IN ('$STATUS_AVAILABLE', '$STATUS_JAMMED')),
                $COLUMN_UPDATED_AT INTEGER DEFAULT (strftime('%s', 'now'))
            )
        """.trimIndent()

        db.execSQL(createCoilsTable)
        Log.d(TAG, "Created coils table")

        // Create transactions table
        val createTransactionsTable = """
            CREATE TABLE IF NOT EXISTS $TABLE_TRANSACTIONS (
                $COLUMN_TRANSACTION_ID TEXT PRIMARY KEY,
                $COLUMN_TRANSACTION_COIL_ID TEXT NOT NULL,
                $COLUMN_TRANSACTION_STATUS TEXT NOT NULL CHECK($COLUMN_TRANSACTION_STATUS IN ('$TRANSACTION_SUCCESS', '$TRANSACTION_JAM', '$TRANSACTION_FAILED')),
                $COLUMN_TIMESTAMP INTEGER NOT NULL DEFAULT (strftime('%s', 'now')),
                $COLUMN_SYNCED INTEGER DEFAULT 0,
                FOREIGN KEY ($COLUMN_TRANSACTION_COIL_ID) REFERENCES $TABLE_COILS($COLUMN_COIL_ID)
            )
        """.trimIndent()

        db.execSQL(createTransactionsTable)
        Log.d(TAG, "Created local_transactions table")

        // Create index for transaction queries
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_transactions_coil ON $TABLE_TRANSACTIONS($COLUMN_TRANSACTION_COIL_ID, $COLUMN_TIMESTAMP DESC)")
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_transactions_synced ON $TABLE_TRANSACTIONS($COLUMN_SYNCED) WHERE $COLUMN_SYNCED = 0")

        // Seed initial coil data (10 coils, each with 10 units)
        seedInitialData(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        Log.d(TAG, "Upgrading database from version $oldVersion to $newVersion")

        // Future migration logic goes here
        when (oldVersion) {
            1 -> {
                // Migration from v1 to v2 (when needed)
            }
        }
    }

    override fun onOpen(db: SQLiteDatabase) {
        super.onOpen(db)
        // Enable foreign key constraints
        db.execSQL("PRAGMA foreign_keys=ON")
    }

    /**
     * Seed initial inventory data
     * Creates 10 coils (A1-J1), each starting with 10 units in available status
     */
    private fun seedInitialData(db: SQLiteDatabase) {
        Log.d(TAG, "Seeding initial coil data")

        db.beginTransaction()
        try {
            COIL_IDS.forEach { coilId ->
                val values = ContentValues().apply {
                    put(COLUMN_COIL_ID, coilId)
                    put(COLUMN_INVENTORY, 10)
                    put(COLUMN_STATUS, STATUS_AVAILABLE)
                    put(COLUMN_UPDATED_AT, System.currentTimeMillis() / 1000)
                }

                val result = db.insertWithOnConflict(
                    TABLE_COILS,
                    null,
                    values,
                    SQLiteDatabase.CONFLICT_IGNORE
                )

                if (result != -1L) {
                    Log.d(TAG, "Inserted coil: $coilId with 10 units")
                }
            }

            db.setTransactionSuccessful()
            Log.d(TAG, "Successfully seeded ${COIL_IDS.size} coils")
        } catch (e: Exception) {
            Log.e(TAG, "Error seeding initial data", e)
        } finally {
            db.endTransaction()
        }
    }

    /**
     * Clear all data and reseed (for testing/development only)
     */
    fun resetDatabase() {
        Log.w(TAG, "Resetting entire database")
        writableDatabase.use { db ->
            db.execSQL("DELETE FROM $TABLE_TRANSACTIONS")
            db.execSQL("DELETE FROM $TABLE_COILS")
            seedInitialData(db)
        }
    }

    /**
     * Get database size in bytes
     */
    fun getDatabaseSize(): Long {
        return readableDatabase.use { db ->
            db.path?.let { java.io.File(it).length() } ?: 0L
        }
    }

    /**
     * Optimize database (vacuum, analyze)
     */
    fun optimizeDatabase() {
        Log.d(TAG, "Optimizing database")
        writableDatabase.use { db ->
            db.execSQL("VACUUM")
            db.execSQL("ANALYZE")
        }
    }
}
