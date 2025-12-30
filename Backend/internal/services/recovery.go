package services

import (
	"database/sql"
	"encoding/json"
	"fmt"

	"github.com/rs/zerolog/log"
)

// RecoveryService handles power loss recovery and data integrity validation
type RecoveryService struct {
	db *sql.DB
}

// NewRecoveryService creates a new recovery service
func NewRecoveryService(db *sql.DB) *RecoveryService {
	return &RecoveryService{db: db}
}

// DataIntegrityReport represents the result of data integrity validation
type DataIntegrityReport struct {
	IsValid             bool   `json:"is_valid"`
	TotalCoils          int    `json:"total_coils"`
	CoilsWithInventory  int    `json:"coils_with_inventory"`
	TotalTransactions   int    `json:"total_transactions"`
	OpenJamEvents       int    `json:"open_jam_events"`
	ResolvedJamEvents   int    `json:"resolved_jam_events"`
	ProductLinks        int    `json:"product_links"`
	ValidationErrors    []string `json:"validation_errors,omitempty"`
}

// ValidateDataIntegrity performs comprehensive data integrity checks
func (s *RecoveryService) ValidateDataIntegrity() (*DataIntegrityReport, error) {
	log.Info().Msg("Starting data integrity validation")

	report := &DataIntegrityReport{
		IsValid:          true,
		ValidationErrors: []string{},
	}

	// Check 1: Verify exactly 10 coils exist (A1-J1)
	var coilCount int
	if err := s.db.QueryRow("SELECT COUNT(*) FROM coils").Scan(&coilCount); err != nil {
		return nil, fmt.Errorf("failed to count coils: %w", err)
	}
	report.TotalCoils = coilCount

	if coilCount != 10 {
		report.IsValid = false
		report.ValidationErrors = append(report.ValidationErrors, fmt.Sprintf("Expected 10 coils, found %d", coilCount))
	}

	// Check 2: Verify all coils have valid inventory (0-10)
	var invalidInventoryCount int
	if err := s.db.QueryRow("SELECT COUNT(*) FROM coils WHERE inventory < 0 OR inventory > 10").Scan(&invalidInventoryCount); err != nil {
		return nil, fmt.Errorf("failed to check inventory bounds: %w", err)
	}

	if invalidInventoryCount > 0 {
		report.IsValid = false
		report.ValidationErrors = append(report.ValidationErrors, fmt.Sprintf("Found %d coils with invalid inventory (must be 0-10)", invalidInventoryCount))
	}

	// Count coils with non-zero inventory
	if err := s.db.QueryRow("SELECT COUNT(*) FROM coils WHERE inventory > 0").Scan(&report.CoilsWithInventory); err != nil {
		return nil, fmt.Errorf("failed to count coils with inventory: %w", err)
	}

	// Check 3: Verify all transactions reference valid coils
	var orphanedTransactions int
	query := `
		SELECT COUNT(*)
		FROM transactions t
		WHERE NOT EXISTS (SELECT 1 FROM coils c WHERE c.id = t.coil_id)
	`
	if err := s.db.QueryRow(query).Scan(&orphanedTransactions); err != nil {
		return nil, fmt.Errorf("failed to check orphaned transactions: %w", err)
	}

	if orphanedTransactions > 0 {
		report.IsValid = false
		report.ValidationErrors = append(report.ValidationErrors, fmt.Sprintf("Found %d transactions referencing non-existent coils", orphanedTransactions))
	}

	// Count total transactions
	if err := s.db.QueryRow("SELECT COUNT(*) FROM transactions").Scan(&report.TotalTransactions); err != nil {
		return nil, fmt.Errorf("failed to count transactions: %w", err)
	}

	// Check 4: Verify all jam events reference valid coils
	var orphanedJamEvents int
	query = `
		SELECT COUNT(*)
		FROM jam_events j
		WHERE NOT EXISTS (SELECT 1 FROM coils c WHERE c.id = j.coil_id)
	`
	if err := s.db.QueryRow(query).Scan(&orphanedJamEvents); err != nil {
		return nil, fmt.Errorf("failed to check orphaned jam events: %w", err)
	}

	if orphanedJamEvents > 0 {
		report.IsValid = false
		report.ValidationErrors = append(report.ValidationErrors, fmt.Sprintf("Found %d jam events referencing non-existent coils", orphanedJamEvents))
	}

	// Count jam events by status
	if err := s.db.QueryRow("SELECT COUNT(*) FROM jam_events WHERE status = 'open'").Scan(&report.OpenJamEvents); err != nil {
		return nil, fmt.Errorf("failed to count open jam events: %w", err)
	}

	if err := s.db.QueryRow("SELECT COUNT(*) FROM jam_events WHERE status = 'resolved'").Scan(&report.ResolvedJamEvents); err != nil {
		return nil, fmt.Errorf("failed to count resolved jam events: %w", err)
	}

	// Check 5: Verify all product links reference valid coils
	// Parse linked_coil_ids JSON arrays and validate each coil_id exists
	rows, err := s.db.Query("SELECT link_group_id, linked_coil_ids FROM product_links")
	if err != nil {
		return nil, fmt.Errorf("failed to query product links: %w", err)
	}
	defer rows.Close()

	productLinkCount := 0
	for rows.Next() {
		var linkGroupID, linkedCoilIDsJSON string
		if err := rows.Scan(&linkGroupID, &linkedCoilIDsJSON); err != nil {
			return nil, fmt.Errorf("failed to scan product link: %w", err)
		}
		productLinkCount++

		// Parse JSON array of coil IDs
		var coilIDs []string
		if err := json.Unmarshal([]byte(linkedCoilIDsJSON), &coilIDs); err != nil {
			report.IsValid = false
			report.ValidationErrors = append(report.ValidationErrors,
				fmt.Sprintf("Product link %s has invalid JSON: %v", linkGroupID, err))
			continue
		}

		// Verify each coil_id exists
		for _, coilID := range coilIDs {
			var exists int
			err := s.db.QueryRow("SELECT COUNT(*) FROM coils WHERE id = ?", coilID).Scan(&exists)
			if err != nil {
				return nil, fmt.Errorf("failed to check coil existence: %w", err)
			}
			if exists == 0 {
				report.IsValid = false
				report.ValidationErrors = append(report.ValidationErrors,
					fmt.Sprintf("Product link %s references non-existent coil %s", linkGroupID, coilID))
			}
		}
	}
	if err := rows.Err(); err != nil {
		return nil, fmt.Errorf("error iterating product links: %w", err)
	}
	report.ProductLinks = productLinkCount

	// Check 6: Verify database schema integrity (WAL mode)
	var journalMode string
	if err := s.db.QueryRow("PRAGMA journal_mode").Scan(&journalMode); err != nil {
		return nil, fmt.Errorf("failed to check journal mode: %w", err)
	}

	if journalMode != "wal" {
		report.IsValid = false
		report.ValidationErrors = append(report.ValidationErrors, fmt.Sprintf("Expected WAL journal mode, found %s", journalMode))
	}

	if report.IsValid {
		log.Info().
			Int("coils", report.TotalCoils).
			Int("transactions", report.TotalTransactions).
			Int("open_jams", report.OpenJamEvents).
			Int("product_links", report.ProductLinks).
			Msg("Data integrity validation PASSED")
	} else {
		log.Warn().
			Int("error_count", len(report.ValidationErrors)).
			Strs("errors", report.ValidationErrors).
			Msg("Data integrity validation FAILED")
	}

	return report, nil
}

// RecoverFromPowerLoss performs recovery operations after power loss
func (s *RecoveryService) RecoverFromPowerLoss() error {
	log.Info().Msg("Performing power loss recovery")

	// Run integrity validation
	report, err := s.ValidateDataIntegrity()
	if err != nil {
		return fmt.Errorf("integrity validation failed: %w", err)
	}

	if !report.IsValid {
		log.Error().
			Int("error_count", len(report.ValidationErrors)).
			Strs("errors", report.ValidationErrors).
			Msg("Database corruption detected")
		return fmt.Errorf("database corruption detected: %d errors", len(report.ValidationErrors))
	}

	// SQLite WAL mode provides automatic recovery for uncommitted transactions
	// No manual rollback needed - uncommitted transactions are automatically discarded
	log.Info().Msg("Power loss recovery completed successfully")

	return nil
}
