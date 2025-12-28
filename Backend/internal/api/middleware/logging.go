package middleware

import (
	"net/http"
	"time"

	"github.com/go-chi/chi/v5/middleware"
	"github.com/rs/zerolog/log"
)

// Logger is a middleware that logs HTTP requests using zerolog
func Logger(next http.Handler) http.Handler {
	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		start := time.Now()

		// Wrap response writer to capture status code
		ww := middleware.NewWrapResponseWriter(w, r.ProtoMajor)

		// Get request ID from context (added by chi middleware.RequestID)
		requestID := middleware.GetReqID(r.Context())

		// Get correlation ID from header (added by our CorrelationID middleware)
		correlationID := r.Header.Get("X-Correlation-ID")
		if correlationID == "" {
			correlationID = requestID // Fallback to request ID
		}

		// Process request
		next.ServeHTTP(ww, r)

		// Log request completion
		duration := time.Since(start)
		log.Info().
			Str("method", r.Method).
			Str("path", r.URL.Path).
			Int("status", ww.Status()).
			Int("bytes", ww.BytesWritten()).
			Dur("duration_ms", duration).
			Str("request_id", requestID).
			Str("correlation_id", correlationID).
			Str("remote_addr", r.RemoteAddr).
			Msg("HTTP request completed")
	})
}
