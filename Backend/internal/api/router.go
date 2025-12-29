package api

import (
	"database/sql"

	"github.com/go-chi/chi/v5"
	"github.com/go-chi/chi/v5/middleware"
	"github.com/zootbox/backend/internal/api/handlers"
	chiMiddleware "github.com/zootbox/backend/internal/api/middleware"
)

// NewRouter creates and configures the Chi router with all middleware and routes
func NewRouter(db *sql.DB) *chi.Mux {
	r := chi.NewRouter()

	// Middleware stack
	r.Use(middleware.RequestID)               // Generate request ID
	r.Use(chiMiddleware.Logger)               // Zerolog request logging
	r.Use(middleware.Recoverer)               // Panic recovery
	r.Use(chiMiddleware.CORS)                 // Localhost-only CORS
	r.Use(chiMiddleware.CorrelationID)        // Extract/generate correlation ID

	// Health and metrics endpoints (no prefix)
	healthHandler := handlers.NewHealthHandler(db)
	r.Get("/health", healthHandler.Health)
	r.Get("/metrics", healthHandler.Metrics)

	// API v1 routes
	r.Route("/api/v1", func(r chi.Router) {
		// Inventory endpoints
		inventoryHandler := handlers.NewInventoryHandler(db)
		r.Get("/coils", inventoryHandler.GetAllCoils)
		r.Get("/coils/{coilId}", inventoryHandler.GetCoil)
		r.Get("/coils/low-stock", inventoryHandler.GetLowStockCoils)

		// Transaction recording endpoint
		transactionHandler := handlers.NewTransactionHandler(db)
		r.Post("/transactions", transactionHandler.RecordTransaction)

		// Jam events endpoints
		jamHandler := handlers.NewJamHandler(db)
		r.Get("/jam-events", jamHandler.GetJamEvents)
		r.Post("/jam-events/{eventId}/resolve", jamHandler.ResolveJamEvent)

		// Product linking endpoints
		productLinkHandler := handlers.NewProductLinkHandler(db)
		r.Get("/product-links/{sku}/resolve", productLinkHandler.ResolveProductToCoil)

		// Admin endpoints
		adminHandler := handlers.NewAdminHandler(db)
		r.Post("/admin/refill", adminHandler.RefillAll)
		r.Put("/admin/coils/{coilId}", adminHandler.UpdateCoilInventory)
		r.Post("/admin/product-links", adminHandler.CreateProductLink)
		r.Get("/admin/product-links", adminHandler.GetProductLinks)
		r.Delete("/admin/product-links/{linkGroupId}", adminHandler.DeleteProductLink)

		// Sync endpoints (Android app inventory synchronization)
		syncHandler := handlers.NewSyncHandler(db)
		r.Post("/sync/inventory", syncHandler.SyncInventory)
		r.Get("/sync/status", syncHandler.GetSyncStatus)
	})

	return r
}
