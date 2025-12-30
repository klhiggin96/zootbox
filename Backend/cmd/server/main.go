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
	"github.com/zootbox/backend/internal/profiling"
	"github.com/zootbox/backend/internal/services"
)

func main() {
	// Parse CLI flags
	var (
		logLevel    = flag.String("log-level", "info", "Log level (debug, info, warn, error)")
		configPath  = flag.String("config", "", "Path to configuration file")
		migrate     = flag.Bool("migrate", false, "Run database migrations and exit")
		cpuProfile  = flag.String("cpuprofile", "", "Write CPU profile to file")
		memProfile  = flag.String("memprofile", "", "Write memory profile to file on exit")
		memMonitor  = flag.Bool("memmonitor", false, "Enable periodic memory monitoring")
	)
	flag.Parse()

	// Configure logging
	setupLogging(*logLevel)

	log.Info().Msg("Starting ZootBox Backend Inventory Service")

	// CPU profiling
	if *cpuProfile != "" {
		stopCPU, err := profiling.StartCPUProfile(*cpuProfile)
		if err != nil {
			log.Fatal().Err(err).Msg("Failed to start CPU profiling")
		}
		defer stopCPU()
	}

	// Memory profiling on exit
	if *memProfile != "" {
		defer func() {
			if err := profiling.MemoryProfile(*memProfile); err != nil {
				log.Error().Err(err).Msg("Failed to write memory profile")
			}
		}()
	}

	// Memory monitoring
	if *memMonitor {
		profiling.MonitorMemory(30*time.Second, 25.0) // Alert if > 25MB
		log.Info().Msg("Memory monitoring enabled (30s interval, 25MB threshold)")
	}

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

	// Run power loss recovery validation
	log.Info().Msg("Running post-startup data integrity validation")
	recovery := services.NewRecoveryService(database)
	if err := recovery.RecoverFromPowerLoss(); err != nil {
		log.Fatal().Err(err).Msg("Power loss recovery failed - database corruption detected")
	}
	log.Info().Msg("Data integrity validation passed")

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

	// Configure logging format based on environment
	logFormat := os.Getenv("LOG_FORMAT")
	if logFormat == "console" {
		// Development: Human-friendly console output
		log.Logger = log.Output(zerolog.ConsoleWriter{
			Out:        os.Stdout,
			TimeFormat: time.RFC3339,
		})
	} else {
		// Production: JSON structured logs
		zerolog.TimeFieldFormat = zerolog.TimeFormatUnix
		log.Logger = zerolog.New(os.Stdout).With().Timestamp().Logger()
	}
}
