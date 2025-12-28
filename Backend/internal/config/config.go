package config

import (
	"os"
	"strconv"
)

// Config holds application configuration
type Config struct {
	// Database configuration
	DBPath string

	// HTTP server configuration
	HTTPHost string
	HTTPPort int

	// Monitoring configuration
	MonitoringURL  string
	MonitoringAuth string

	// Logging configuration
	LogLevel string
}

// Load reads configuration from environment variables with sensible defaults
func Load(configPath string) (*Config, error) {
	// TODO: If configPath provided, load from file
	// For now, use environment variables only

	cfg := &Config{
		DBPath:         getEnv("DB_PATH", "/tmp/zootbox/inventory.db"),
		HTTPHost:       getEnv("HTTP_HOST", "127.0.0.1"),
		HTTPPort:       getEnvInt("HTTP_PORT", 8080),
		MonitoringURL:  getEnv("MONITORING_URL", ""),
		MonitoringAuth: getEnv("MONITORING_AUTH", ""),
		LogLevel:       getEnv("LOG_LEVEL", "info"),
	}

	return cfg, nil
}

func getEnv(key, defaultValue string) string {
	value := os.Getenv(key)
	if value == "" {
		return defaultValue
	}
	return value
}

func getEnvInt(key string, defaultValue int) int {
	value := os.Getenv(key)
	if value == "" {
		return defaultValue
	}
	intValue, err := strconv.Atoi(value)
	if err != nil {
		return defaultValue
	}
	return intValue
}
