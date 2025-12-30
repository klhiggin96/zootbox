package middleware

import (
	"net/http"
	"net/http/httptest"
	"testing"

	"github.com/stretchr/testify/assert"
)

func TestCORS_LocalhostWithPort_Allowed(t *testing.T) {
	// Test that localhost:3000 (portal) is allowed
	handler := CORS(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		w.WriteHeader(http.StatusOK)
		w.Write([]byte("success"))
	}))

	req := httptest.NewRequest(http.MethodGet, "/api/v1/coils", nil)
	req.Header.Set("Origin", "http://localhost:3000")
	w := httptest.NewRecorder()

	handler.ServeHTTP(w, req)

	assert.Equal(t, http.StatusOK, w.Code)
	assert.Equal(t, "http://localhost:3000", w.Header().Get("Access-Control-Allow-Origin"))
	assert.Equal(t, "GET, POST, PUT, DELETE, OPTIONS", w.Header().Get("Access-Control-Allow-Methods"))
	assert.Equal(t, "Content-Type, X-Correlation-ID, X-Request-ID", w.Header().Get("Access-Control-Allow-Headers"))
	assert.Equal(t, "success", w.Body.String())
}

func TestCORS_LocalhostWithDifferentPort_Allowed(t *testing.T) {
	// Test that localhost:8080 (backend same-origin) is allowed
	handler := CORS(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		w.WriteHeader(http.StatusOK)
		w.Write([]byte("success"))
	}))

	req := httptest.NewRequest(http.MethodGet, "/api/v1/coils", nil)
	req.Header.Set("Origin", "http://localhost:8080")
	w := httptest.NewRecorder()

	handler.ServeHTTP(w, req)

	assert.Equal(t, http.StatusOK, w.Code)
	assert.Equal(t, "http://localhost:8080", w.Header().Get("Access-Control-Allow-Origin"))
}

func TestCORS_LocalhostNoPort_Allowed(t *testing.T) {
	// Test that localhost without port is allowed
	handler := CORS(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		w.WriteHeader(http.StatusOK)
		w.Write([]byte("success"))
	}))

	req := httptest.NewRequest(http.MethodGet, "/api/v1/coils", nil)
	req.Header.Set("Origin", "http://localhost")
	w := httptest.NewRecorder()

	handler.ServeHTTP(w, req)

	assert.Equal(t, http.StatusOK, w.Code)
	assert.Equal(t, "http://localhost", w.Header().Get("Access-Control-Allow-Origin"))
}

func TestCORS_NoOriginHeader_Allowed(t *testing.T) {
	// Test that requests without Origin header are allowed (same-origin from Android)
	handler := CORS(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		w.WriteHeader(http.StatusOK)
		w.Write([]byte("success"))
	}))

	req := httptest.NewRequest(http.MethodGet, "/api/v1/coils", nil)
	// No Origin header set
	w := httptest.NewRecorder()

	handler.ServeHTTP(w, req)

	assert.Equal(t, http.StatusOK, w.Code)
	assert.Equal(t, "success", w.Body.String())
	// No Access-Control-Allow-Origin header should be set for same-origin requests
	assert.Empty(t, w.Header().Get("Access-Control-Allow-Origin"))
}

func TestCORS_UnauthorizedOrigin_Forbidden(t *testing.T) {
	// Test that external origins are rejected with 403
	handler := CORS(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		w.WriteHeader(http.StatusOK)
		w.Write([]byte("should not reach here"))
	}))

	unauthorizedOrigins := []string{
		"http://evil.com",
		"https://malicious.com",
		"http://192.168.1.100:3000",
		"http://example.com",
		"https://localhost:3000", // https not allowed, only http
	}

	for _, origin := range unauthorizedOrigins {
		t.Run(origin, func(t *testing.T) {
			req := httptest.NewRequest(http.MethodGet, "/api/v1/coils", nil)
			req.Header.Set("Origin", origin)
			w := httptest.NewRecorder()

			handler.ServeHTTP(w, req)

			assert.Equal(t, http.StatusForbidden, w.Code, "Origin %s should be forbidden", origin)
			assert.Contains(t, w.Body.String(), "Forbidden: Origin not allowed")
		})
	}
}

func TestCORS_PreflightRequest_Handled(t *testing.T) {
	// Test that OPTIONS preflight requests are handled correctly
	handler := CORS(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		w.WriteHeader(http.StatusOK)
		w.Write([]byte("should not reach here"))
	}))

	req := httptest.NewRequest(http.MethodOptions, "/api/v1/coils", nil)
	req.Header.Set("Origin", "http://localhost:3000")
	w := httptest.NewRecorder()

	handler.ServeHTTP(w, req)

	assert.Equal(t, http.StatusNoContent, w.Code)
	assert.Equal(t, "http://localhost:3000", w.Header().Get("Access-Control-Allow-Origin"))
	assert.Equal(t, "GET, POST, PUT, DELETE, OPTIONS", w.Header().Get("Access-Control-Allow-Methods"))
	assert.Equal(t, "Content-Type, X-Correlation-ID, X-Request-ID", w.Header().Get("Access-Control-Allow-Headers"))
	assert.Equal(t, "86400", w.Header().Get("Access-Control-Max-Age"))
	assert.Empty(t, w.Body.String()) // OPTIONS should not reach the handler
}

func TestCORS_PreflightUnauthorized_Forbidden(t *testing.T) {
	// Test that OPTIONS from unauthorized origin is also rejected
	handler := CORS(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		w.WriteHeader(http.StatusOK)
	}))

	req := httptest.NewRequest(http.MethodOptions, "/api/v1/coils", nil)
	req.Header.Set("Origin", "http://evil.com")
	w := httptest.NewRecorder()

	handler.ServeHTTP(w, req)

	assert.Equal(t, http.StatusForbidden, w.Code)
}

func TestCORS_AllMethods_SetHeaders(t *testing.T) {
	// Test that CORS headers are set for all allowed methods
	methods := []string{http.MethodGet, http.MethodPost, http.MethodPut, http.MethodDelete}

	for _, method := range methods {
		t.Run(method, func(t *testing.T) {
			handler := CORS(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
				w.WriteHeader(http.StatusOK)
			}))

			req := httptest.NewRequest(method, "/api/v1/coils", nil)
			req.Header.Set("Origin", "http://localhost:3000")
			w := httptest.NewRecorder()

			handler.ServeHTTP(w, req)

			assert.Equal(t, http.StatusOK, w.Code)
			assert.Equal(t, "http://localhost:3000", w.Header().Get("Access-Control-Allow-Origin"))
		})
	}
}

func TestCORS_MaxAgeHeader_Set(t *testing.T) {
	// Test that Access-Control-Max-Age is set correctly
	handler := CORS(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		w.WriteHeader(http.StatusOK)
	}))

	req := httptest.NewRequest(http.MethodGet, "/api/v1/coils", nil)
	req.Header.Set("Origin", "http://localhost:3000")
	w := httptest.NewRecorder()

	handler.ServeHTTP(w, req)

	assert.Equal(t, "86400", w.Header().Get("Access-Control-Max-Age")) // 24 hours
}
