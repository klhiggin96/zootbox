package models

import "time"

// JamEvent represents a motor jam or failure event
type JamEvent struct {
	ID         string     `json:"id" db:"id"`                             // UUID
	CoilID     string     `json:"coil_id" db:"coil_id"`
	Timestamp  time.Time  `json:"timestamp" db:"timestamp"`
	Status     string     `json:"status" db:"status"`                     // open | resolved
	ResolvedAt *time.Time `json:"resolved_at,omitempty" db:"resolved_at"` // nullable
}

// JamEventStatus constants
const (
	JamEventStatusOpen     = "open"
	JamEventStatusResolved = "resolved"
)

// IsOpen returns true if jam event is still open
func (j *JamEvent) IsOpen() bool {
	return j.Status == JamEventStatusOpen
}
