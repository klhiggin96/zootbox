package db

import (
	"database/sql"
	"embed"
	"fmt"
	"sort"
	"strings"

	"github.com/rs/zerolog/log"
)

//go:embed migrations/*.sql
var migrationsFS embed.FS

// RunMigrations applies all pending database migrations
func RunMigrations(db *sql.DB) error {
	log.Info().Msg("Starting database migrations")

	// Read all migration files from embedded filesystem
	entries, err := migrationsFS.ReadDir("migrations")
	if err != nil {
		return fmt.Errorf("failed to read migrations directory: %w", err)
	}

	// Sort migration files by name (ensures 001, 002, 003 order)
	var migrationFiles []string
	for _, entry := range entries {
		if !entry.IsDir() && strings.HasSuffix(entry.Name(), ".sql") {
			migrationFiles = append(migrationFiles, entry.Name())
		}
	}
	sort.Strings(migrationFiles)

	log.Info().Int("count", len(migrationFiles)).Msg("Found migration files")

	// Apply each migration
	for _, filename := range migrationFiles {
		migrationName := strings.TrimSuffix(filename, ".sql")

		// Check if already applied
		var count int
		err := db.QueryRow("SELECT COUNT(*) FROM migrations WHERE version = ?", migrationName).Scan(&count)
		if err != nil && !strings.Contains(err.Error(), "no such table") {
			return fmt.Errorf("failed to check migration status for %s: %w", migrationName, err)
		}

		if count > 0 {
			log.Debug().Str("migration", migrationName).Msg("Migration already applied, skipping")
			continue
		}

		// Read migration SQL
		// Use forward slashes for embedded filesystem (cross-platform)
		sqlBytes, err := migrationsFS.ReadFile("migrations/" + filename)
		if err != nil {
			return fmt.Errorf("failed to read migration %s: %w", filename, err)
		}

		// Execute migration in transaction
		tx, err := db.Begin()
		if err != nil {
			return fmt.Errorf("failed to begin transaction for %s: %w", migrationName, err)
		}

		log.Info().Str("migration", migrationName).Msg("Applying migration")

		if _, err := tx.Exec(string(sqlBytes)); err != nil {
			tx.Rollback()
			return fmt.Errorf("failed to execute migration %s: %w", migrationName, err)
		}

		if err := tx.Commit(); err != nil {
			return fmt.Errorf("failed to commit migration %s: %w", migrationName, err)
		}

		log.Info().Str("migration", migrationName).Msg("Migration applied successfully")
	}

	// Verify final schema
	var coilCount int
	if err := db.QueryRow("SELECT COUNT(*) FROM coils").Scan(&coilCount); err != nil {
		return fmt.Errorf("failed to verify coils table: %w", err)
	}

	log.Info().Int("coil_count", coilCount).Msg("Database migrations completed successfully")

	return nil
}
