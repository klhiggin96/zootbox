package api

import (
	"context"
	"database/sql"
	"net/http"
	"time"

	"github.com/zootbox/backend/internal/config"
)

// Server wraps the HTTP server with dependencies
type Server struct {
	config *config.Config
	db     *sql.DB
	server *http.Server
}

// NewServer creates a new HTTP server instance
func NewServer(cfg *config.Config, db *sql.DB) *Server {
	router := NewRouter(db)

	return &Server{
		config: cfg,
		db:     db,
		server: &http.Server{
			Handler:      router,
			ReadTimeout:  10 * time.Second,
			WriteTimeout: 10 * time.Second,
			IdleTimeout:  60 * time.Second,
		},
	}
}

// Start begins listening for HTTP requests
func (s *Server) Start(addr string) error {
	s.server.Addr = addr
	return s.server.ListenAndServe()
}

// Shutdown gracefully shuts down the server
func (s *Server) Shutdown(ctx context.Context) error {
	return s.server.Shutdown(ctx)
}
