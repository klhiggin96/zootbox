/**
 * API Client - Fetch wrapper with error handling, timeouts, and request cancellation
 *
 * Usage:
 *   import { apiClient } from './api/client.js';
 *   const data = await apiClient.get('/health');
 */

import { API } from '../config.js';

class APIClient {
  constructor(baseURL = API.DEFAULT_BACKEND_URL, defaultTimeout = API.DEFAULT_TIMEOUT) {
    this.baseURL = baseURL;
    this.defaultTimeout = defaultTimeout;
    this.activeRequests = new Map(); // Track active requests for cancellation
  }

  /**
   * Set the base URL for API requests (used when switching machines)
   */
  setBaseURL(url) {
    this.baseURL = url;
  }

  /**
   * Cancel all in-flight requests (used when switching machines)
   */
  cancelAllRequests() {
    this.activeRequests.forEach((controller, key) => {
      controller.abort();
      this.activeRequests.delete(key);
    });
  }

  /**
   * Internal fetch wrapper with timeout and error handling
   */
  async request(endpoint, options = {}) {
    const {
      method = 'GET',
      body = null,
      headers = {},
      timeout = this.defaultTimeout,
      signal = null
    } = options;

    // Create AbortController for timeout and manual cancellation
    const controller = new AbortController();
    const requestKey = `${method}-${endpoint}-${Date.now()}`;
    this.activeRequests.set(requestKey, controller);

    // Set timeout
    const timeoutId = setTimeout(() => {
      controller.abort();
    }, timeout);

    try {
      const url = `${this.baseURL}${endpoint}`;

      const fetchOptions = {
        method,
        headers: {
          'Content-Type': 'application/json',
          ...headers
        },
        signal: signal || controller.signal
      };

      if (body) {
        fetchOptions.body = JSON.stringify(body);
      }

      const response = await fetch(url, fetchOptions);

      clearTimeout(timeoutId);
      this.activeRequests.delete(requestKey);

      // Handle HTTP errors
      if (!response.ok) {
        const errorData = await response.json().catch(() => ({
          error: `HTTP ${response.status}: ${response.statusText}`
        }));

        throw new APIError(
          errorData.error || `Request failed with status ${response.status}`,
          response.status,
          errorData
        );
      }

      // Handle 204 No Content
      if (response.status === 204) {
        return null;
      }

      // Parse JSON response
      const data = await response.json();
      return data;

    } catch (error) {
      clearTimeout(timeoutId);
      this.activeRequests.delete(requestKey);

      // Handle AbortError (timeout or manual cancellation)
      if (error.name === 'AbortError') {
        throw new APIError('Request timeout or cancelled', 0, { timeout: true });
      }

      // Handle network errors
      if (error instanceof TypeError) {
        throw new APIError('Network error: Unable to reach backend', 0, {
          network: true,
          originalError: error.message
        });
      }

      // Re-throw APIError
      if (error instanceof APIError) {
        throw error;
      }

      // Unknown error
      throw new APIError(`Unexpected error: ${error.message}`, 0, { originalError: error });
    }
  }

  /**
   * GET request
   */
  async get(endpoint, options = {}) {
    return this.request(endpoint, { ...options, method: 'GET' });
  }

  /**
   * POST request
   */
  async post(endpoint, body = null, options = {}) {
    return this.request(endpoint, { ...options, method: 'POST', body });
  }

  /**
   * PUT request
   */
  async put(endpoint, body = null, options = {}) {
    return this.request(endpoint, { ...options, method: 'PUT', body });
  }

  /**
   * DELETE request
   */
  async delete(endpoint, options = {}) {
    return this.request(endpoint, { ...options, method: 'DELETE' });
  }

  /**
   * Health check with retry logic
   */
  async healthCheck(retries = API.MAX_HEALTH_CHECK_RETRIES) {
    for (let i = 0; i < retries; i++) {
      try {
        const response = await this.get('/health', { timeout: API.HEALTH_CHECK_TIMEOUT });
        return {
          status: 'online',
          data: response
        };
      } catch (error) {
        if (i === retries - 1) {
          return {
            status: 'offline',
            error: error.message
          };
        }
        // Wait before retry (exponential backoff)
        await new Promise(resolve => setTimeout(resolve, API.RETRY_DELAY_MS * (i + 1)));
      }
    }
  }
}

/**
 * Custom API Error class
 */
class APIError extends Error {
  constructor(message, statusCode, details = {}) {
    super(message);
    this.name = 'APIError';
    this.statusCode = statusCode;
    this.details = details;
    this.isNetworkError = details.network || false;
    this.isTimeout = details.timeout || false;
  }

  /**
   * Check if error is a specific HTTP status code
   */
  isStatus(code) {
    return this.statusCode === code;
  }

  /**
   * Check if error is a client error (4xx)
   */
  isClientError() {
    return this.statusCode >= 400 && this.statusCode < 500;
  }

  /**
   * Check if error is a server error (5xx)
   */
  isServerError() {
    return this.statusCode >= 500 && this.statusCode < 600;
  }

  /**
   * Get user-friendly error message
   */
  getUserMessage() {
    if (this.isNetworkError) {
      return 'Unable to connect to backend. Please check your connection.';
    }
    if (this.isTimeout) {
      return 'Request timed out. Please try again.';
    }
    if (this.statusCode === 404) {
      return 'Resource not found.';
    }
    if (this.statusCode === 409) {
      return 'Data conflict. Please refresh and try again.';
    }
    if (this.isServerError()) {
      return 'Server error. Please try again later.';
    }
    return this.message;
  }
}

// Export singleton instance
export const apiClient = new APIClient();

// Export class for custom instances
export { APIClient, APIError };
