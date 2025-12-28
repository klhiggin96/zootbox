package middleware

import (
	"net/http"

	"github.com/go-chi/chi/v5/middleware"
)

// CorrelationID extracts or generates a correlation ID for request tracing
// Checks X-Correlation-ID header first (from Android app), falls back to request ID
func CorrelationID(next http.Handler) http.Handler {
	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		correlationID := r.Header.Get("X-Correlation-ID")

		// If Android app didn't provide correlation ID, use request ID
		if correlationID == "" {
			correlationID = middleware.GetReqID(r.Context())
			r.Header.Set("X-Correlation-ID", correlationID)
		}

		// Echo correlation ID back in response headers
		w.Header().Set("X-Correlation-ID", correlationID)

		next.ServeHTTP(w, r)
	})
}
