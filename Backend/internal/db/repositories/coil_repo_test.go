package repositories

import (
	"database/sql"
	"testing"

	_ "modernc.org/sqlite"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"
	"github.com/zootbox/backend/internal/models"
)

func setupTestDB(t *testing.T) *sql.DB {
	// Create in-memory SQLite database for testing
	db, err := sql.Open("sqlite", ":memory:")
	require.NoError(t, err, "Failed to open test database")

	// Create schema
	schema := `
	CREATE TABLE coils (
		id TEXT PRIMARY KEY,
		inventory INTEGER NOT NULL DEFAULT 10 CHECK (inventory >= 0 AND inventory <= 10),
		status TEXT NOT NULL DEFAULT 'available' CHECK (status IN ('available', 'jammed')),
		version INTEGER NOT NULL DEFAULT 1,
		link_group_id TEXT,
		updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
	);
	`
	_, err = db.Exec(schema)
	require.NoError(t, err, "Failed to create schema")

	return db
}

func seedTestCoils(t *testing.T, db *sql.DB) {
	// Seed a few test coils
	coils := []struct {
		id        string
		inventory int
		status    string
	}{
		{"A1", 10, "available"},
		{"A2", 5, "available"},
		{"A3", 2, "available"},
		{"A4", 0, "available"},
		{"A5", 8, "jammed"},
	}

	for _, coil := range coils {
		_, err := db.Exec(
			"INSERT INTO coils (id, inventory, status, version) VALUES (?, ?, ?, 1)",
			coil.id, coil.inventory, coil.status,
		)
		require.NoError(t, err, "Failed to seed coil %s", coil.id)
	}
}

func TestCoilRepository_GetByID(t *testing.T) {
	db := setupTestDB(t)
	defer db.Close()
	seedTestCoils(t, db)

	repo := NewCoilRepository(db)

	t.Run("returns coil when exists", func(t *testing.T) {
		coil, err := repo.GetByID("A1")
		require.NoError(t, err)
		assert.Equal(t, "A1", coil.ID)
		assert.Equal(t, 10, coil.Inventory)
		assert.Equal(t, models.CoilStatusAvailable, coil.Status)
		assert.Equal(t, 1, coil.Version)
	})

	t.Run("returns error when coil not found", func(t *testing.T) {
		_, err := repo.GetByID("Z99")
		require.Error(t, err)
		assert.Contains(t, err.Error(), "not found")
	})
}

func TestCoilRepository_GetAll(t *testing.T) {
	db := setupTestDB(t)
	defer db.Close()
	seedTestCoils(t, db)

	repo := NewCoilRepository(db)

	coils, err := repo.GetAll()
	require.NoError(t, err)
	assert.Len(t, coils, 5)
	assert.Equal(t, "A1", coils[0].ID) // Should be sorted by ID
}

func TestCoilRepository_GetLowStock(t *testing.T) {
	db := setupTestDB(t)
	defer db.Close()
	seedTestCoils(t, db)

	repo := NewCoilRepository(db)

	coils, err := repo.GetLowStock()
	require.NoError(t, err)
	assert.Len(t, coils, 2) // A3 (2) and A4 (0)

	// Should be sorted by inventory ASC
	assert.Equal(t, "A4", coils[0].ID)
	assert.Equal(t, 0, coils[0].Inventory)
	assert.Equal(t, "A3", coils[1].ID)
	assert.Equal(t, 2, coils[1].Inventory)
}

func TestCoilRepository_UpdateInventory(t *testing.T) {
	db := setupTestDB(t)
	defer db.Close()
	seedTestCoils(t, db)

	repo := NewCoilRepository(db)

	t.Run("decrements inventory with correct version", func(t *testing.T) {
		tx, err := db.Begin()
		require.NoError(t, err)
		defer tx.Rollback()

		err = repo.UpdateInventory(tx, "A1", 1)
		require.NoError(t, err)

		tx.Commit()

		// Verify inventory was decremented
		coil, err := repo.GetByID("A1")
		require.NoError(t, err)
		assert.Equal(t, 9, coil.Inventory)
		assert.Equal(t, 2, coil.Version) // Version incremented
	})

	t.Run("fails with version mismatch (optimistic locking)", func(t *testing.T) {
		tx, err := db.Begin()
		require.NoError(t, err)
		defer tx.Rollback()

		// Try to update with wrong version
		err = repo.UpdateInventory(tx, "A1", 999)
		require.Error(t, err)
		assert.Contains(t, err.Error(), "version conflict")
	})

	t.Run("fails when inventory is 0", func(t *testing.T) {
		tx, err := db.Begin()
		require.NoError(t, err)
		defer tx.Rollback()

		err = repo.UpdateInventory(tx, "A4", 1)
		require.Error(t, err)
		assert.Contains(t, err.Error(), "inventory is 0")
	})
}

func TestCoilRepository_RefillAll(t *testing.T) {
	db := setupTestDB(t)
	defer db.Close()
	seedTestCoils(t, db)

	repo := NewCoilRepository(db)

	tx, err := db.Begin()
	require.NoError(t, err)
	defer tx.Rollback()

	rowsAffected, err := repo.RefillAll(tx)
	require.NoError(t, err)
	assert.Equal(t, int64(5), rowsAffected)

	tx.Commit()

	// Verify all coils set to 10
	coils, err := repo.GetAll()
	require.NoError(t, err)
	for _, coil := range coils {
		assert.Equal(t, 10, coil.Inventory, "Coil %s should have inventory 10", coil.ID)
	}
}

func TestCoilRepository_UpdateInventoryManual(t *testing.T) {
	db := setupTestDB(t)
	defer db.Close()
	seedTestCoils(t, db)

	repo := NewCoilRepository(db)

	tx, err := db.Begin()
	require.NoError(t, err)
	defer tx.Rollback()

	err = repo.UpdateInventoryManual(tx, "A2", 7)
	require.NoError(t, err)

	tx.Commit()

	coil, err := repo.GetByID("A2")
	require.NoError(t, err)
	assert.Equal(t, 7, coil.Inventory)
	assert.Equal(t, 2, coil.Version) // Version incremented
}
