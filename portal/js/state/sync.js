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

const REFRESH_INTERVAL = 5000; // 5 seconds
const HEALTH_CHECK_INTERVAL = 30000; // 30 seconds
const MAX_FAILED_ATTEMPTS = 3;
const RETRY_INTERVAL = 30000; // 30 seconds

let refreshTimer = null;
let healthCheckTimer = null;
let retryTimer = null;
let failedAttempts = 0;
let isRefreshing = false;

/**
 * Start auto-refresh
 */
export function startAutoRefresh() {
  console.log('Starting auto-refresh...');

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
  console.log('Stopping auto-refresh...');

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
    console.warn('No machine selected');
    return;
  }

  if (isRefreshing) {
    console.log('Refresh already in progress, skipping...');
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

  } catch (error) {
    console.error('Refresh failed:', error);

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
  console.warn('Machine offline (3 consecutive failures)');

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
      console.log('Health check: OK');

      // If we were offline, resume auto-refresh
      if (machine.status === 'offline') {
        console.log('Machine back online, resuming auto-refresh');
        startAutoRefresh();
      }

    } else {
      console.warn('Health check: Failed');
      failedAttempts = MAX_FAILED_ATTEMPTS; // Trigger offline state
      handleOffline(machine);
    }

  } catch (error) {
    console.error('Health check error:', error);
  }
}

/**
 * Start retry timer (when offline)
 */
function startRetryTimer() {
  if (retryTimer) {
    clearInterval(retryTimer);
  }

  console.log('Starting retry timer (30s interval)');

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

  console.log('Attempting to reconnect...');

  updateConnectionStatus('checking');

  try {
    const result = await apiClient.healthCheck(1);

    if (result.status === 'online') {
      console.log('Reconnected successfully!');

      // Clear retry timer
      if (retryTimer) {
        clearInterval(retryTimer);
        retryTimer = null;
      }

      // Resume auto-refresh
      startAutoRefresh();

    } else {
      console.log('Reconnect failed, will retry in 30s');
      updateConnectionStatus('offline');
    }

  } catch (error) {
    console.error('Reconnect error:', error);
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
