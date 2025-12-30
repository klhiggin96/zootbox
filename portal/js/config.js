/**
 * ZootBox Portal Configuration
 *
 * Centralized configuration with environment detection.
 * Uses hostname-based detection to determine development vs production mode.
 */

// Environment Detection
const isDevelopment = window.location.hostname === 'localhost' ||
                     window.location.hostname === '127.0.0.1' ||
                     window.location.hostname === '';

const isProduction = !isDevelopment;

// API Configuration
const API_CONFIG = {
  // Default backend URL (development)
  DEFAULT_BACKEND_URL: 'http://localhost:8080',

  // Request timeouts
  DEFAULT_TIMEOUT: 10000,      // 10 seconds for regular requests
  HEALTH_CHECK_TIMEOUT: 5000,  // 5 seconds for health checks

  // Retry configuration
  MAX_HEALTH_CHECK_RETRIES: 3,
  RETRY_DELAY_MS: 1000,        // Initial retry delay
};

// UI Configuration
const UI_CONFIG = {
  // Auto-refresh settings
  AUTO_REFRESH_INTERVAL: 30000,  // 30 seconds

  // Offline detection
  OFFLINE_THRESHOLD: 3,          // Number of consecutive failures before marking offline

  // Toast notification duration
  TOAST_DURATION: 3000,          // 3 seconds

  // Theme
  DEFAULT_THEME: 'light',

  // Coil configuration
  TOTAL_COILS: 10,
  COIL_IDS: ['A1', 'B1', 'C1', 'D1', 'E1', 'F1', 'G1', 'H1', 'I1', 'J1'],
};

// Storage Keys
const STORAGE_KEYS = {
  THEME: 'zootbox-theme',
  MACHINES: 'zootbox-machines',
  CURRENT_MACHINE: 'zootbox-current-machine',
  AUTO_REFRESH: 'zootbox-auto-refresh',
  DEBUG: 'DEBUG',
};

// Feature Flags
const FEATURES_CONFIG = {
  // Enable debug mode (force logging even in production)
  DEBUG_MODE: localStorage.getItem(STORAGE_KEYS.DEBUG) === 'true',

  // Enable auto-refresh
  AUTO_REFRESH: true,

  // Enable offline detection
  OFFLINE_DETECTION: true,
};

// Security Configuration
const SECURITY_CONFIG = {
  // Content Security Policy (informational - not enforced in code)
  CSP_NOTES: 'Relies on Tailscale VPN for network security. No authentication required.',

  // localStorage encryption
  ENCRYPT_STORAGE: false,  // Plaintext acceptable per requirements

  // Allowed origins for API requests
  ALLOWED_ORIGINS: isProduction
    ? [window.location.origin]  // Production: only same origin
    : ['http://localhost:3000', 'http://127.0.0.1:3000', window.location.origin],  // Development: include common dev ports
};

// Export configuration object
export const CONFIG = {
  // Environment
  ENV: {
    isDevelopment,
    isProduction,
  },

  // API settings
  API: API_CONFIG,

  // UI settings
  UI: UI_CONFIG,

  // Storage keys
  STORAGE: STORAGE_KEYS,

  // Features
  FEATURES: FEATURES_CONFIG,

  // Security
  SECURITY: SECURITY_CONFIG,
};

// Export individual sections for convenience
export const ENV = CONFIG.ENV;
export const API = CONFIG.API;
export const UI = CONFIG.UI;
export const STORAGE = CONFIG.STORAGE;
export const FEATURES = CONFIG.FEATURES;
export const SECURITY = CONFIG.SECURITY;

// Helper function to get backend URL (from localStorage or default)
export function getBackendURL() {
  try {
    const currentMachine = JSON.parse(localStorage.getItem(STORAGE.CURRENT_MACHINE));
    if (currentMachine && currentMachine.endpointUrl) {
      return currentMachine.endpointUrl;
    }
  } catch (e) {
    // Ignore parse errors, fall back to default
  }
  return API.DEFAULT_BACKEND_URL;
}

// Log configuration on load (development only)
if (ENV.isDevelopment) {
  console.log('[Config] Environment:', ENV.isDevelopment ? 'Development' : 'Production');
  console.log('[Config] Backend URL:', getBackendURL());
  console.log('[Config] Features:', CONFIG.FEATURES);
}
