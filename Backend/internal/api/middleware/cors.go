package middleware

import (
	"net/http"
	"strings"
)

// CORS middleware for localhost-only access
// Allows requests from Android app running on same device and portal
func CORS(next http.Handler) http.Handler {
	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		origin := r.Header.Get("Origin")

		// Production: Allow localhost origins only (portal on :3000, Android app same-origin)
		if origin != "" && strings.HasPrefix(origin, "http://localhost:") {
			w.Header().Set("Access-Control-Allow-Origin", origin)
		} else if origin == "http://localhost" {
			w.Header().Set("Access-Control-Allow-Origin", origin)
		} else if origin == "" {
			// No Origin header - same-origin request, allow it
			// (Android app accessing localhost:8080 from localhost:8080)
		} else {
			// Production: Reject unauthorized origins
			w.WriteHeader(http.StatusForbidden)
			w.Write([]byte("Forbidden: Origin not allowed"))
			return
		}

		w.Header().Set("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS")
		w.Header().Set("Access-Control-Allow-Headers", "Content-Type, X-Correlation-ID, X-Request-ID")
		w.Header().Set("Access-Control-Max-Age", "86400") // 24 hours

		// Handle preflight requests
		if r.Method == http.MethodOptions {
			w.WriteHeader(http.StatusNoContent)
			return
		}

		next.ServeHTTP(w, r)
	})
}
