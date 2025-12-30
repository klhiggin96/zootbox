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
        private const val DATABASE_VERSION = 2
        private const val TAG = "InventoryDatabase"

        // Table names
        const val TABLE_COILS = "coils"
        const val TABLE_TRANSACTIONS = "local_transactions"
        const val TABLE_PRODUCTS = "products"
        const val TABLE_PRODUCT_COIL_ASSIGNMENTS = "product_coil_assignments"

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
        const val COLUMN_AMOUNT = "amount"
        const val COLUMN_PAYMENT_METHOD = "payment_method"
        const val COLUMN_PAYMENT_STATUS = "payment_status"
        const val COLUMN_CURRENCY = "currency"
        const val COLUMN_NAYAX_TRANSACTION_ID = "nayax_transaction_id"
        const val COLUMN_PRODUCT_ID = "product_id"

        // Products table columns
        const val COLUMN_PRODUCT_ID_PK = "id"
        const val COLUMN_PRODUCT_NAME = "name"
        const val COLUMN_PRODUCT_CATEGORY = "category"
        const val COLUMN_PRODUCT_PRICE = "price"
        const val COLUMN_PRODUCT_AGE_RESTRICTION = "age_restriction"
        const val COLUMN_PRODUCT_IMAGE_URL = "image_url"
        const val COLUMN_PRODUCT_VIDEO_FILENAME = "video_filename"
        const val COLUMN_PRODUCT_IS_DIGITAL = "is_digital"
        const val COLUMN_PRODUCT_ACTIVE = "active"
        const val COLUMN_PRODUCT_CREATED_AT = "created_at"
        const val COLUMN_PRODUCT_UPDATED_AT = "updated_at"

        // Product-Coil Assignments table columns
        const val COLUMN_ASSIGNMENT_ID = "id"
        const val COLUMN_ASSIGNMENT_PRODUCT_ID = "product_id"
        const val COLUMN_ASSIGNMENT_COIL_ID = "coil_id"
        const val COLUMN_ASSIGNMENT_PRIORITY = "priority"
        const val COLUMN_ASSIGNMENT_CREATED_AT = "created_at"

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

        // Create products table
        val createProductsTable = """
            CREATE TABLE IF NOT EXISTS $TABLE_PRODUCTS (
                $COLUMN_PRODUCT_ID_PK TEXT PRIMARY KEY,
                $COLUMN_PRODUCT_NAME TEXT NOT NULL,
                $COLUMN_PRODUCT_CATEGORY TEXT NOT NULL,
                $COLUMN_PRODUCT_PRICE REAL NOT NULL CHECK($COLUMN_PRODUCT_PRICE >= 0),
                $COLUMN_PRODUCT_AGE_RESTRICTION INTEGER DEFAULT 0,
                $COLUMN_PRODUCT_IMAGE_URL TEXT,
                $COLUMN_PRODUCT_VIDEO_FILENAME TEXT,
                $COLUMN_PRODUCT_IS_DIGITAL INTEGER DEFAULT 0,
                $COLUMN_PRODUCT_ACTIVE INTEGER DEFAULT 1,
                $COLUMN_PRODUCT_CREATED_AT INTEGER DEFAULT (strftime('%s', 'now')),
                $COLUMN_PRODUCT_UPDATED_AT INTEGER DEFAULT (strftime('%s', 'now'))
            )
        """.trimIndent()

        db.execSQL(createProductsTable)
        Log.d(TAG, "Created products table")

        // Create product-coil assignments table
        val createAssignmentsTable = """
            CREATE TABLE IF NOT EXISTS $TABLE_PRODUCT_COIL_ASSIGNMENTS (
                $COLUMN_ASSIGNMENT_ID TEXT PRIMARY KEY,
                $COLUMN_ASSIGNMENT_PRODUCT_ID TEXT NOT NULL,
                $COLUMN_ASSIGNMENT_COIL_ID TEXT NOT NULL,
                $COLUMN_ASSIGNMENT_PRIORITY INTEGER DEFAULT 0,
                $COLUMN_ASSIGNMENT_CREATED_AT INTEGER DEFAULT (strftime('%s', 'now')),
                FOREIGN KEY ($COLUMN_ASSIGNMENT_PRODUCT_ID) REFERENCES $TABLE_PRODUCTS($COLUMN_PRODUCT_ID_PK) ON DELETE CASCADE,
                FOREIGN KEY ($COLUMN_ASSIGNMENT_COIL_ID) REFERENCES $TABLE_COILS($COLUMN_COIL_ID) ON DELETE CASCADE,
                UNIQUE ($COLUMN_ASSIGNMENT_PRODUCT_ID, $COLUMN_ASSIGNMENT_COIL_ID)
            )
        """.trimIndent()

        db.execSQL(createAssignmentsTable)
        Log.d(TAG, "Created product_coil_assignments table")

        // Create indexes for product tables
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_products_category ON $TABLE_PRODUCTS($COLUMN_PRODUCT_CATEGORY)")
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_products_active ON $TABLE_PRODUCTS($COLUMN_PRODUCT_ACTIVE)")
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_assignments_product ON $TABLE_PRODUCT_COIL_ASSIGNMENTS($COLUMN_ASSIGNMENT_PRODUCT_ID)")
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_assignments_coil ON $TABLE_PRODUCT_COIL_ASSIGNMENTS($COLUMN_ASSIGNMENT_COIL_ID)")

        // Seed initial coil data (10 coils, each with 10 units)
        seedInitialData(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        Log.d(TAG, "Upgrading database from version $oldVersion to $newVersion")

        // Migration logic
        if (oldVersion < 2) {
            Log.d(TAG, "Migrating from v1 to v2: Adding payment and product catalog support")

            // Add payment columns to transactions table
            db.execSQL("ALTER TABLE $TABLE_TRANSACTIONS ADD COLUMN $COLUMN_AMOUNT REAL")
            db.execSQL("ALTER TABLE $TABLE_TRANSACTIONS ADD COLUMN $COLUMN_PAYMENT_METHOD TEXT")
            db.execSQL("ALTER TABLE $TABLE_TRANSACTIONS ADD COLUMN $COLUMN_PAYMENT_STATUS TEXT")
            db.execSQL("ALTER TABLE $TABLE_TRANSACTIONS ADD COLUMN $COLUMN_CURRENCY TEXT DEFAULT 'USD'")
            db.execSQL("ALTER TABLE $TABLE_TRANSACTIONS ADD COLUMN $COLUMN_NAYAX_TRANSACTION_ID TEXT")
            db.execSQL("ALTER TABLE $TABLE_TRANSACTIONS ADD COLUMN $COLUMN_PRODUCT_ID TEXT")

            Log.d(TAG, "Added payment columns to transactions table")

            // Create products table
            val createProductsTable = """
                CREATE TABLE IF NOT EXISTS $TABLE_PRODUCTS (
                    $COLUMN_PRODUCT_ID_PK TEXT PRIMARY KEY,
                    $COLUMN_PRODUCT_NAME TEXT NOT NULL,
                    $COLUMN_PRODUCT_CATEGORY TEXT NOT NULL,
                    $COLUMN_PRODUCT_PRICE REAL NOT NULL CHECK($COLUMN_PRODUCT_PRICE >= 0),
                    $COLUMN_PRODUCT_AGE_RESTRICTION INTEGER DEFAULT 0,
                    $COLUMN_PRODUCT_IMAGE_URL TEXT,
                    $COLUMN_PRODUCT_VIDEO_FILENAME TEXT,
                    $COLUMN_PRODUCT_IS_DIGITAL INTEGER DEFAULT 0,
                    $COLUMN_PRODUCT_ACTIVE INTEGER DEFAULT 1,
                    $COLUMN_PRODUCT_CREATED_AT INTEGER DEFAULT (strftime('%s', 'now')),
                    $COLUMN_PRODUCT_UPDATED_AT INTEGER DEFAULT (strftime('%s', 'now'))
                )
            """.trimIndent()

            db.execSQL(createProductsTable)
            Log.d(TAG, "Created products table")

            // Create product-coil assignments table
            val createAssignmentsTable = """
                CREATE TABLE IF NOT EXISTS $TABLE_PRODUCT_COIL_ASSIGNMENTS (
                    $COLUMN_ASSIGNMENT_ID TEXT PRIMARY KEY,
                    $COLUMN_ASSIGNMENT_PRODUCT_ID TEXT NOT NULL,
                    $COLUMN_ASSIGNMENT_COIL_ID TEXT NOT NULL,
                    $COLUMN_ASSIGNMENT_PRIORITY INTEGER DEFAULT 0,
                    $COLUMN_ASSIGNMENT_CREATED_AT INTEGER DEFAULT (strftime('%s', 'now')),
                    FOREIGN KEY ($COLUMN_ASSIGNMENT_PRODUCT_ID) REFERENCES $TABLE_PRODUCTS($COLUMN_PRODUCT_ID_PK) ON DELETE CASCADE,
                    FOREIGN KEY ($COLUMN_ASSIGNMENT_COIL_ID) REFERENCES $TABLE_COILS($COLUMN_COIL_ID) ON DELETE CASCADE,
                    UNIQUE ($COLUMN_ASSIGNMENT_PRODUCT_ID, $COLUMN_ASSIGNMENT_COIL_ID)
                )
            """.trimIndent()

            db.execSQL(createAssignmentsTable)
            Log.d(TAG, "Created product_coil_assignments table")

            // Create indexes for product tables
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_products_category ON $TABLE_PRODUCTS($COLUMN_PRODUCT_CATEGORY)")
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_products_active ON $TABLE_PRODUCTS($COLUMN_PRODUCT_ACTIVE)")
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_assignments_product ON $TABLE_PRODUCT_COIL_ASSIGNMENTS($COLUMN_ASSIGNMENT_PRODUCT_ID)")
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_assignments_coil ON $TABLE_PRODUCT_COIL_ASSIGNMENTS($COLUMN_ASSIGNMENT_COIL_ID)")

            // Seed initial product data
            seedProductData(db)
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
     * Seed initial product data (11 products from ProductGridActivity)
     * This mirrors the backend database seed in 002_payment_and_products.sql
     */
    private fun seedProductData(db: SQLiteDatabase) {
        Log.d(TAG, "Seeding initial product data")

        db.beginTransaction()
        try {
            // ZyNS Products (age restricted, 21+)
            val zynsProducts = listOf(
                arrayOf("PRD001", "ZYN CITRUS", "ZyNS", 3.50, 21, "A1", 0),
                arrayOf("PRD002", "ZYN COOL MINT", "ZyNS", 3.50, 21, "B1", 0),
                arrayOf("PRD003", "ZYN WINTERGREEN", "ZyNS", 3.50, 21, "C1", 0),
                arrayOf("PRD004", "ZYN SMOOTH", "ZyNS", 3.50, 21, "D1", 0),
                arrayOf("PRD005", "ZYN PEPPERMINT", "ZyNS", 3.50, 21, "E1", 0),
                arrayOf("PRD006", "ZYN CINNAMON", "ZyNS", 3.50, 21, "F1", 0),
                arrayOf("PRD007", "ZYN COFFEE", "ZyNS", 3.50, 21, "G1", 0),
                arrayOf("PRD008", "ZYN SPEARMINT", "ZyNS", 3.50, 21, "H1", 0)
            )

            // Extras (no age restriction)
            val extrasProducts = listOf(
                arrayOf("PRD009", "BOTTLE WATER", "Extras", 1.50, 0, "I1", 0),
                arrayOf("PRD010", "CANDY", "Extras", 1.00, 0, "J1", 0)
            )

            // Donations (digital/virtual - no coil assignment)
            val donationProducts = listOf(
                arrayOf("PRD011", "TIP DONATION", "Donations", 0.50, 0, null, null)
            )

            val allProducts = zynsProducts + extrasProducts + donationProducts
            val timestamp = System.currentTimeMillis() / 1000

            allProducts.forEach { product ->
                val productId = product[0] as String
                val name = product[1] as String
                val category = product[2] as String
                val price = product[3] as Double
                val ageRestriction = product[4] as Int
                val coilId = product.getOrNull(5) as? String
                val priority = product.getOrNull(6) as? Int

                // Insert product
                val productValues = ContentValues().apply {
                    put(COLUMN_PRODUCT_ID_PK, productId)
                    put(COLUMN_PRODUCT_NAME, name)
                    put(COLUMN_PRODUCT_CATEGORY, category)
                    put(COLUMN_PRODUCT_PRICE, price)
                    put(COLUMN_PRODUCT_AGE_RESTRICTION, ageRestriction)
                    put(COLUMN_PRODUCT_IS_DIGITAL, if (category == "Donations") 1 else 0)
                    put(COLUMN_PRODUCT_ACTIVE, 1)
                    put(COLUMN_PRODUCT_CREATED_AT, timestamp)
                    put(COLUMN_PRODUCT_UPDATED_AT, timestamp)
                }

                val productResult = db.insertWithOnConflict(
                    TABLE_PRODUCTS,
                    null,
                    productValues,
                    SQLiteDatabase.CONFLICT_IGNORE
                )

                if (productResult != -1L) {
                    Log.d(TAG, "Inserted product: $name ($productId) at $${price}")
                }

                // Insert product-coil assignment (if physical product)
                if (coilId != null && priority != null) {
                    val assignmentValues = ContentValues().apply {
                        put(COLUMN_ASSIGNMENT_ID, "ASSIGN${productId.substring(3)}") // ASSIGN001, etc.
                        put(COLUMN_ASSIGNMENT_PRODUCT_ID, productId)
                        put(COLUMN_ASSIGNMENT_COIL_ID, coilId)
                        put(COLUMN_ASSIGNMENT_PRIORITY, priority)
                        put(COLUMN_ASSIGNMENT_CREATED_AT, timestamp)
                    }

                    val assignmentResult = db.insertWithOnConflict(
                        TABLE_PRODUCT_COIL_ASSIGNMENTS,
                        null,
                        assignmentValues,
                        SQLiteDatabase.CONFLICT_IGNORE
                    )

                    if (assignmentResult != -1L) {
                        Log.d(TAG, "Assigned $productId to coil $coilId")
                    }
                }
            }

            db.setTransactionSuccessful()
            Log.d(TAG, "Successfully seeded ${allProducts.size} products")
        } catch (e: Exception) {
            Log.e(TAG, "Error seeding product data", e)
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
            db.execSQL("DELETE FROM $TABLE_PRODUCT_COIL_ASSIGNMENTS")
            db.execSQL("DELETE FROM $TABLE_PRODUCTS")
            db.execSQL("DELETE FROM $TABLE_COILS")
            seedInitialData(db)
            seedProductData(db)
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
