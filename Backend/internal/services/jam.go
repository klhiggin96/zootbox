package services

import (
	"database/sql"
	"fmt"

	"github.com/rs/zerolog/log"
	"github.com/zootbox/backend/internal/db/repositories"
	"github.com/zootbox/backend/internal/models"
)

// JamService handles jam event operations
type JamService struct {
	db           *sql.DB
	jamEventRepo *repositories.JamEventRepository
}

// NewJamService creates a new jam service
func NewJamService(db *sql.DB) *JamService {
	return &JamService{
		db:           db,
		jamEventRepo: repositories.NewJamEventRepository(db),
	}
}

// GetJamEvents retrieves jam events, optionally filtered by status
func (s *JamService) GetJamEvents(status string) ([]*models.JamEvent, error) {
	if status == "" {
		// Return all jam events
		return s.jamEventRepo.GetAll()
	}

	// Validate status
	if status != models.JamEventStatusOpen && status != models.JamEventStatusResolved {
		return nil, fmt.Errorf("invalid status: must be 'open' or 'resolved', got '%s'", status)
	}

	// Return filtered jam events
	return s.jamEventRepo.GetByStatus(status)
}

// ResolveJamEvent marks a jam event as resolved
func (s *JamService) ResolveJamEvent(eventID string) (*models.JamEvent, error) {
	log.Info().Str("jam_event_id", eventID).Msg("Resolving jam event")

	// Resolve the jam event
	if err := s.jamEventRepo.Resolve(eventID); err != nil {
		return nil, fmt.Errorf("failed to resolve jam event: %w", err)
	}

	// Fetch all jam events to find the resolved one
	// Note: This is inefficient but works for MVP. In production, add GetByID to repository
	allEvents, err := s.jamEventRepo.GetAll()
	if err != nil {
		return nil, fmt.Errorf("failed to fetch resolved jam event: %w", err)
	}

	// Find the resolved event
	for _, event := range allEvents {
		if event.ID == eventID {
			log.Info().
				Str("jam_event_id", eventID).
				Str("coil_id", event.CoilID).
				Msg("Jam event resolved successfully")
			return event, nil
		}
	}

	return nil, fmt.Errorf("jam event %s not found after resolution", eventID)
}
