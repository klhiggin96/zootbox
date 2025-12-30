package repositories

import (
	"database/sql"
	"testing"
	"time"

	_ "modernc.org/sqlite"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"
	"github.com/zootbox/backend/internal/models"
)

func setupJamEventTestDB(t *testing.T) *sql.DB {
	db, err := sql.Open("sqlite", ":memory:")
	require.NoError(t, err)

	schema := `
	CREATE TABLE coils (
		id TEXT PRIMARY KEY,
		inventory INTEGER NOT NULL DEFAULT 10,
		status TEXT NOT NULL DEFAULT 'available',
		version INTEGER NOT NULL DEFAULT 1,
		link_group_id TEXT,
		updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
	);

	CREATE TABLE jam_events (
		id TEXT PRIMARY KEY,
		coil_id TEXT NOT NULL,
		timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
		status TEXT NOT NULL DEFAULT 'open' CHECK (status IN ('open', 'resolved')),
		resolved_at TIMESTAMP,
		FOREIGN KEY (coil_id) REFERENCES coils(id)
	);

	-- Seed test coils
	INSERT INTO coils (id, inventory, status, version) VALUES ('A1', 10, 'available', 1);
	INSERT INTO coils (id, inventory, status, version) VALUES ('B1', 5, 'jammed', 1);
	INSERT INTO coils (id, inventory, status, version) VALUES ('C1', 8, 'available', 1);
	`
	_, err = db.Exec(schema)
	require.NoError(t, err)

	return db
}

func TestJamEventRepository_Create(t *testing.T) {
	db := setupJamEventTestDB(t)
	defer db.Close()

	repo := NewJamEventRepository(db)

	tx, err := db.Begin()
	require.NoError(t, err)
	defer tx.Rollback()

	jamEvent := &models.JamEvent{
		ID:        "jam-001",
		CoilID:    "A1",
		Timestamp: time.Now(),
		Status:    models.JamEventStatusOpen,
	}

	err = repo.Create(tx, jamEvent)
	require.NoError(t, err)

	tx.Commit()

	// Verify jam event was created
	var count int
	err = db.QueryRow("SELECT COUNT(*) FROM jam_events WHERE id = ?", "jam-001").Scan(&count)
	require.NoError(t, err)
	assert.Equal(t, 1, count)
}

func TestJamEventRepository_GetByID_Found(t *testing.T) {
	db := setupJamEventTestDB(t)
	defer db.Close()

	// Insert test jam event
	now := time.Now()
	_, err := db.Exec(`
		INSERT INTO jam_events (id, coil_id, timestamp, status)
		VALUES ('jam-123', 'B1', ?, 'open')
	`, now)
	require.NoError(t, err)

	repo := NewJamEventRepository(db)

	event, err := repo.GetByID("jam-123")
	require.NoError(t, err)
	assert.Equal(t, "jam-123", event.ID)
	assert.Equal(t, "B1", event.CoilID)
	assert.Equal(t, models.JamEventStatusOpen, event.Status)
	assert.Nil(t, event.ResolvedAt)
}

func TestJamEventRepository_GetByID_NotFound(t *testing.T) {
	db := setupJamEventTestDB(t)
	defer db.Close()

	repo := NewJamEventRepository(db)

	_, err := repo.GetByID("nonexistent")
	require.Error(t, err)
	assert.Contains(t, err.Error(), "not found")
}

func TestJamEventRepository_GetByID_Resolved(t *testing.T) {
	db := setupJamEventTestDB(t)
	defer db.Close()

	// Insert resolved jam event
	now := time.Now()
	resolvedAt := now.Add(5 * time.Minute)
	_, err := db.Exec(`
		INSERT INTO jam_events (id, coil_id, timestamp, status, resolved_at)
		VALUES ('jam-456', 'C1', ?, 'resolved', ?)
	`, now, resolvedAt)
	require.NoError(t, err)

	repo := NewJamEventRepository(db)

	event, err := repo.GetByID("jam-456")
	require.NoError(t, err)
	assert.Equal(t, "jam-456", event.ID)
	assert.Equal(t, "C1", event.CoilID)
	assert.Equal(t, models.JamEventStatusResolved, event.Status)
	assert.NotNil(t, event.ResolvedAt)
}

func TestJamEventRepository_GetAll(t *testing.T) {
	db := setupJamEventTestDB(t)
	defer db.Close()

	// Insert multiple jam events
	now := time.Now()
	_, err := db.Exec(`
		INSERT INTO jam_events (id, coil_id, timestamp, status) VALUES
		('jam-1', 'A1', ?, 'open'),
		('jam-2', 'B1', ?, 'resolved'),
		('jam-3', 'C1', ?, 'open')
	`, now.Add(-2*time.Hour), now.Add(-1*time.Hour), now)
	require.NoError(t, err)

	repo := NewJamEventRepository(db)

	events, err := repo.GetAll()
	require.NoError(t, err)
	assert.Len(t, events, 3)
	// Should be ordered by timestamp DESC (most recent first)
	assert.Equal(t, "jam-3", events[0].ID)
	assert.Equal(t, "jam-2", events[1].ID)
	assert.Equal(t, "jam-1", events[2].ID)
}

func TestJamEventRepository_GetByStatus_Open(t *testing.T) {
	db := setupJamEventTestDB(t)
	defer db.Close()

	// Insert jam events with different statuses
	now := time.Now()
	_, err := db.Exec(`
		INSERT INTO jam_events (id, coil_id, timestamp, status) VALUES
		('jam-open-1', 'A1', ?, 'open'),
		('jam-open-2', 'B1', ?, 'open'),
		('jam-resolved-1', 'C1', ?, 'resolved')
	`, now.Add(-2*time.Hour), now.Add(-1*time.Hour), now)
	require.NoError(t, err)

	repo := NewJamEventRepository(db)

	events, err := repo.GetByStatus(models.JamEventStatusOpen)
	require.NoError(t, err)
	assert.Len(t, events, 2)
	assert.Equal(t, models.JamEventStatusOpen, events[0].Status)
	assert.Equal(t, models.JamEventStatusOpen, events[1].Status)
}

func TestJamEventRepository_GetByStatus_Resolved(t *testing.T) {
	db := setupJamEventTestDB(t)
	defer db.Close()

	// Insert jam events with different statuses
	now := time.Now()
	_, err := db.Exec(`
		INSERT INTO jam_events (id, coil_id, timestamp, status) VALUES
		('jam-open-1', 'A1', ?, 'open'),
		('jam-resolved-1', 'B1', ?, 'resolved'),
		('jam-resolved-2', 'C1', ?, 'resolved')
	`, now.Add(-3*time.Hour), now.Add(-2*time.Hour), now.Add(-1*time.Hour))
	require.NoError(t, err)

	repo := NewJamEventRepository(db)

	events, err := repo.GetByStatus(models.JamEventStatusResolved)
	require.NoError(t, err)
	assert.Len(t, events, 2)
	assert.Equal(t, models.JamEventStatusResolved, events[0].Status)
	assert.Equal(t, models.JamEventStatusResolved, events[1].Status)
}

func TestJamEventRepository_Resolve_Success(t *testing.T) {
	db := setupJamEventTestDB(t)
	defer db.Close()

	// Insert open jam event
	now := time.Now()
	_, err := db.Exec(`
		INSERT INTO jam_events (id, coil_id, timestamp, status)
		VALUES ('jam-to-resolve', 'A1', ?, 'open')
	`, now)
	require.NoError(t, err)

	repo := NewJamEventRepository(db)

	err = repo.Resolve("jam-to-resolve")
	require.NoError(t, err)

	// Verify status changed to resolved
	var status string
	var resolvedAt sql.NullTime
	err = db.QueryRow(`
		SELECT status, resolved_at FROM jam_events WHERE id = ?
	`, "jam-to-resolve").Scan(&status, &resolvedAt)
	require.NoError(t, err)
	assert.Equal(t, models.JamEventStatusResolved, status)
	assert.True(t, resolvedAt.Valid)
}

func TestJamEventRepository_Resolve_NotFound(t *testing.T) {
	db := setupJamEventTestDB(t)
	defer db.Close()

	repo := NewJamEventRepository(db)

	err := repo.Resolve("nonexistent")
	require.Error(t, err)
	assert.Contains(t, err.Error(), "not found")
}

func TestJamEventRepository_Resolve_AlreadyResolved(t *testing.T) {
	db := setupJamEventTestDB(t)
	defer db.Close()

	// Insert already resolved jam event
	now := time.Now()
	_, err := db.Exec(`
		INSERT INTO jam_events (id, coil_id, timestamp, status, resolved_at)
		VALUES ('jam-already-resolved', 'A1', ?, 'resolved', ?)
	`, now, now.Add(5*time.Minute))
	require.NoError(t, err)

	repo := NewJamEventRepository(db)

	err = repo.Resolve("jam-already-resolved")
	require.Error(t, err)
	assert.Contains(t, err.Error(), "already resolved")
}

func TestJamEventRepository_GetAll_EmptyTable(t *testing.T) {
	db := setupJamEventTestDB(t)
	defer db.Close()

	repo := NewJamEventRepository(db)

	events, err := repo.GetAll()
	require.NoError(t, err)
	assert.Empty(t, events)
}

func TestJamEventRepository_GetByStatus_NoMatches(t *testing.T) {
	db := setupJamEventTestDB(t)
	defer db.Close()

	// Insert only open events
	now := time.Now()
	_, err := db.Exec(`
		INSERT INTO jam_events (id, coil_id, timestamp, status)
		VALUES ('jam-open-1', 'A1', ?, 'open')
	`, now)
	require.NoError(t, err)

	repo := NewJamEventRepository(db)

	// Query for resolved events (should return empty)
	events, err := repo.GetByStatus(models.JamEventStatusResolved)
	require.NoError(t, err)
	assert.Empty(t, events)
}

func TestJamEventRepository_Create_WithinTransaction(t *testing.T) {
	db := setupJamEventTestDB(t)
	defer db.Close()

	repo := NewJamEventRepository(db)

	// Create two jam events in one transaction
	tx, err := db.Begin()
	require.NoError(t, err)

	jamEvent1 := &models.JamEvent{
		ID:        "jam-tx-001",
		CoilID:    "A1",
		Timestamp: time.Now(),
		Status:    models.JamEventStatusOpen,
	}

	jamEvent2 := &models.JamEvent{
		ID:        "jam-tx-002",
		CoilID:    "B1",
		Timestamp: time.Now(),
		Status:    models.JamEventStatusOpen,
	}

	err = repo.Create(tx, jamEvent1)
	require.NoError(t, err)

	err = repo.Create(tx, jamEvent2)
	require.NoError(t, err)

	tx.Commit()

	// Verify both were created
	var count int
	err = db.QueryRow("SELECT COUNT(*) FROM jam_events WHERE id IN ('jam-tx-001', 'jam-tx-002')").Scan(&count)
	require.NoError(t, err)
	assert.Equal(t, 2, count)
}

func TestJamEventRepository_Create_RollbackOnError(t *testing.T) {
	db := setupJamEventTestDB(t)
	defer db.Close()

	repo := NewJamEventRepository(db)

	tx, err := db.Begin()
	require.NoError(t, err)
	defer tx.Rollback()

	jamEvent := &models.JamEvent{
		ID:        "jam-rollback",
		CoilID:    "A1",
		Timestamp: time.Now(),
		Status:    models.JamEventStatusOpen,
	}

	err = repo.Create(tx, jamEvent)
	require.NoError(t, err)

	// Rollback the transaction
	tx.Rollback()

	// Verify jam event was NOT created
	var count int
	err = db.QueryRow("SELECT COUNT(*) FROM jam_events WHERE id = 'jam-rollback'").Scan(&count)
	require.NoError(t, err)
	assert.Equal(t, 0, count)
}
