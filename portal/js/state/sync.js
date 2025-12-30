/**
 * Sync State Management - Auto-refresh and offline detection
 *
 * Usage:
 *   import { startAutoRefresh, stopAutoRefresh } from './state/sync.js';
 *   startAutoRefresh();
 */

import { refreshGrid } from '../components/CoilGrid.js';
import { getCurrentMachine, updateMachine } from './machines.js';
import { markCacheStale, clearStaleMarker } from './inventory.js';
import { updateConnectionStatus } from '../components/StatusIndicator.js';
import { apiClient } from '../api/client.js';
import { log, warn, error } from '../utils/logger.js';
import { UI } from '../config.js';

const REFRESH_INTERVAL = UI.AUTO_REFRESH_INTERVAL;
const HEALTH_CHECK_INTERVAL = UI.AUTO_REFRESH_INTERVAL;
const MAX_FAILED_ATTEMPTS = UI.OFFLINE_THRESHOLD;
const RETRY_INTERVAL = UI.AUTO_REFRESH_INTERVAL;

let refreshTimer = null;
let healthCheckTimer = null;
let retryTimer = null;
let failedAttempts = 0;
let isRefreshing = false;

/**
 * Start auto-refresh
 */
export function startAutoRefresh() {
  log('Starting auto-refresh...');

  // Stop any existing timers
  stopAutoRefresh();

  // Reset failed attempts
  failedAttempts = 0;

  // Start health check
  startHealthCheck();

  // Start inventory refresh
  refreshTimer = setInterval(async () => {
    await performRefresh();
  }, REFRESH_INTERVAL);

  // Perform initial refresh
  performRefresh();
}

/**
 * Stop auto-refresh
 */
export function stopAutoRefresh() {
  log('Stopping auto-refresh...');

  if (refreshTimer) {
    clearInterval(refreshTimer);
    refreshTimer = null;
  }

  if (healthCheckTimer) {
    clearInterval(healthCheckTimer);
    healthCheckTimer = null;
  }

  if (retryTimer) {
    clearInterval(retryTimer);
    retryTimer = null;
  }

  isRefreshing = false;
}

/**
 * Perform refresh operation
 */
async function performRefresh() {
  const machine = getCurrentMachine();

  if (!machine) {
    warn('No machine selected');
    return;
  }

  if (isRefreshing) {
    log('Refresh already in progress, skipping...');
    return;
  }

  isRefreshing = true;

  try {
    await refreshGrid();

    // Success - reset failed attempts
    failedAttempts = 0;

    // Mark machine as online
    updateMachine(machine.id, {
      status: 'online',
      lastSyncTime: new Date().toISOString()
    });

    // Update connection status
    updateConnectionStatus('online');

    // Clear stale marker
    clearStaleMarker(machine.id);

  } catch (err) {
    error('Refresh failed:', err);

    // Increment failed attempts
    failedAttempts++;

    // Handle offline detection
    if (failedAttempts >= MAX_FAILED_ATTEMPTS) {
      handleOffline(machine);
    }

  } finally {
    isRefreshing = false;
  }
}

/**
 * Handle offline state
 */
function handleOffline(machine) {
  warn('Machine offline (3 consecutive failures)');

  // Stop auto-refresh
  if (refreshTimer) {
    clearInterval(refreshTimer);
    refreshTimer = null;
  }

  // Mark machine as offline
  updateMachine(machine.id, { status: 'offline' });

  // Update connection status
  updateConnectionStatus('offline');

  // Mark cache as stale
  markCacheStale(machine.id);

  // Start retry timer (try to reconnect every 30 seconds)
  startRetryTimer();
}

/**
 * Start health check timer
 */
function startHealthCheck() {
  if (healthCheckTimer) {
    clearInterval(healthCheckTimer);
  }

  healthCheckTimer = setInterval(async () => {
    await performHealthCheck();
  }, HEALTH_CHECK_INTERVAL);

  // Perform initial health check
  performHealthCheck();
}

/**
 * Perform health check
 */
async function performHealthCheck() {
  const machine = getCurrentMachine();

  if (!machine) {
    return;
  }

  try {
    const result = await apiClient.healthCheck(1); // 1 retry

    if (result.status === 'online') {
      log('Health check: OK');

      // If we were offline, resume auto-refresh
      if (machine.status === 'offline') {
        log('Machine back online, resuming auto-refresh');
        startAutoRefresh();
      }

    } else {
      warn('Health check: Failed');
      failedAttempts = MAX_FAILED_ATTEMPTS; // Trigger offline state
      handleOffline(machine);
    }

  } catch (err) {
    error('Health check error:', err);
  }
}

/**
 * Start retry timer (when offline)
 */
function startRetryTimer() {
  if (retryTimer) {
    clearInterval(retryTimer);
  }

  log('Starting retry timer (30s interval)');

  retryTimer = setInterval(async () => {
    await attemptReconnect();
  }, RETRY_INTERVAL);

  // Attempt immediate reconnect
  attemptReconnect();
}

/**
 * Attempt to reconnect
 */
async function attemptReconnect() {
  const machine = getCurrentMachine();

  if (!machine) {
    return;
  }

  log('Attempting to reconnect...');

  updateConnectionStatus('checking');

  try {
    const result = await apiClient.healthCheck(1);

    if (result.status === 'online') {
      log('Reconnected successfully!');

      // Clear retry timer
      if (retryTimer) {
        clearInterval(retryTimer);
        retryTimer = null;
      }

      // Resume auto-refresh
      startAutoRefresh();

    } else {
      log('Reconnect failed, will retry in 30s');
      updateConnectionStatus('offline');
    }

  } catch (err) {
    error('Reconnect error:', err);
    updateConnectionStatus('offline');
  }
}

/**
 * Check if auto-refresh is active
 */
export function isAutoRefreshActive() {
  return refreshTimer !== null;
}

/**
 * Get failed attempts count
 */
export function getFailedAttempts() {
  return failedAttempts;
}

/**
 * Force refresh (manual trigger)
 */
export async function forceRefresh() {
  await performRefresh();
}
