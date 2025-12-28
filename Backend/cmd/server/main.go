package main

import (
	"context"
	"flag"
	"fmt"
	"os"
	"os/signal"
	"syscall"
	"time"

	"github.com/rs/zerolog"
	"github.com/rs/zerolog/log"
	"github.com/zootbox/backend/internal/api"
	"github.com/zootbox/backend/internal/config"
	"github.com/zootbox/backend/internal/db"
)

func main() {
	// Parse CLI flags
	var (
		logLevel  = flag.String("log-level", "info", "Log level (debug, info, warn, error)")
		configPath = flag.String("config", "", "Path to configuration file")
		migrate    = flag.Bool("migrate", false, "Run database migrations and exit")
	)
	flag.Parse()

	// Configure logging
	setupLogging(*logLevel)

	log.Info().Msg("Starting ZootBox Backend Inventory Service")

	// Load configuration
	cfg, err := config.Load(*configPath)
	if err != nil {
		log.Fatal().Err(err).Msg("Failed to load configuration")
	}

	log.Info().
		Str("db_path", cfg.DBPath).
		Str("http_host", cfg.HTTPHost).
		Int("http_port", cfg.HTTPPort).
		Msg("Configuration loaded")

	// Initialize database
	database, err := db.Connect(cfg.DBPath)
	if err != nil {
		log.Fatal().Err(err).Msg("Failed to connect to database")
	}
	defer database.Close()

	log.Info().Msg("Database connection established")

	// Run migrations if requested
	if *migrate {
		log.Info().Msg("Running database migrations")
		if err := db.RunMigrations(database); err != nil {
			log.Fatal().Err(err).Msg("Migration failed")
		}
		log.Info().Msg("Migrations completed successfully")
		return
	}

	// TODO: Run recovery validation
	// recovery := services.NewRecoveryService(database)
	// if err := recovery.ValidateDataIntegrity(); err != nil {
	//     log.Warn().Err(err).Msg("Recovery validation found issues")
	// }

	// Create HTTP server
	server := api.NewServer(cfg, database)

	// Start server in goroutine
	go func() {
		addr := fmt.Sprintf("%s:%d", cfg.HTTPHost, cfg.HTTPPort)
		log.Info().Str("address", addr).Msg("Starting HTTP server")
		if err := server.Start(addr); err != nil {
			log.Fatal().Err(err).Msg("Server failed to start")
		}
	}()

	// Wait for interrupt signal
	quit := make(chan os.Signal, 1)
	signal.Notify(quit, syscall.SIGINT, syscall.SIGTERM)
	<-quit

	log.Info().Msg("Shutting down server...")

	// Graceful shutdown with timeout
	ctx, cancel := context.WithTimeout(context.Background(), 10*time.Second)
	defer cancel()

	if err := server.Shutdown(ctx); err != nil {
		log.Error().Err(err).Msg("Server forced to shutdown")
	}

	log.Info().Msg("Server exited")
}

func setupLogging(level string) {
	// Parse log level
	logLevel, err := zerolog.ParseLevel(level)
	if err != nil {
		logLevel = zerolog.InfoLevel
	}
	zerolog.SetGlobalLevel(logLevel)

	// Configure human-friendly console output for development
	log.Logger = log.Output(zerolog.ConsoleWriter{
		Out:        os.Stdout,
		TimeFormat: time.RFC3339,
	})
}
