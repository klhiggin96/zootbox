package repositories

import (
	"database/sql"
	"fmt"
	"time"

	"github.com/zootbox/backend/internal/models"
)

// JamEventRepository handles database operations for jam events
type JamEventRepository struct {
	db *sql.DB
}

// NewJamEventRepository creates a new jam event repository
func NewJamEventRepository(db *sql.DB) *JamEventRepository {
	return &JamEventRepository{db: db}
}

// Create inserts a new jam event
func (r *JamEventRepository) Create(tx *sql.Tx, jamEvent *models.JamEvent) error {
	query := `INSERT INTO jam_events (id, coil_id, timestamp, status)
	          VALUES (?, ?, ?, ?)`

	_, err := tx.Exec(
		query,
		jamEvent.ID,
		jamEvent.CoilID,
		jamEvent.Timestamp,
		jamEvent.Status,
	)

	if err != nil {
		return fmt.Errorf("failed to create jam event: %w", err)
	}

	return nil
}

// GetAll retrieves all jam events
func (r *JamEventRepository) GetAll() ([]*models.JamEvent, error) {
	query := `SELECT id, coil_id, timestamp, status, resolved_at
	          FROM jam_events
	          ORDER BY timestamp DESC`

	return r.query(query)
}

// GetByID retrieves a single jam event by ID
func (r *JamEventRepository) GetByID(eventID string) (*models.JamEvent, error) {
	query := `SELECT id, coil_id, timestamp, status, resolved_at
	          FROM jam_events
	          WHERE id = ?`

	var event models.JamEvent
	err := r.db.QueryRow(query, eventID).Scan(
		&event.ID,
		&event.CoilID,
		&event.Timestamp,
		&event.Status,
		&event.ResolvedAt,
	)
	if err == sql.ErrNoRows {
		return nil, fmt.Errorf("jam event %s not found", eventID)
	}
	if err != nil {
		return nil, fmt.Errorf("failed to get jam event: %w", err)
	}

	return &event, nil
}

// GetByStatus retrieves jam events filtered by status
func (r *JamEventRepository) GetByStatus(status string) ([]*models.JamEvent, error) {
	query := `SELECT id, coil_id, timestamp, status, resolved_at
	          FROM jam_events
	          WHERE status = ?
	          ORDER BY timestamp DESC`

	return r.queryWithArg(query, status)
}

// Resolve marks a jam event as resolved
func (r *JamEventRepository) Resolve(eventID string) error {
	query := `UPDATE jam_events
	          SET status = ?, resolved_at = ?
	          WHERE id = ? AND status = ?`

	result, err := r.db.Exec(query, models.JamEventStatusResolved, time.Now(), eventID, models.JamEventStatusOpen)
	if err != nil {
		return fmt.Errorf("failed to resolve jam event: %w", err)
	}

	rowsAffected, err := result.RowsAffected()
	if err != nil {
		return fmt.Errorf("failed to get rows affected: %w", err)
	}

	if rowsAffected == 0 {
		return fmt.Errorf("jam event %s not found or already resolved", eventID)
	}

	return nil
}

// Helper method to execute query and scan results
func (r *JamEventRepository) query(query string) ([]*models.JamEvent, error) {
	rows, err := r.db.Query(query)
	if err != nil {
		return nil, fmt.Errorf("failed to query jam events: %w", err)
	}
	defer rows.Close()

	return r.scanRows(rows)
}

// Helper method to execute query with argument and scan results
func (r *JamEventRepository) queryWithArg(query string, arg interface{}) ([]*models.JamEvent, error) {
	rows, err := r.db.Query(query, arg)
	if err != nil {
		return nil, fmt.Errorf("failed to query jam events: %w", err)
	}
	defer rows.Close()

	return r.scanRows(rows)
}

// Helper method to scan rows into JamEvent structs
func (r *JamEventRepository) scanRows(rows *sql.Rows) ([]*models.JamEvent, error) {
	var events []*models.JamEvent
	for rows.Next() {
		var event models.JamEvent
		err := rows.Scan(
			&event.ID,
			&event.CoilID,
			&event.Timestamp,
			&event.Status,
			&event.ResolvedAt,
		)
		if err != nil {
			return nil, fmt.Errorf("failed to scan jam event: %w", err)
		}
		events = append(events, &event)
	}

	return events, nil
}
