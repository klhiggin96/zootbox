package db

import (
	"database/sql"
	"fmt"
	"os"
	"path/filepath"

	_ "modernc.org/sqlite" // Pure-Go SQLite driver (no CGO required)
	"github.com/rs/zerolog/log"
)

// Connect establishes a connection to the SQLite database
// and configures it for optimal performance with WAL mode
func Connect(dbPath string) (*sql.DB, error) {
	// Ensure directory exists
	dir := filepath.Dir(dbPath)
	if err := os.MkdirAll(dir, 0755); err != nil {
		return nil, fmt.Errorf("failed to create database directory: %w", err)
	}

	// Open database connection
	db, err := sql.Open("sqlite", dbPath)
	if err != nil {
		return nil, fmt.Errorf("failed to open database: %w", err)
	}

	// Configure database with optimizations
	// - WAL mode for concurrent reads/writes
	// - busy_timeout prevents immediate failures on lock contention
	// - foreign_keys enforces referential integrity
	pragmas := []string{
		"PRAGMA journal_mode=WAL",
		"PRAGMA busy_timeout=5000",
		"PRAGMA foreign_keys=ON",
		"PRAGMA synchronous=NORMAL",
	}
	for _, pragma := range pragmas {
		if _, err := db.Exec(pragma); err != nil {
			db.Close()
			return nil, fmt.Errorf("failed to execute %s: %w", pragma, err)
		}
	}

	// Set connection pool limits
	// Single connection prevents lock contention on write-heavy workload
	// WAL mode allows concurrent reads even with single write connection
	db.SetMaxOpenConns(10)
	db.SetMaxIdleConns(5)

	// Verify connection
	if err := db.Ping(); err != nil {
		db.Close()
		return nil, fmt.Errorf("failed to ping database: %w", err)
	}

	log.Info().
		Str("db_path", dbPath).
		Msg("SQLite database connection established with WAL mode")

	return db, nil
}
